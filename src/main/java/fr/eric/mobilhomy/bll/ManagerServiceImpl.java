package fr.eric.mobilhomy.bll;

import fr.eric.mobilhomy.bo.*;
import fr.eric.mobilhomy.dal.*;
import fr.eric.mobilhomy.dto.*;
import fr.eric.mobilhomy.exception.BusinessRuleException;
import fr.eric.mobilhomy.exception.NotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class ManagerServiceImpl implements ManagerService {

    static final int MIN_PASSWORD_LENGTH = 8;

    private final ManagerRepository managerRepository;
    private final MobilhomeRepository mobilhomeRepository;
    private final ReservationRepository reservationRepository;
    private final InvoiceRepository invoiceRepository;
    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;

    // =====================================================================
    // Réservations
    // =====================================================================

    /**
     * Seuls les séjours VALIDÉS sont visibles : le gérant n'a pas à connaître les coordonnées
     * d'un vacancier tant que la réservation n'est pas confirmée (minimisation RGPD).
     */
    @Override
    @Transactional(readOnly = true)
    public List<ManagerReservationResponseDto> getReservations(Integer managerId) {
        requireId(managerId);
        return reservationRepository.findAllByManagerIdAndStatus(managerId, ReservationStatus.VALIDATED).stream()
                .map(ManagerReservationResponseDto::fromEntity)
                .toList();
    }

    // =====================================================================
    // Mobil-homes gérés
    // =====================================================================

    @Override
    @Transactional(readOnly = true)
    public List<ManagerMobilhomeResponseDto> getMobilhomes(Integer managerId) {
        requireId(managerId);
        return mobilhomeRepository.findByManagerIdOrderByName(managerId).stream()
                .map(ManagerMobilhomeResponseDto::fromEntity)
                .toList();
    }

    // =====================================================================
    // Factures
    // =====================================================================

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponseDto> getInvoices(Integer managerId) {
        requireId(managerId);
        return invoiceRepository.findAllByManagerId(managerId).stream()
                .map(InvoiceResponseDto::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public InvoiceResponseDto createInvoice(Integer managerId, InvoiceRequestDto req) {
        requireId(managerId);
        if (req == null || req.mobilhomeId() == null) {
            throw new IllegalArgumentException("Mobil-home obligatoire");
        }
        // Un gérant ne facture que pour un mobil-home qu'il gère.
        Mobilhome mobilhome = mobilhomeRepository.findByIdAndManagerId(req.mobilhomeId(), managerId)
                .orElseThrow(() -> new NotFoundException("Mobil-home introuvable"));

        Invoice invoice = Invoice.builder()
                .manager(mobilhome.getManager())
                .mobilhome(mobilhome)
                .owner(mobilhome.getOwner()) // c'est le propriétaire qui règle la facture
                .description(req.description().trim())
                .amount(req.amount())
                .date(req.date())
                .category(req.category())
                .type(InvoiceType.OUTGOING)  // dépense pour le propriétaire
                .status(InvoiceStatus.ISSUED)
                .build();
        return InvoiceResponseDto.fromEntity(invoiceRepository.save(invoice));
    }

    /** Le gérant peut annuler une facture saisie par erreur, tant qu'elle n'est pas payée. */
    @Override
    @Transactional
    public InvoiceResponseDto cancelInvoice(Integer managerId, Integer invoiceId) {
        requireId(managerId);
        requireId(invoiceId);
        Invoice invoice = invoiceRepository.findByIdAndManagerId(invoiceId, managerId)
                .orElseThrow(() -> new NotFoundException("Facture introuvable"));
        if (invoice.getStatus() != InvoiceStatus.ISSUED) {
            throw new BusinessRuleException("Impossible d'annuler une facture au statut " + invoice.getStatus());
        }
        invoice.setStatus(InvoiceStatus.CANCELLED);
        return InvoiceResponseDto.fromEntity(invoiceRepository.save(invoice));
    }

    // =====================================================================
    // Mon compte
    // =====================================================================

    @Override
    @Transactional(readOnly = true)
    public AccountResDto getAccount(Integer managerId) {
        return toAccount(getManager(managerId));
    }

    @Override
    @Transactional
    public AccountResDto updateAccount(Integer managerId, VacationerAccountReqDto req) {
        Manager manager = getManager(managerId);
        if (req == null || isBlank(req.firstname()) || isBlank(req.name()) || isBlank(req.phone())) {
            throw new IllegalArgumentException("Le prénom, le nom et le téléphone sont obligatoires");
        }
        manager.setFirstname(req.firstname().trim());
        manager.setName(req.name().trim());
        manager.setPhone(req.phone().trim()); // obligatoire : c'est le contact donné aux vacanciers
        return toAccount(managerRepository.save(manager));
    }

    @Override
    @Transactional
    public AccountResDto updateEmail(Integer managerId, VacationerMailReqDto req) {
        Manager manager = getManagerWithVerifiedPassword(managerId, req.currentPassword());
        String email = req.newEmail().trim().toLowerCase();
        Auth auth = manager.getAuth();
        if (!email.equalsIgnoreCase(auth.getLogin())) {
            if (authRepository.existsByLogin(email)) {
                throw new BusinessRuleException("Email déjà utilisé");
            }
            auth.setLogin(email);
            authRepository.save(auth);
        }
        return toAccount(manager);
    }

    @Override
    @Transactional
    public AccountResDto updatePassword(Integer managerId, VacationerPassReqDto req) {
        if (req.newPassword() == null || req.newPassword().length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins " + MIN_PASSWORD_LENGTH + " caractères");
        }
        Manager manager = getManagerWithVerifiedPassword(managerId, req.currentPassword());
        Auth auth = manager.getAuth();
        auth.setPassword(passwordEncoder.encode(req.newPassword()));
        authRepository.save(auth);
        return toAccount(manager);
    }

    private Manager getManager(Integer managerId) {
        requireId(managerId);
        return managerRepository.findById(managerId)
                .orElseThrow(() -> new NotFoundException("Gérant introuvable"));
    }

    private Manager getManagerWithVerifiedPassword(Integer managerId, String currentPassword) {
        Manager manager = getManager(managerId);
        if (currentPassword == null || manager.getAuth() == null
                || !passwordEncoder.matches(currentPassword, manager.getAuth().getPassword())) {
            throw new BadCredentialsException("Mot de passe incorrect");
        }
        return manager;
    }

    private static AccountResDto toAccount(Manager m) {
        return new AccountResDto(m.getId(), m.getName(), m.getFirstname(),
                m.getAuth() != null ? m.getAuth().getLogin() : null, m.getPhone());
    }

    // =====================================================================
    // Utilitaires
    // =====================================================================

    private static void requireId(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("Id must not be null");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
