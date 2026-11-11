package fr.eric.mobilhomy.bll;

import fr.eric.mobilhomy.bo.*;
import fr.eric.mobilhomy.dal.*;
import fr.eric.mobilhomy.dto.AdminOwnerRequestDto;
import fr.eric.mobilhomy.dto.AdminOwnerResponseDto;
import fr.eric.mobilhomy.dto.AdminStatsResponseDto;
import fr.eric.mobilhomy.exception.BusinessRuleException;
import fr.eric.mobilhomy.exception.NotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final OwnerRepository ownerRepository;
    private final AuthRepository authRepository;
    private final MobilhomeRepository mobilhomeRepository;
    private final ReservationRepository reservationRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<AdminOwnerResponseDto> getOwners() {
        return ownerRepository.findAll(Sort.by("name", "firstname")).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public AdminOwnerResponseDto createOwner(AdminOwnerRequestDto req) {
        String email = req.email().trim().toLowerCase();
        if (authRepository.existsByLogin(email)) {
            throw new BusinessRuleException("Email déjà utilisé");
        }
        Owner owner = Owner.builder()
                .firstname(req.firstname().trim())
                .name(req.name().trim())
                .phone(req.phone() == null || req.phone().isBlank() ? null : req.phone().trim())
                .build();
        Auth auth = Auth.builder()
                .utilisateur(owner)
                .login(email)
                .password(passwordEncoder.encode(req.password()))
                .authority(RolesEnum.OWNER)
                .build();
        owner.setAuth(auth);
        authRepository.save(auth); // cascade : l'Owner est créé avec son Auth
        return toDto(owner);
    }

    @Override
    @Transactional
    public AdminOwnerResponseDto setOwnerEnabled(Integer ownerId, boolean enabled) {
        if (ownerId == null) {
            throw new IllegalArgumentException("Id must not be null");
        }
        Owner owner = ownerRepository.findById(ownerId)
                .orElseThrow(() -> new NotFoundException("Propriétaire introuvable"));
        Auth auth = owner.getAuth();
        if (auth == null) {
            throw new NotFoundException("Compte introuvable");
        }
        // Effet immédiat : JwtAuthenticationFilter vérifie isEnabled() à chaque requête,
        // donc les jetons déjà délivrés au propriétaire cessent de fonctionner.
        auth.setEnabled(enabled);
        authRepository.save(auth);
        return toDto(owner);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminStatsResponseDto getStats() {
        return new AdminStatsResponseDto(
                authRepository.countByAuthority(RolesEnum.OWNER),
                authRepository.countByAuthorityAndEnabledFalse(RolesEnum.OWNER),
                authRepository.countByAuthority(RolesEnum.MANAGER),
                authRepository.countByAuthority(RolesEnum.VACATIONER),
                mobilhomeRepository.count(),
                reservationRepository.count(),
                reservationRepository.countByStatus(ReservationStatus.VALIDATED)
        );
    }

    private AdminOwnerResponseDto toDto(Owner o) {
        return AdminOwnerResponseDto.fromEntity(o,
                o.getId() != null ? mobilhomeRepository.countByOwnerId(o.getId()) : 0,
                o.getId() != null ? reservationRepository.countByMobilhomeOwnerId(o.getId()) : 0);
    }
}
