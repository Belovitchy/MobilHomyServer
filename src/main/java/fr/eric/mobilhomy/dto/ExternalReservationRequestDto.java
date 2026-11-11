package fr.eric.mobilhomy.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.eric.mobilhomy.bo.ReservationSource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Réservation prise sur une autre plateforme (Booking, Airbnb, Abritel…) et saisie par le propriétaire.
 * Elle est confirmée d'office : la plateforme d'origine a déjà encaissé le séjour.
 */
public record ExternalReservationRequestDto(
        @NotNull @JsonProperty("mobil_home_id") @JsonAlias("mobilhomeId")
        Integer mobilhomeId,

        @NotNull
        ReservationSource source,

        @NotBlank @Size(max = 120) @JsonProperty("guest_name") @JsonAlias("guestName")
        String guestName,

        @Pattern(regexp = "^$|^[0-9 +.()-]{6,20}$", message = "Numéro de téléphone invalide")
        @JsonProperty("guest_phone") @JsonAlias("guestPhone")
        String guestPhone,

        @NotNull @JsonProperty("start_date") @JsonAlias("startDate")
        LocalDate startDate,

        @NotNull @JsonProperty("end_date") @JsonAlias("endDate")
        LocalDate endDate,

        @Pattern(regexp = "^$|^[A-Za-z0-9 -]{4,15}$", message = "Plaque invalide")
        String immat,

        @Size(max = 16) @Valid
        List<OwnerOccupantDto> occupants
) {
}
