package fr.eric.mobilhomy.dto;

import java.util.List;

public record ReservationVacationerUpdateRequestDto(
        String immat,
        List<OccupantRequestDto> occupants
) {
}
