package fr.eric.mobilhomy.dto;

import fr.eric.mobilhomy.bo.Owner;

/** Profil du propriétaire connecté (même forme que le profil vacancier, sans les réservations). */
public record OwnerAccountResDto(
        Integer id,
        String name,
        String firstname,
        String email,
        String phone
) {
    public static OwnerAccountResDto fromEntity(Owner owner) {
        return new OwnerAccountResDto(
                owner.getId(),
                owner.getName(),
                owner.getFirstname(),
                owner.getAuth() != null ? owner.getAuth().getLogin() : null,
                owner.getPhone()
        );
    }
}
