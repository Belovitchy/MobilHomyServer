package fr.eric.mobilhomy.bll;

import fr.eric.mobilhomy.bo.Reservation;
import fr.eric.mobilhomy.dto.ReservationVacationerRequestDto;
import fr.eric.mobilhomy.dto.ReservationVacationerResponseDto;

import java.util.List;

public interface ReservationService {
    ReservationVacationerResponseDto addReservation(Integer userId, ReservationVacationerRequestDto req);

    List<ReservationVacationerResponseDto> getReservationsByUserId(Integer userId);
}
