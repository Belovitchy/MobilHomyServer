package fr.eric.mobilhomy.controller;

import fr.eric.mobilhomy.bll.AdminService;
import fr.eric.mobilhomy.dto.AdminOwnerRequestDto;
import fr.eric.mobilhomy.dto.AdminOwnerResponseDto;
import fr.eric.mobilhomy.dto.AdminStatsResponseDto;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Espace administrateur — accès limité au rôle ADMIN par MobilHomySecurityConfig (/api/admin/**). */
@RestController
@AllArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponseDto> getStats() {
        return ResponseEntity.ok(adminService.getStats());
    }

    @GetMapping("/owners")
    public ResponseEntity<List<AdminOwnerResponseDto>> getOwners() {
        return ResponseEntity.ok(adminService.getOwners());
    }

    @PostMapping("/owners")
    public ResponseEntity<AdminOwnerResponseDto> createOwner(@Valid @RequestBody AdminOwnerRequestDto req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createOwner(req));
    }

    @PutMapping("/owners/{id}/suspend")
    public ResponseEntity<AdminOwnerResponseDto> suspend(@PathVariable Integer id) {
        return ResponseEntity.ok(adminService.setOwnerEnabled(id, false));
    }

    @PutMapping("/owners/{id}/reactivate")
    public ResponseEntity<AdminOwnerResponseDto> reactivate(@PathVariable Integer id) {
        return ResponseEntity.ok(adminService.setOwnerEnabled(id, true));
    }
}
