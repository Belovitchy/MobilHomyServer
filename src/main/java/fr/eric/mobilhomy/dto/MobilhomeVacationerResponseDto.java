package fr.eric.mobilhomy.dto;

import fr.eric.mobilhomy.bo.Image;
import fr.eric.mobilhomy.bo.Mobilhome;
import fr.eric.mobilhomy.bo.ReservationStatus;

import java.math.BigDecimal;
import java.util.List;

public record MobilhomeVacationerResponseDto(
        Integer id,
        String name,
        String description,
        String location,
        int capacity,
        String address,
        String postalCode,
        BigDecimal price,
        List<String> images,
        List<ReservationDateValidatedDto> reservations
) {
    public static MobilhomeVacationerResponseDto fromEntity(Mobilhome m) {
        return new MobilhomeVacationerResponseDto(
                m.getId(),
                m.getName(),
                m.getDescription(),
                m.getLocation(),
                m.getCapacity(),
                m.getAddress(),
                m.getPostalCode(),
                m.getPrice(),
                m.getImages() != null ? m.getImages().stream().map(Image::getPath).toList() : List.of(),
                m.getReservations() != null
                        ? m.getReservations().stream()
                        .filter(r -> r.getStatus() == ReservationStatus.VALIDATED)
                        .map(ReservationDateValidatedDto::fromEntity)
                        .toList()
                        : List.of()
        );
    }
}
