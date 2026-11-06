package fr.eric.mobilhomy.controller;

import fr.eric.mobilhomy.bll.VacationerService;
import fr.eric.mobilhomy.bo.Auth;
import fr.eric.mobilhomy.dto.*;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@RequestMapping("/api/vacationer/account")
public class VacationerAccountController {

    private final VacationerService vacationerService;

    @GetMapping
    public ResponseEntity<VacationerAccountResDto> getAccount(
            @AuthenticationPrincipal Auth auth
            ) {
        Integer userId = auth.getUtilisateur().getId();
        VacationerAccountResDto res = vacationerService.getAccount(userId);
        return ResponseEntity.ok(res);
    }

    @PutMapping
    public ResponseEntity<VacationerAccountResDto> updateAccount(
            @Valid @RequestBody VacationerAccountReqDto req,
            @AuthenticationPrincipal Auth auth
    ) {
        Integer userId = auth.getUtilisateur().getId();
        VacationerAccountResDto res = vacationerService.updateAccount(userId, req);
        return ResponseEntity.ok(res);
    }

    @PutMapping("/email")
    public ResponseEntity<VacationerAccountResDto> updateEmail(
            @Valid @RequestBody VacationerMailReqDto req,
            @AuthenticationPrincipal Auth auth
            ){
        Integer userId = auth.getUtilisateur().getId();
        VacationerAccountResDto res = vacationerService.updateEmail(userId, req);
        return ResponseEntity.ok(res);
    }

    @PutMapping("/password")
    public ResponseEntity<VacationerAccountResDto> updatePassword(
            @Valid @RequestBody VacationerPassReqDto req,
            @AuthenticationPrincipal Auth auth
            ){
        Integer userId = auth.getUtilisateur().getId();
        VacationerAccountResDto res = vacationerService.updatePassword(userId, req);
        return ResponseEntity.ok(res);
    }
}
