package fr.eric.mobilhomy.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Numéro de réservation délivré par le camping Siblu après la déclaration du séjour (ex : SBLU-2026-0042). */
public record SibluRefRequestDto(
        @NotBlank
        @Size(max = 30)
        @Pattern(regexp = "^\\s*[A-Za-z0-9][A-Za-z0-9 /-]*\\s*$", message = "Lettres, chiffres, tirets uniquement")
        @JsonProperty("siblu_ref") @JsonAlias("sibluRef")
        String sibluRef
) {
}
