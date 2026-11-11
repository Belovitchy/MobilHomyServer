package fr.eric.mobilhomy.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Création d'un compte propriétaire par l'administrateur (mot de passe provisoire à changer). */
public record AdminOwnerRequestDto(
        @NotBlank @Size(max = 255) String firstname,
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @Pattern(regexp = "^$|^[0-9 +.()-]{6,20}$", message = "Numéro de téléphone invalide") String phone,
        @NotBlank @Size(min = 8, max = 100, message = "8 caractères minimum") String password
) {
}
