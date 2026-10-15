package fr.eric.mobilhomy.controller;

import fr.eric.mobilhomy.bll.ReservationService;
import fr.eric.mobilhomy.bo.Auth;
import fr.eric.mobilhomy.dto.ReservationVacationerRequestDto;
import fr.eric.mobilhomy.dto.ReservationVacationerResponseDto;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/vacationer/reservations")
public class ReservationVacationerController {

    private final ReservationService reservationService;

    @PostMapping
    public ResponseEntity<ReservationVacationerResponseDto> addReservation(
            @Valid @RequestBody ReservationVacationerRequestDto req,
            @AuthenticationPrincipal Auth auth
            ){
        Integer userId = auth.getUtilisateur().getId();
        ReservationVacationerResponseDto res = reservationService.addReservation(userId, req);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/mine")
    public ResponseEntity<List<ReservationVacationerResponseDto>> getReservationsByUserId(
            @AuthenticationPrincipal Auth auth
    ){
        Integer userId = auth.getUtilisateur().getId();
        List<ReservationVacationerResponseDto> res = reservationService.getReservationsByUserId(userId);
        return ResponseEntity.ok(res);
    }
}
