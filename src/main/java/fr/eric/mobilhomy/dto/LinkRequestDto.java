package fr.eric.mobilhomy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Lien utile du propriétaire (extranet Booking, déclaration Siblu, calendrier iCal…). */
public record LinkRequestDto(
        @NotBlank @Size(max = 60)
        String name,

        /* http(s) uniquement : un lien « javascript: » ou « data: » serait exécuté au clic (XSS). */
        @NotBlank @Size(max = 500)
        @Pattern(regexp = "^https?://\\S+$", message = "URL http(s) attendue")
        String url
) {
}
