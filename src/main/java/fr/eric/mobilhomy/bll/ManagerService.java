package fr.eric.mobilhomy.bll;

import fr.eric.mobilhomy.dto.*;

import java.util.List;

/**
 * Cas d'utilisation du gérant. L'id du gérant vient toujours du JWT ; chaque méthode
 * ne touche qu'aux mobil-homes qu'IL gère et aux factures qu'IL a saisies.
 */
public interface ManagerService {

    // ----- Réservations (VALIDATED uniquement) -----
    List<ManagerReservationResponseDto> getReservations(Integer managerId);

    // ----- Mobil-homes gérés -----
    List<ManagerMobilhomeResponseDto> getMobilhomes(Integer managerId);

    // ----- Factures -----
    List<InvoiceResponseDto> getInvoices(Integer managerId);

    InvoiceResponseDto createInvoice(Integer managerId, InvoiceRequestDto req);

    InvoiceResponseDto cancelInvoice(Integer managerId, Integer invoiceId);

    // ----- Mon compte -----
    AccountResDto getAccount(Integer managerId);

    AccountResDto updateAccount(Integer managerId, VacationerAccountReqDto req);

    AccountResDto updateEmail(Integer managerId, VacationerMailReqDto req);

    AccountResDto updatePassword(Integer managerId, VacationerPassReqDto req);
}
