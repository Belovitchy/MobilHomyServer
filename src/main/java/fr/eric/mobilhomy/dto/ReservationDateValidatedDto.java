package fr.eric.mobilhomy.dto;

import fr.eric.mobilhomy.bo.Reservation;
import java.time.LocalDate;

public record ReservationDateValidatedDto(
        LocalDate startDate,
        LocalDate endDate
) {
    public static ReservationDateValidatedDto fromEntity(Reservation r) {
        return new ReservationDateValidatedDto(
                r.getStartDate(),
                r.getEndDate()
        );
    }
}