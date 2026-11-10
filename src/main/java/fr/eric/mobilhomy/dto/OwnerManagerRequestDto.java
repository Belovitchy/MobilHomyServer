package fr.eric.mobilhomy.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Création / modification d'un gérant par un propriétaire (ManagerInput côté Angular).
 * Le mot de passe est obligatoire à la création (vérifié dans le service), ignoré en modification.
 */
public record OwnerManagerRequestDto(
        @NotBlank @Size(max = 255) String firstname,
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Pattern(regexp = "^[0-9 +.()-]{6,20}$", message = "Numéro de téléphone invalide") String phone,
        @Size(min = 8, max = 100, message = "8 caractères minimum") String password
) {
}
