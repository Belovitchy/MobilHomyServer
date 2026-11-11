package fr.eric.mobilhomy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.eric.mobilhomy.bo.Mobilhome;
import fr.eric.mobilhomy.bo.Reservation;
import fr.eric.mobilhomy.bo.ReservationStatus;
import fr.eric.mobilhomy.bo.Vacationer;

import java.util.List;

/**
 * Séjour confirmé vu par le gérant. Contrairement à la vue propriétaire, elle contient les
 * coordonnées du vacancier et la plaque : le gérant en a besoin pour l'accueil et l'accès au camping.
 * Elle n'est construite QUE pour des réservations VALIDATED (filtrage fait dans ManagerService).
 */
public record ManagerReservationResponseDto(
        Integer id,
        @JsonProperty("mobil_home_id") Integer mobilhomeId,
        @JsonProperty("mobil_home_title") String mobilhomeTitle,
        String location,
        @JsonProperty("start_date") String startDate,
        @JsonProperty("end_date") String endDate,
        ReservationStatus status,
        @JsonProperty("vacationer_name") String vacationerName,
        @JsonProperty("vacationer_phone") String vacationerPhone,
        @JsonProperty("vacationer_email") String vacationerEmail,
        String immat,
        @JsonProperty("occupants_count") int occupantsCount,
        List<OccupantResponseDto> occupants
) {
    public static ManagerReservationResponseDto fromEntity(Reservation r) {
        Mobilhome m = r.getMobilhome();
        Vacationer v = r.getVacationer();
        List<OccupantResponseDto> occupants = r.getOccupants() != null
                ? r.getOccupants().stream().map(OccupantResponseDto::fromOccupant).toList()
                : List.of();
        return new ManagerReservationResponseDto(
                r.getId(),
                m.getId(),
                m.getName(),
                m.getLocation(),
                r.getStartDate() != null ? r.getStartDate().toString() : null,
                r.getEndDate() != null ? r.getEndDate().toString() : null,
                r.getStatus(),
                v != null ? v.getFirstname() + " " + v.getName() : null,
                v != null ? v.getPhone() : null,
                v != null && v.getAuth() != null ? v.getAuth().getLogin() : null,
                r.getImmat(),
                occupants.isEmpty() ? r.getNumberOfPerson() : occupants.size(),
                occupants
        );
    }
}
