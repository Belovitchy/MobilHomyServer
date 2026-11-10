package fr.eric.mobilhomy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.eric.mobilhomy.bo.Reservation;
import fr.eric.mobilhomy.bo.ReservationStatus;

/**
 * Événement de calendrier (CalendarEvent côté Angular).
 * source = INTERNAL pour les réservations MobilHomy ; ICAL est réservé à la future synchro externe.
 */
public record CalendarEventDto(
        Integer id,
        @JsonProperty("mobil_home_id") Integer mobilhomeId,
        String start,
        String end,
        String label,
        ReservationStatus status,
        String source
) {
    public static CalendarEventDto fromReservation(Reservation r) {
        String who = r.getVacationer() != null ? r.getVacationer().getName() : "Réservation";
        return new CalendarEventDto(
                r.getId(),
                r.getMobilhome().getId(),
                r.getStartDate().toString(),
                r.getEndDate().toString(),
                r.getMobilhome().getName() + " — " + who,
                r.getStatus(),
                "INTERNAL"
        );
    }
}
