package fr.eric.mobilhomy.dto;

/**
 * Profil « Mon compte » générique (même forme JSON que celui du vacancier et du propriétaire).
 * Utilisé pour le gérant ; OwnerAccountResDto pourra être remplacé par celui-ci lors du nettoyage.
 */
public record AccountResDto(
        Integer id,
        String name,
        String firstname,
        String email,
        String phone
) {
}
