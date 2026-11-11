package fr.eric.mobilhomy.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Occupant saisi par le propriétaire (réservation externe) : nom, prénom, âge (FunPass, déclaration camping). */
public record OwnerOccupantDto(
        @NotBlank @Size(max = 100) String firstname,
        @NotBlank @Size(max = 100) String name,
        @Min(0) @Max(120) int age
) {
}
