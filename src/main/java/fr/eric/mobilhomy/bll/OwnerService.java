package fr.eric.mobilhomy.bll;

import fr.eric.mobilhomy.bo.ReservationStatus;
import fr.eric.mobilhomy.dto.*;

import java.util.List;

/**
 * Cas d'utilisation du propriétaire. Chaque méthode reçoit l'id du propriétaire connecté
 * (issu du JWT, jamais du client) et ne manipule que SES ressources.
 */
public interface OwnerService {

    // ----- Mobil-homes -----
    List<OwnerMobilhomeResponseDto> getMobilhomes(Integer ownerId);

    OwnerMobilhomeResponseDto createMobilhome(Integer ownerId, OwnerMobilhomeRequestDto req);

    OwnerMobilhomeResponseDto updateMobilhome(Integer ownerId, Integer mobilhomeId, OwnerMobilhomeRequestDto req);

    void deleteMobilhome(Integer ownerId, Integer mobilhomeId);

    // ----- Gérants -----
    List<ManagerResponseDto> getManagers(Integer ownerId);

    ManagerResponseDto createManager(Integer ownerId, OwnerManagerRequestDto req);

    ManagerResponseDto updateManager(Integer ownerId, Integer managerId, OwnerManagerRequestDto req);

    void deleteManager(Integer ownerId, Integer managerId);

    // ----- Réservations -----
    List<OwnerReservationResponseDto> getReservations(Integer ownerId, ReservationStatus status);

    OwnerReservationResponseDto validateReservation(Integer ownerId, Integer reservationId);

    OwnerReservationResponseDto rejectReservation(Integer ownerId, Integer reservationId);

    // ----- Calendrier -----
    List<CalendarEventDto> getCalendar(Integer ownerId, Integer mobilhomeId);

    // ----- Mon compte -----
    OwnerAccountResDto getAccount(Integer ownerId);

    OwnerAccountResDto updateAccount(Integer ownerId, VacationerAccountReqDto req);

    OwnerAccountResDto updateEmail(Integer ownerId, VacationerMailReqDto req);

    OwnerAccountResDto updatePassword(Integer ownerId, VacationerPassReqDto req);
}
