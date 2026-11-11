package fr.eric.mobilhomy.controller;

import fr.eric.mobilhomy.bll.ImageStorageService;
import fr.eric.mobilhomy.bll.OwnerService;
import fr.eric.mobilhomy.bo.Auth;
import fr.eric.mobilhomy.bo.ReservationStatus;
import fr.eric.mobilhomy.dto.*;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

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
    private final ImageStorageService imageStorageService;

    // ----- Images -----

    /**
     * Téléverse une photo (multipart, champ "file") dans uploads/images et renvoie son chemin public.
     * Le front ajoute ensuite ce chemin à la liste "images" du mobil-home lors de l'enregistrement.
     */
    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("path", imageStorageService.store(file)));
    }

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

    @PutMapping("/reservations/{id}/siblu-ref")
    public ResponseEntity<OwnerReservationResponseDto> updateSibluRef(
            @PathVariable Integer id,
            @Valid @RequestBody SibluRefRequestDto req,
            @AuthenticationPrincipal Auth auth) {
        return ResponseEntity.ok(ownerService.updateSibluRef(ownerId(auth), id, req));
    }

    // ----- Réservations externes (Booking, Airbnb, Abritel…) -----

    @PostMapping("/reservations")
    public ResponseEntity<OwnerReservationResponseDto> createExternalReservation(
            @Valid @RequestBody ExternalReservationRequestDto req, @AuthenticationPrincipal Auth auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ownerService.createExternalReservation(ownerId(auth), req));
    }

    @PutMapping("/reservations/{id}/details")
    public ResponseEntity<OwnerReservationResponseDto> updateReservationDetails(
            @PathVariable Integer id,
            @Valid @RequestBody ReservationDetailsRequestDto req,
            @AuthenticationPrincipal Auth auth) {
        return ResponseEntity.ok(ownerService.updateReservationDetails(ownerId(auth), id, req));
    }

    @PutMapping("/reservations/{id}/cancel")
    public ResponseEntity<OwnerReservationResponseDto> cancelExternalReservation(
            @PathVariable Integer id, @AuthenticationPrincipal Auth auth) {
        return ResponseEntity.ok(ownerService.cancelExternalReservation(ownerId(auth), id));
    }

    // ----- Liens utiles -----

    @GetMapping("/links")
    public ResponseEntity<List<LinkResponseDto>> getLinks(@AuthenticationPrincipal Auth auth) {
        return ResponseEntity.ok(ownerService.getLinks(ownerId(auth)));
    }

    @PostMapping("/links")
    public ResponseEntity<LinkResponseDto> createLink(@Valid @RequestBody LinkRequestDto req, @AuthenticationPrincipal Auth auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ownerService.createLink(ownerId(auth), req));
    }

    @PutMapping("/links/{id}")
    public ResponseEntity<LinkResponseDto> updateLink(
            @PathVariable Integer id, @Valid @RequestBody LinkRequestDto req, @AuthenticationPrincipal Auth auth) {
        return ResponseEntity.ok(ownerService.updateLink(ownerId(auth), id, req));
    }

    @DeleteMapping("/links/{id}")
    public ResponseEntity<Void> deleteLink(@PathVariable Integer id, @AuthenticationPrincipal Auth auth) {
        ownerService.deleteLink(ownerId(auth), id);
        return ResponseEntity.noContent().build();
    }

    // ----- Calendrier -----

    @GetMapping("/calendar")
    public ResponseEntity<List<CalendarEventDto>> getCalendar(
            @RequestParam(required = false) Integer mobilHomeId,
            @AuthenticationPrincipal Auth auth) {
        return ResponseEntity.ok(ownerService.getCalendar(ownerId(auth), mobilHomeId));
    }

    // ----- Mon compte (mêmes routes et mêmes corps que /api/vacationer/account) -----

    @GetMapping("/account")
    public ResponseEntity<OwnerAccountResDto> getAccount(@AuthenticationPrincipal Auth auth) {
        return ResponseEntity.ok(ownerService.getAccount(ownerId(auth)));
    }

    @PutMapping("/account")
    public ResponseEntity<OwnerAccountResDto> updateAccount(
            @Valid @RequestBody VacationerAccountReqDto req, @AuthenticationPrincipal Auth auth) {
        return ResponseEntity.ok(ownerService.updateAccount(ownerId(auth), req));
    }

    @PutMapping("/account/email")
    public ResponseEntity<OwnerAccountResDto> updateEmail(
            @Valid @RequestBody VacationerMailReqDto req, @AuthenticationPrincipal Auth auth) {
        return ResponseEntity.ok(ownerService.updateEmail(ownerId(auth), req));
    }

    @PutMapping("/account/password")
    public ResponseEntity<OwnerAccountResDto> updatePassword(
            @Valid @RequestBody VacationerPassReqDto req, @AuthenticationPrincipal Auth auth) {
        return ResponseEntity.ok(ownerService.updatePassword(ownerId(auth), req));
    }

    private static Integer ownerId(Auth auth) {
        return auth.getUtilisateur().getId();
    }
}
