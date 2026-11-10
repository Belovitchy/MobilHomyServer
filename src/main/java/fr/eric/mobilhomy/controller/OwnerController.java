package fr.eric.mobilhomy.controller;

import fr.eric.mobilhomy.bll.OwnerService;
import fr.eric.mobilhomy.bo.Auth;
import fr.eric.mobilhomy.bo.ReservationStatus;
import fr.eric.mobilhomy.dto.*;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API de l'espace propriétaire. L'accès est limité au rôle OWNER par MobilHomySecurityConfig
 * (/api/owner/**) ; le contrôle « cette ressource est-elle à lui ? » est fait dans OwnerService.
 * L'id du propriétaire vient toujours du JWT, jamais du corps de la requête.
 */
@RestController
@AllArgsConstructor
@RequestMapping("/api/owner")
public class OwnerController {

    private final OwnerService ownerService;

    // ----- Mobil-homes -----

    @GetMapping("/mobilhomes")
    public ResponseEntity<List<OwnerMobilhomeResponseDto>> getMobilhomes(@AuthenticationPrincipal Auth auth) {
        return ResponseEntity.ok(ownerService.getMobilhomes(ownerId(auth)));
    }

    @PostMapping("/mobilhomes")
    public ResponseEntity<OwnerMobilhomeResponseDto> createMobilhome(
            @Valid @RequestBody OwnerMobilhomeRequestDto req,
            @AuthenticationPrincipal Auth auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ownerService.createMobilhome(ownerId(auth), req));
    }

    @PutMapping("/mobilhomes/{id}")
    public ResponseEntity<OwnerMobilhomeResponseDto> updateMobilhome(
            @PathVariable Integer id,
            @Valid @RequestBody OwnerMobilhomeRequestDto req,
            @AuthenticationPrincipal Auth auth) {
        return ResponseEntity.ok(ownerService.updateMobilhome(ownerId(auth), id, req));
    }

    @DeleteMapping("/mobilhomes/{id}")
    public ResponseEntity<Void> deleteMobilhome(@PathVariable Integer id, @AuthenticationPrincipal Auth auth) {
        ownerService.deleteMobilhome(ownerId(auth), id);
        return ResponseEntity.noContent().build();
    }

    // ----- Gérants -----

    @GetMapping("/managers")
    public ResponseEntity<List<ManagerResponseDto>> getManagers(@AuthenticationPrincipal Auth auth) {
        return ResponseEntity.ok(ownerService.getManagers(ownerId(auth)));
    }

    @PostMapping("/managers")
    public ResponseEntity<ManagerResponseDto> createManager(
            @Valid @RequestBody OwnerManagerRequestDto req,
            @AuthenticationPrincipal Auth auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ownerService.createManager(ownerId(auth), req));
    }

    @PutMapping("/managers/{id}")
    public ResponseEntity<ManagerResponseDto> updateManager(
            @PathVariable Integer id,
            @Valid @RequestBody OwnerManagerRequestDto req,
            @AuthenticationPrincipal Auth auth) {
        return ResponseEntity.ok(ownerService.updateManager(ownerId(auth), id, req));
    }

    @DeleteMapping("/managers/{id}")
    public ResponseEntity<Void> deleteManager(@PathVariable Integer id, @AuthenticationPrincipal Auth auth) {
        ownerService.deleteManager(ownerId(auth), id);
        return ResponseEntity.noContent().build();
    }

    // ----- Réservations -----

    @GetMapping("/reservations")
    public ResponseEntity<List<OwnerReservationResponseDto>> getReservations(
            @RequestParam(required = false) ReservationStatus status,
            @AuthenticationPrincipal Auth auth) {
        return ResponseEntity.ok(ownerService.getReservations(ownerId(auth), status));
    }

    @PutMapping("/reservations/{id}/validate")
    public ResponseEntity<OwnerReservationResponseDto> validateReservation(
            @PathVariable Integer id, @AuthenticationPrincipal Auth auth) {
        return ResponseEntity.ok(ownerService.validateReservation(ownerId(auth), id));
    }

    @PutMapping("/reservations/{id}/reject")
    public ResponseEntity<OwnerReservationResponseDto> rejectReservation(
            @PathVariable Integer id, @AuthenticationPrincipal Auth auth) {
        return ResponseEntity.ok(ownerService.rejectReservation(ownerId(auth), id));
    }

    // ----- Calendrier -----

    @GetMapping("/calendar")
    public ResponseEntity<List<CalendarEventDto>> getCalendar(
            @RequestParam(required = false) Integer mobilHomeId,
            @AuthenticationPrincipal Auth auth) {
        return ResponseEntity.ok(ownerService.getCalendar(ownerId(auth), mobilHomeId));
    }

    private static Integer ownerId(Auth auth) {
        return auth.getUtilisateur().getId();
    }
}
