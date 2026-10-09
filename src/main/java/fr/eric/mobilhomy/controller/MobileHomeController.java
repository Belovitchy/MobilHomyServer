package fr.eric.mobilhomy.controller;

import fr.eric.mobilhomy.bll.MobilhomeService;
import fr.eric.mobilhomy.dto.MobilhomeVacationerResponseDto;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/mobilhomes")
public class MobileHomeController {

    private final MobilhomeService mobilhomeService;

    @GetMapping
    public ResponseEntity<List<MobilhomeVacationerResponseDto>> getAll(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) Integer capacity
    ){
        List<MobilhomeVacationerResponseDto> response = mobilhomeService.search(department, capacity)
                .stream()
                .map(MobilhomeVacationerResponseDto::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MobilhomeVacationerResponseDto> getById(@PathVariable Integer id){
        return ResponseEntity.ok(MobilhomeVacationerResponseDto.fromEntity(mobilhomeService.getById(id)));
    }
}
