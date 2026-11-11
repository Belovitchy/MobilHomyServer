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

    /** Enregistre le numéro de réservation Siblu obtenu après déclaration du séjour au camping. */
    OwnerReservationResponseDto updateSibluRef(Integer ownerId, Integer reservationId, SibluRefRequestDto req);

    // ----- Réservations externes (Booking, Airbnb, Abritel…) -----

    /** Saisie d'une réservation prise sur une autre plateforme : confirmée d'office (VALIDATED). */
    OwnerReservationResponseDto createExternalReservation(Integer ownerId, ExternalReservationRequestDto req);

    /** Modification du client, de la plaque et des occupants — réservations externes uniquement. */
    OwnerReservationResponseDto updateReservationDetails(Integer ownerId, Integer reservationId, ReservationDetailsRequestDto req);

    /** Annulation d'une réservation externe (annulée sur la plateforme d'origine, ou saisie par erreur). */
    OwnerReservationResponseDto cancelExternalReservation(Integer ownerId, Integer reservationId);

    // ----- Factures de fonctionnement (saisies par ses gérants) -----
    List<InvoiceResponseDto> getInvoices(Integer ownerId);

    /** Le propriétaire a réglé la facture : ISSUED → PAID. */
    InvoiceResponseDto payInvoice(Integer ownerId, Integer invoiceId);

    // ----- Liens utiles -----
    List<LinkResponseDto> getLinks(Integer ownerId);

    LinkResponseDto createLink(Integer ownerId, LinkRequestDto req);

    LinkResponseDto updateLink(Integer ownerId, Integer linkId, LinkRequestDto req);

    void deleteLink(Integer ownerId, Integer linkId);

    // ----- Calendrier -----
    List<CalendarEventDto> getCalendar(Integer ownerId, Integer mobilhomeId);

    // ----- Mon compte -----
    OwnerAccountResDto getAccount(Integer ownerId);

    OwnerAccountResDto updateAccount(Integer ownerId, VacationerAccountReqDto req);

    OwnerAccountResDto updateEmail(Integer ownerId, VacationerMailReqDto req);

    OwnerAccountResDto updatePassword(Integer ownerId, VacationerPassReqDto req);
}
