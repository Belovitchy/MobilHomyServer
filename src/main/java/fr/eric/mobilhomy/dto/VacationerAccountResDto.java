package fr.eric.mobilhomy.dto;

import fr.eric.mobilhomy.bo.Vacationer;
import lombok.Builder;

import java.util.List;

@Builder
public record VacationerAccountResDto(
        Integer id,
        String name,
        String firstname,
        String email,
        String phone,
        List<ReservationVacationerResponseDto> reservations
) {
    public static VacationerAccountResDto fromEntity(Vacationer vacationer) {
        return new VacationerAccountResDto(
                vacationer.getId(),
                vacationer.getName(),
                vacationer.getFirstname(),
                vacationer.getAuth().getLogin(),
                vacationer.getPhone(),
                vacationer.getReservations() !=null ?
                        vacationer.getReservations().stream()
                                .map(ReservationVacationerResponseDto::fromEntity)
                                .toList() : List.of()
        );
    }
}
