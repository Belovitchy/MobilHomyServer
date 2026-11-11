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

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@AllArgsConstructor
public class OwnerServiceImpl implements OwnerService {

    /**
     * Statut « demande soumise par le vacancier, à traiter par le propriétaire ».
     * Dans l'application actuelle c'est COMPLETED (cf. submitReservation et l'onglet « À traiter » du front).
     */
    static final ReservationStatus SUBMITTED = ReservationStatus.COMPLETED;

    /** Statuts qui interdisent de supprimer un mobil-home (on ne détruit pas un engagement ni une demande en cours). */
    static final Set<ReservationStatus> BLOCKING_STATUSES = EnumSet.of(ReservationStatus.VALIDATED, SUBMITTED);

    /** Statuts affichés dans le calendrier du propriétaire. */
    static final Set<ReservationStatus> CALENDAR_STATUSES = EnumSet.of(ReservationStatus.VALIDATED, SUBMITTED);

    private final MobilhomeRepository mobilhomeRepository;
    private final ReservationRepository reservationRepository;
    private final OwnerManagerRepository ownerManagerRepository;
    private final OwnerRepository ownerRepository;
    private final AuthRepository authRepository;
    private final ImageRepository imageRepository;
    private final InvoiceRepository invoiceRepository;
    private final LinkRepository linkRepository;
    private final PasswordEncoder passwordEncoder;

    // =====================================================================
    // Mobil-homes
    // =====================================================================

