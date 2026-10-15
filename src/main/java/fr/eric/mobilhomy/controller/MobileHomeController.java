package fr.eric.mobilhomy.controller;

import fr.eric.mobilhomy.bll.MobilhomeService;
import fr.eric.mobilhomy.dto.MobilhomeVisitorResponseDto;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api")
public class MobileHomeController {

    private final MobilhomeService mobilhomeService;

    @GetMapping("/mobilhomes")
    public ResponseEntity<List<MobilhomeVisitorResponseDto>> getAll(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) Integer capacity
    ){
        List<MobilhomeVisitorResponseDto> response = mobilhomeService.search(department, capacity)
                .stream()
                .map(MobilhomeVisitorResponseDto::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("mobilhomes/{id}")
    public ResponseEntity<MobilhomeVisitorResponseDto> getById(@PathVariable Integer id){
        return ResponseEntity.ok(MobilhomeVisitorResponseDto.fromEntity(mobilhomeService.getById(id)));
    }
}
