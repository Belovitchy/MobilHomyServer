package fr.eric.mobilhomy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import fr.eric.mobilhomy.bo.Occupant;
import fr.eric.mobilhomy.bo.Reservation;
import fr.eric.mobilhomy.bo.ReservationStatus;

import java.util.List;

public record ReservationVacationerResponseDto(
        Integer id,
        String mobilhomeName,
        String location,
        String start_date,
        String  end_date,
        ReservationStatus status,
        String immat,
        @JsonProperty("siblu_ref") String sibluRef,
        List<OccupantResponseDto> occupants,
        ManagerResponseDto manager


) {
    public static ReservationVacationerResponseDto fromEntity(Reservation reservation) {
        return new ReservationVacationerResponseDto(
                reservation.getId(),
                reservation.getMobilhome().getName(),
                reservation.getMobilhome().getLocation(),
                reservation.getStartDate().toString(),
                reservation.getEndDate().toString(),
                reservation.getStatus(),
                reservation.getImmat(),
                // Numéro de réservation camping : communiqué au vacancier uniquement si le séjour est confirmé
                reservation.getStatus() == ReservationStatus.VALIDATED ? reservation.getSibluRef() : null,
                reservation.getOccupants() !=null ?
                        reservation.getOccupants().stream()
                                .map(OccupantResponseDto::fromOccupant)
                                .toList() : List.of(),
                        reservation.getStatus() == ReservationStatus.VALIDATED
                                ? ManagerResponseDto.fromEntity(reservation.getMobilhome().getManager())
                                : null
                );
    }
}
