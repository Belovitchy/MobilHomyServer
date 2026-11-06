package fr.eric.mobilhomy.controller;

import fr.eric.mobilhomy.bll.VacationerService;
import fr.eric.mobilhomy.bo.Auth;
import fr.eric.mobilhomy.dto.VacationerAccountResDto;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
