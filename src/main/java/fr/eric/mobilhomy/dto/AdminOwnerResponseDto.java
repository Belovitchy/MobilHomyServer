package fr.eric.mobilhomy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.eric.mobilhomy.bo.Owner;

/** Compte propriétaire vu par l'administrateur (sans donnée de réservation détaillée). */
public record AdminOwnerResponseDto(
        Integer id,
        String firstname,
        String name,
        String email,
        String phone,
        boolean enabled,
        @JsonProperty("mobilhomes_count") long mobilhomesCount,
        @JsonProperty("reservations_count") long reservationsCount
) {
    public static AdminOwnerResponseDto fromEntity(Owner o, long mobilhomes, long reservations) {
        return new AdminOwnerResponseDto(
                o.getId(),
                o.getFirstname(),
                o.getName(),
                o.getAuth() != null ? o.getAuth().getLogin() : null,
                o.getPhone(),
                o.getAuth() == null || o.getAuth().isEnabled(),
                mobilhomes,
                reservations
        );
    }
}
