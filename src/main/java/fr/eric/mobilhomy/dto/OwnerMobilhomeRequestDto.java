package fr.eric.mobilhomy.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Création / modification d'un mobil-home par son propriétaire.
 * Noms JSON alignés sur OwnerMobilHomeInput (Angular).
 */
public record OwnerMobilhomeRequestDto(
        @NotBlank @Size(max = 120)
        @JsonProperty("title") @JsonAlias("name")
        String title,

        @Size(max = 2000)
        String description,

        /* Nom du camping (ex : « La Réserve ») — optionnel, absent du formulaire Angular actuel. */
        @Size(max = 255)
        String location,

        @NotBlank @Size(max = 255)
        String address,

        @NotBlank @Pattern(regexp = "^\\d{5}$", message = "Code postal sur 5 chiffres")
        @JsonProperty("postal_code") @JsonAlias("postalCode")
        String postalCode,

        /* Envoyé par le front mais ignoré : le département est déduit du code postal (source unique de vérité). */
        String department,

        @Min(1) @Max(16)
        int capacity,

        @NotNull @PositiveOrZero @Digits(integer = 8, fraction = 2)
        BigDecimal price,

        @Size(max = 500)
        @JsonProperty("ical_link") @JsonAlias("icalLink")
        String icalLink,

        /* Gérant affecté — doit faire partie des gérants du propriétaire. */
        @JsonProperty("manager_id") @JsonAlias("managerId")
        Integer managerId,

        /* Chemins servis par /images/** ou URL https — rien d'autre (pas de javascript:, pas de ../). */
        @Size(max = 10)
        List<@NotBlank @Size(max = 255)
             @Pattern(regexp = "^(/images/[\\w.-]+|https://\\S+)$", message = "Chemin /images/... ou URL https")
             String> images
) {
}
