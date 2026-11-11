package fr.eric.mobilhomy.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Modification par le propriétaire du client, de la plaque et des occupants d'une réservation externe. */
public record ReservationDetailsRequestDto(
        @NotBlank @Size(max = 120) @JsonProperty("guest_name") @JsonAlias("guestName")
        String guestName,

        @Pattern(regexp = "^$|^[0-9 +.()-]{6,20}$", message = "Numéro de téléphone invalide")
        @JsonProperty("guest_phone") @JsonAlias("guestPhone")
        String guestPhone,

        @Pattern(regexp = "^$|^[A-Za-z0-9 -]{4,15}$", message = "Plaque invalide")
        String immat,

        @Size(max = 16) @Valid
        List<OwnerOccupantDto> occupants
) {
}
