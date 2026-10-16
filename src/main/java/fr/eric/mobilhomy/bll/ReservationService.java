package fr.eric.mobilhomy.bll;

import fr.eric.mobilhomy.bo.Reservation;
import fr.eric.mobilhomy.dto.ReservationVacationerRequestDto;
import fr.eric.mobilhomy.dto.ReservationVacationerResponseDto;
import fr.eric.mobilhomy.dto.ReservationVacationerUpdateRequestDto;

import java.util.List;

public interface ReservationService {
    ReservationVacationerResponseDto addReservation(Integer userId, ReservationVacationerRequestDto req);

    List<ReservationVacationerResponseDto> getReservationsByUserId(Integer userId);

    ReservationVacationerResponseDto updateReservation(Integer userId, Integer resaId, ReservationVacationerUpdateRequestDto req);

    ReservationVacationerResponseDto submitReservation(Integer userId, Integer resaId, ReservationVacationerUpdateRequestDto req);
}
