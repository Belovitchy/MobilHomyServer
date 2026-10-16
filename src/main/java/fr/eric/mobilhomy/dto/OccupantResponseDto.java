package fr.eric.mobilhomy.dto;

import fr.eric.mobilhomy.bo.Occupant;

public record OccupantResponseDto(
        Integer id,
        String firstname,
        String name,
        int age
) {
    public static OccupantResponseDto fromOccupant(Occupant occupant) {
        return new OccupantResponseDto(
                occupant.getId(),
                occupant.getFirstname(),
                occupant.getName(),
                occupant.getAge()
        );
    }
}
