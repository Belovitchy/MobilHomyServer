package fr.eric.mobilhomy.controller;

import fr.eric.mobilhomy.bll.ManagerService;
import fr.eric.mobilhomy.bo.Auth;
import fr.eric.mobilhomy.dto.*;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API de l'espace gérant. Accès limité au rôle MANAGER par MobilHomySecurityConfig (/api/manager/**) ;
 * le contrôle « ce mobil-home / cette facture est-il à lui ? » est fait dans ManagerService.
 */
@RestController
@AllArgsConstructor
@RequestMapping("/api/manager")
public class ManagerController {

    private final ManagerService managerService;

    // ----- Réservations confirmées des mobil-homes gérés -----

    @GetMapping("/reservations")
    public ResponseEntity<List<ManagerReservationResponseDto>> getReservations(@AuthenticationPrincipal Auth auth) {
        return ResponseEntity.ok(managerService.getReservations(managerId(auth)));
    }

    // ----- Mobil-homes gérés -----

    @GetMapping("/mobilhomes")
    public ResponseEntity<List<ManagerMobilhomeResponseDto>> getMobilhomes(@AuthenticationPrincipal Auth auth) {
        return ResponseEntity.ok(managerService.getMobilhomes(managerId(auth)));
    }

    // ----- Factures -----

    @GetMapping("/invoices")
    public ResponseEntity<List<InvoiceResponseDto>> getInvoices(@AuthenticationPrincipal Auth auth) {
        return ResponseEntity.ok(managerService.getInvoices(managerId(auth)));
    }

    @PostMapping("/invoices")
    public ResponseEntity<InvoiceResponseDto> createInvoice(
            @Valid @RequestBody InvoiceRequestDto req, @AuthenticationPrincipal Auth auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(managerService.createInvoice(managerId(auth), req));
    }

    @PutMapping("/invoices/{id}/cancel")
    public ResponseEntity<InvoiceResponseDto> cancelInvoice(@PathVariable Integer id, @AuthenticationPrincipal Auth auth) {
        return ResponseEntity.ok(managerService.cancelInvoice(managerId(auth), id));
    }

    // ----- Mon compte (mêmes routes et corps que /api/vacationer/account et /api/owner/account) -----

    @GetMapping("/account")
    public ResponseEntity<AccountResDto> getAccount(@AuthenticationPrincipal Auth auth) {
        return ResponseEntity.ok(managerService.getAccount(managerId(auth)));
    }

    @PutMapping("/account")
    public ResponseEntity<AccountResDto> updateAccount(
            @Valid @RequestBody VacationerAccountReqDto req, @AuthenticationPrincipal Auth auth) {
        return ResponseEntity.ok(managerService.updateAccount(managerId(auth), req));
    }

    @PutMapping("/account/email")
    public ResponseEntity<AccountResDto> updateEmail(
            @Valid @RequestBody VacationerMailReqDto req, @AuthenticationPrincipal Auth auth) {
        return ResponseEntity.ok(managerService.updateEmail(managerId(auth), req));
    }

    @PutMapping("/account/password")
    public ResponseEntity<AccountResDto> updatePassword(
            @Valid @RequestBody VacationerPassReqDto req, @AuthenticationPrincipal Auth auth) {
        return ResponseEntity.ok(managerService.updatePassword(managerId(auth), req));
    }

    private static Integer managerId(Auth auth) {
        return auth.getUtilisateur().getId();
    }
}