    @Override
    @Transactional(readOnly = true)
    public List<OwnerMobilhomeResponseDto> getMobilhomes(Integer ownerId) {
        requireId(ownerId);
        return mobilhomeRepository.findAllByOwnerIdWithImages(ownerId).stream()
                .map(OwnerMobilhomeResponseDto::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public OwnerMobilhomeResponseDto createMobilhome(Integer ownerId, OwnerMobilhomeRequestDto req) {
        requireId(ownerId);
        Owner owner = ownerRepository.findById(ownerId)
                .orElseThrow(() -> new NotFoundException("Propriétaire introuvable"));

        Mobilhome m = Mobilhome.builder().owner(owner).build();
        applyRequest(ownerId, m, req);
        return OwnerMobilhomeResponseDto.fromEntity(mobilhomeRepository.save(m));
    }

    @Override
    @Transactional
    public OwnerMobilhomeResponseDto updateMobilhome(Integer ownerId, Integer mobilhomeId, OwnerMobilhomeRequestDto req) {
        Mobilhome m = getOwnedMobilhome(ownerId, mobilhomeId);
        applyRequest(ownerId, m, req);
        return OwnerMobilhomeResponseDto.fromEntity(mobilhomeRepository.save(m));
    }

    @Override
    @Transactional
    public void deleteMobilhome(Integer ownerId, Integer mobilhomeId) {
        Mobilhome m = getOwnedMobilhome(ownerId, mobilhomeId);
        if (reservationRepository.existsByMobilhomeIdAndStatusIn(m.getId(), BLOCKING_STATUSES)) {
            throw new BusinessRuleException(
                    "Ce mobil-home a des réservations validées ou en attente de traitement : suppression impossible");
        }
        if (invoiceRepository.existsByMobilhomeId(m.getId())) {
            // Les factures sont des pièces comptables : on ne les supprime pas avec le mobil-home.
            throw new BusinessRuleException("Ce mobil-home a des factures rattachées : suppression impossible");
        }
        // Cascade : images, brouillons et réservations refusées/annulées sont supprimés avec lui.
        mobilhomeRepository.delete(m);
    }

    /** Copie les champs de la requête dans l'entité (création comme modification). */
    private void applyRequest(Integer ownerId, Mobilhome m, OwnerMobilhomeRequestDto req) {
        if (req == null) {
            throw new IllegalArgumentException("Request body must not be null");
        }
        m.setName(req.title().trim());
        m.setDescription(trimToNull(req.description()));
        m.setLocation(trimToNull(req.location()));
        m.setAddress(req.address().trim());
        m.setPostalCode(req.postalCode());
        m.setCapacity(req.capacity());
        m.setPrice(req.price());
        m.setIcalLink(trimToNull(req.icalLink()));

        if (req.managerId() == null) {
            m.setManager(null);
        } else {
            // Un propriétaire ne peut affecter qu'un de SES gérants.
            OwnerManager link = ownerManagerRepository.findByOwnerIdAndManagerId(ownerId, req.managerId())
                    .orElseThrow(() -> new NotFoundException("Gérant introuvable"));
            m.setManager(link.getManager());
        }

        // Remplacement complet de la liste d'images.
        if (req.images() != null) {
            if (!m.getImages().isEmpty()) {
                imageRepository.deleteAll(m.getImages());
                m.getImages().clear();
            }
            req.images().stream()
                    .map(String::trim)
                    .distinct()
                    .forEach(path -> m.getImages().add(Image.builder().path(path).mobilhome(m).build()));
        }
    }

    private Mobilhome getOwnedMobilhome(Integer ownerId, Integer mobilhomeId) {
        requireId(ownerId);
        requireId(mobilhomeId);
        return mobilhomeRepository.findByIdAndOwnerId(mobilhomeId, ownerId)
                .orElseThrow(() -> new NotFoundException("Mobil-home introuvable"));
    }

    // =====================================================================
    // Gérants
    // =====================================================================

    @Override
    @Transactional(readOnly = true)
    public List<ManagerResponseDto> getManagers(Integer ownerId) {
        requireId(ownerId);
        return ownerManagerRepository.findManagersByOwnerId(ownerId).stream()
                .map(ManagerResponseDto::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public ManagerResponseDto createManager(Integer ownerId, OwnerManagerRequestDto req) {
        requireId(ownerId);
        if (req.password() == null || req.password().isBlank()) {
            throw new IllegalArgumentException("Le mot de passe est obligatoire à la création d'un gérant");
        }
        String email = normalizeEmail(req.email());
        if (authRepository.existsByLogin(email)) {
            throw new BusinessRuleException("Email déjà utilisé");
        }
        Owner owner = ownerRepository.findById(ownerId)
                .orElseThrow(() -> new NotFoundException("Propriétaire introuvable"));

        Manager manager = Manager.builder()
                .firstname(req.firstname().trim())
                .name(req.name().trim())
                .phone(req.phone().trim())
                .build();
        Auth auth = Auth.builder()
                .login(email)
                .password(passwordEncoder.encode(req.password()))
                .authority(RolesEnum.MANAGER)
                .utilisateur(manager)
                .build();
        manager.setAuth(auth);
        authRepository.save(auth); // cascade ALL : le Manager est persisté avec son Auth

        ownerManagerRepository.save(OwnerManager.builder().owner(owner).manager(manager).build());
        return ManagerResponseDto.fromEntity(manager);
    }

    @Override
    @Transactional
    public ManagerResponseDto updateManager(Integer ownerId, Integer managerId, OwnerManagerRequestDto req) {
        Manager manager = getOwnedManager(ownerId, managerId);

        manager.setFirstname(req.firstname().trim());
        manager.setName(req.name().trim());
        manager.setPhone(req.phone().trim());

        String email = normalizeEmail(req.email());
        Auth auth = manager.getAuth();
        if (auth != null && !email.equalsIgnoreCase(auth.getLogin())) {
            if (authRepository.existsByLogin(email)) {
                throw new BusinessRuleException("Email déjà utilisé");
            }
            auth.setLogin(email);
        }
        // Le mot de passe n'est pas modifiable par le propriétaire (c'est au gérant de le faire).
        return ManagerResponseDto.fromEntity(manager);
    }

    @Override
    @Transactional
    public void deleteManager(Integer ownerId, Integer managerId) {
        OwnerManager link = getOwnerManagerLink(ownerId, managerId);
        Manager manager = link.getManager();

        // 1. Les mobil-homes du propriétaire n'ont plus de gérant.
        mobilhomeRepository.findByOwnerIdAndManagerId(ownerId, managerId)
                .forEach(m -> m.setManager(null));

        // 2. On retire le lien propriétaire ↔ gérant.
        ownerManagerRepository.delete(link);
        ownerManagerRepository.flush();

        // 3. Si le gérant ne travaille plus pour personne, on supprime son compte (Auth → Manager en cascade).
        //    Exception : s'il a saisi des factures, on garde son compte pour conserver l'historique comptable.
        if (ownerManagerRepository.countByManagerId(managerId) == 0
                && !invoiceRepository.existsByManagerId(managerId)
                && manager.getAuth() != null) {
            authRepository.delete(manager.getAuth());
        }
    }

    private Manager getOwnedManager(Integer ownerId, Integer managerId) {
        return getOwnerManagerLink(ownerId, managerId).getManager();
    }

    private OwnerManager getOwnerManagerLink(Integer ownerId, Integer managerId) {
        requireId(ownerId);
        requireId(managerId);
        return ownerManagerRepository.findByOwnerIdAndManagerId(ownerId, managerId)
                .orElseThrow(() -> new NotFoundException("Gérant introuvable"));
    }

    // =====================================================================
    // Réservations
    // =====================================================================

    @Override
    @Transactional(readOnly = true)
    public List<OwnerReservationResponseDto> getReservations(Integer ownerId, ReservationStatus status) {
        requireId(ownerId);
        List<Reservation> reservations = status == null
                ? reservationRepository.findAllByOwnerId(ownerId)
                : reservationRepository.findAllByOwnerIdAndStatus(ownerId, status);
        return reservations.stream().map(OwnerReservationResponseDto::fromEntity).toList();
    }

    @Override
    @Transactional
    public OwnerReservationResponseDto validateReservation(Integer ownerId, Integer reservationId) {
        Reservation r = getOwnedReservation(ownerId, reservationId);
        requireStatus(r, SUBMITTED, "valider");
        if (r.getStartDate() == null || r.getEndDate() == null || !r.getStartDate().isBefore(r.getEndDate())) {
            throw new BusinessRuleException("Dates de séjour invalides");
        }
        boolean overlap = reservationRepository.existsOverlap(
                r.getMobilhome().getId(), r.getId(), ReservationStatus.VALIDATED, r.getStartDate(), r.getEndDate());
        if (overlap) {
            throw new BusinessRuleException("Le mobil-home est déjà réservé sur tout ou partie de cette période");
        }
        r.setStatus(ReservationStatus.VALIDATED);
        return OwnerReservationResponseDto.fromEntity(reservationRepository.save(r));
    }

    @Override
    @Transactional
    public OwnerReservationResponseDto rejectReservation(Integer ownerId, Integer reservationId) {
        Reservation r = getOwnedReservation(ownerId, reservationId);
        requireStatus(r, SUBMITTED, "refuser");
        r.setStatus(ReservationStatus.REJECTED);
        return OwnerReservationResponseDto.fromEntity(reservationRepository.save(r));
    }

    /**
     * Le propriétaire déclare le séjour au camping (avec la plaque) une fois la réservation validée,
     * puis reporte ici le numéro Siblu reçu : il ne peut donc être saisi que sur une réservation VALIDATED.
     */
    @Override
    @Transactional
    public OwnerReservationResponseDto updateSibluRef(Integer ownerId, Integer reservationId, SibluRefRequestDto req) {
        Reservation r = getOwnedReservation(ownerId, reservationId);
        if (r.getStatus() != ReservationStatus.VALIDATED) {
            throw new BusinessRuleException("Le numéro Siblu ne peut être saisi que sur une réservation validée");
        }
        r.setSibluRef(req.sibluRef().trim().toUpperCase());
        return OwnerReservationResponseDto.fromEntity(reservationRepository.save(r));
    }

    // =====================================================================
    // Réservations externes (Booking, Airbnb, Abritel…)
    // =====================================================================

    @Override
    @Transactional
    public OwnerReservationResponseDto createExternalReservation(Integer ownerId, ExternalReservationRequestDto req) {
        if (req.source() == ReservationSource.MOBILHOMY) {
            throw new IllegalArgumentException("Une réservation externe doit venir d'une autre plateforme");
        }
        Mobilhome m = getOwnedMobilhome(ownerId, req.mobilhomeId());
        if (!req.startDate().isBefore(req.endDate())) {
            throw new IllegalArgumentException("La date de départ doit être après la date d'arrivée");
        }
        checkCapacity(m, req.occupants());
        // Même garde-fou que pour une demande MobilHomy : pas de double réservation.
        if (reservationRepository.existsOverlap(m.getId(), -1, ReservationStatus.VALIDATED, req.startDate(), req.endDate())) {
            throw new BusinessRuleException("Le mobil-home est déjà réservé sur tout ou partie de cette période");
        }

        Reservation r = Reservation.builder()
                .mobilhome(m)
                .source(req.source())
                .guestName(req.guestName().trim())
                .guestPhone(trimToNull(req.guestPhone()))
                .startDate(req.startDate())
                .endDate(req.endDate())
                .immat(normalizeImmat(req.immat()))
                .status(ReservationStatus.VALIDATED) // déjà payée sur la plateforme d'origine
                .build();
        replaceOccupants(r, req.occupants());
        return OwnerReservationResponseDto.fromEntity(reservationRepository.save(r));
    }

    @Override
    @Transactional
    public OwnerReservationResponseDto updateReservationDetails(Integer ownerId, Integer reservationId,
                                                               ReservationDetailsRequestDto req) {
        Reservation r = getOwnedReservation(ownerId, reservationId);
        requireExternal(r);
        if (r.getStatus() == ReservationStatus.CANCELLED) {
            throw new BusinessRuleException("Réservation annulée : modification impossible");
        }
        checkCapacity(r.getMobilhome(), req.occupants());
        r.setGuestName(req.guestName().trim());
        r.setGuestPhone(trimToNull(req.guestPhone()));
        r.setImmat(normalizeImmat(req.immat()));
        replaceOccupants(r, req.occupants());
        return OwnerReservationResponseDto.fromEntity(reservationRepository.save(r));
    }

    @Override
    @Transactional
    public OwnerReservationResponseDto cancelExternalReservation(Integer ownerId, Integer reservationId) {
        Reservation r = getOwnedReservation(ownerId, reservationId);
        requireExternal(r);
        if (r.getStatus() != ReservationStatus.VALIDATED) {
            throw new BusinessRuleException("Impossible d'annuler une réservation au statut " + r.getStatus());
        }
        r.setStatus(ReservationStatus.CANCELLED);
        return OwnerReservationResponseDto.fromEntity(reservationRepository.save(r));
    }

    /** Les données d'une réservation MobilHomy appartiennent au vacancier : le propriétaire ne les modifie pas. */
    private static void requireExternal(Reservation r) {
        if (!r.isExternal()) {
            throw new BusinessRuleException("Seules les réservations externes sont modifiables par le propriétaire");
        }
    }

    private static void checkCapacity(Mobilhome m, List<OwnerOccupantDto> occupants) {
        if (occupants != null && m.getCapacity() > 0 && occupants.size() > m.getCapacity()) {
            throw new BusinessRuleException("Trop d'occupants : ce mobil-home accueille " + m.getCapacity() + " personnes maximum");
        }
    }

    private static void replaceOccupants(Reservation r, List<OwnerOccupantDto> occupants) {
        r.getOccupants().clear(); // orphanRemoval : les anciens occupants sont supprimés
        if (occupants != null) {
            occupants.forEach(o -> r.getOccupants().add(Occupant.builder()
                    .firstname(o.firstname().trim())
                    .name(o.name().trim())
                    .age(o.age())
                    .reservation(r)
                    .build()));
        }
        r.setNumberOfPerson(r.getOccupants().size());
    }

    private static String normalizeImmat(String immat) {
        return immat == null || immat.isBlank() ? null : immat.trim().toUpperCase();
    }

    // =====================================================================
    // Factures de fonctionnement (saisies par ses gérants)
    // =====================================================================

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponseDto> getInvoices(Integer ownerId) {
        requireId(ownerId);
        return invoiceRepository.findAllByOwnerId(ownerId).stream().map(InvoiceResponseDto::fromEntity).toList();
    }

    @Override
    @Transactional
    public InvoiceResponseDto payInvoice(Integer ownerId, Integer invoiceId) {
        requireId(ownerId);
        requireId(invoiceId);
        Invoice invoice = invoiceRepository.findByIdAndOwnerId(invoiceId, ownerId)
                .orElseThrow(() -> new NotFoundException("Facture introuvable"));
        if (invoice.getStatus() != InvoiceStatus.ISSUED) {
            throw new BusinessRuleException("Seule une facture en attente peut être marquée payée (statut actuel : "
                    + invoice.getStatus() + ")");
        }
        invoice.setStatus(InvoiceStatus.PAID);
        return InvoiceResponseDto.fromEntity(invoiceRepository.save(invoice));
    }

    // =====================================================================
    // Liens utiles (extranet Booking, déclaration Siblu, iCal…)
    // =====================================================================

    @Override
    @Transactional(readOnly = true)
    public List<LinkResponseDto> getLinks(Integer ownerId) {
        requireId(ownerId);
        return linkRepository.findByOwnerIdOrderByNameAsc(ownerId).stream().map(LinkResponseDto::fromEntity).toList();
    }

    @Override
    @Transactional
    public LinkResponseDto createLink(Integer ownerId, LinkRequestDto req) {
        Owner owner = getOwner(ownerId);
        Link link = Link.builder().owner(owner).name(req.name().trim()).url(req.url().trim()).build();
        return LinkResponseDto.fromEntity(linkRepository.save(link));
    }

    @Override
    @Transactional
    public LinkResponseDto updateLink(Integer ownerId, Integer linkId, LinkRequestDto req) {
        Link link = getOwnedLink(ownerId, linkId);
        link.setName(req.name().trim());
        link.setUrl(req.url().trim());
        return LinkResponseDto.fromEntity(linkRepository.save(link));
    }

    @Override
    @Transactional
    public void deleteLink(Integer ownerId, Integer linkId) {
        linkRepository.delete(getOwnedLink(ownerId, linkId));
    }

    private Link getOwnedLink(Integer ownerId, Integer linkId) {
        requireId(ownerId);
        requireId(linkId);
        return linkRepository.findByIdAndOwnerId(linkId, ownerId)
                .orElseThrow(() -> new NotFoundException("Lien introuvable"));
    }

    private Reservation getOwnedReservation(Integer ownerId, Integer reservationId) {
        requireId(ownerId);
        requireId(reservationId);
        return reservationRepository.findByIdAndMobilhomeOwnerId(reservationId, ownerId)
                .orElseThrow(() -> new NotFoundException("Réservation introuvable"));
    }

    private static void requireStatus(Reservation r, ReservationStatus expected, String action) {
        if (r.getStatus() != expected) {
            throw new BusinessRuleException(
                    "Impossible de " + action + " une réservation au statut " + r.getStatus());
        }
    }

    // =====================================================================
    // Calendrier
    // =====================================================================

    @Override
    @Transactional(readOnly = true)
    public List<CalendarEventDto> getCalendar(Integer ownerId, Integer mobilhomeId) {
        requireId(ownerId);
        if (mobilhomeId != null) {
            getOwnedMobilhome(ownerId, mobilhomeId); // 404 si le mobil-home n'est pas à lui
        }
        return reservationRepository.findAllByOwnerId(ownerId).stream()
                .filter(r -> CALENDAR_STATUSES.contains(r.getStatus()))
                .filter(r -> r.getStartDate() != null && r.getEndDate() != null)
                .filter(r -> mobilhomeId == null || mobilhomeId.equals(r.getMobilhome().getId()))
                .map(CalendarEventDto::fromReservation)
                .toList();
        // TODO évolution : fusionner ici les événements iCal externes (source = "ICAL").
    }

    // =====================================================================
    // Mon compte
    // =====================================================================

    static final int MIN_PASSWORD_LENGTH = 8;

    @Override
    @Transactional(readOnly = true)
    public OwnerAccountResDto getAccount(Integer ownerId) {
        return OwnerAccountResDto.fromEntity(getOwner(ownerId));
    }

    @Override
    @Transactional
    public OwnerAccountResDto updateAccount(Integer ownerId, VacationerAccountReqDto req) {
        Owner owner = getOwner(ownerId);
        if (req == null || isBlank(req.firstname()) || isBlank(req.name())) {
            throw new IllegalArgumentException("Le prénom et le nom sont obligatoires");
        }
        owner.setFirstname(req.firstname().trim());
        owner.setName(req.name().trim());
        owner.setPhone(trimToNull(req.phone()));
        return OwnerAccountResDto.fromEntity(ownerRepository.save(owner));
    }

    @Override
    @Transactional
    public OwnerAccountResDto updateEmail(Integer ownerId, VacationerMailReqDto req) {
        Owner owner = getOwnerWithVerifiedPassword(ownerId, req.currentPassword());
        String email = normalizeEmail(req.newEmail());
        Auth auth = owner.getAuth();
        if (!email.equalsIgnoreCase(auth.getLogin())) {
            if (authRepository.existsByLogin(email)) {
                throw new BusinessRuleException("Email déjà utilisé");
            }
            auth.setLogin(email);
            authRepository.save(auth);
        }
        // Le JWT porte l'ancien email : le front déconnecte l'utilisateur après ce changement.
        return OwnerAccountResDto.fromEntity(owner);
    }

    @Override
    @Transactional
    public OwnerAccountResDto updatePassword(Integer ownerId, VacationerPassReqDto req) {
        if (req.newPassword() == null || req.newPassword().length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins " + MIN_PASSWORD_LENGTH + " caractères");
        }
        Owner owner = getOwnerWithVerifiedPassword(ownerId, req.currentPassword());
        Auth auth = owner.getAuth();
        auth.setPassword(passwordEncoder.encode(req.newPassword()));
        authRepository.save(auth);
        return OwnerAccountResDto.fromEntity(owner);
    }

    private Owner getOwner(Integer ownerId) {
        requireId(ownerId);
        return ownerRepository.findById(ownerId)
                .orElseThrow(() -> new NotFoundException("Propriétaire introuvable"));
    }

    /** Changement d'email ou de mot de passe : on redemande le mot de passe actuel (ré-authentification). */
    private Owner getOwnerWithVerifiedPassword(Integer ownerId, String currentPassword) {
        Owner owner = getOwner(ownerId);
        if (currentPassword == null || owner.getAuth() == null
                || !passwordEncoder.matches(currentPassword, owner.getAuth().getPassword())) {
            throw new BadCredentialsException("Mot de passe incorrect");
        }
        return owner;
    }

    // =====================================================================
    // Utilitaires
    // =====================================================================

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static void requireId(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("Id must not be null");
        }
    }

    private static String trimToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
