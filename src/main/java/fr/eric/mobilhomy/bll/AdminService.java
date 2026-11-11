package fr.eric.mobilhomy.bll;

import fr.eric.mobilhomy.dto.AdminOwnerRequestDto;
import fr.eric.mobilhomy.dto.AdminOwnerResponseDto;
import fr.eric.mobilhomy.dto.AdminStatsResponseDto;

import java.util.List;

/** Cas d'utilisation de l'administrateur : gestion des comptes propriétaires et supervision. */
public interface AdminService {

    List<AdminOwnerResponseDto> getOwners();

    AdminOwnerResponseDto createOwner(AdminOwnerRequestDto req);

    /** Suspend (enabled=false) ou réactive un compte propriétaire. */
    AdminOwnerResponseDto setOwnerEnabled(Integer ownerId, boolean enabled);

    AdminStatsResponseDto getStats();
}
