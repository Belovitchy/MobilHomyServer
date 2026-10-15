package fr.eric.mobilhomy.dto;

import fr.eric.mobilhomy.bo.Reservation;
import fr.eric.mobilhomy.bo.ReservationStatus;
import jakarta.annotation.Nullable;

public record ReservationVacationerResponseDto(
        Integer id,
        String mobilhomeName,
        String location,
        String start_date,
        String  end_date,
        ReservationStatus status,
        Integer contractId,
        boolean contractSigned
        // Renseignés par le back UNIQUEMENT si confirmée (RG-07)
//        @Nullable
//        String managerFirstname,
//        @Nullable
//        String managerName,
//        @Nullable
//        String managerPhone,
//        @Nullable
//        String managerEmail

) {
    public static ReservationVacationerResponseDto fromEntity(Reservation reservation) {
        return new ReservationVacationerResponseDto(
                reservation.getId(),
                reservation.getMobilhome().getName(),
                reservation.getMobilhome().getLocation(),
                reservation.getStartDate().toString(),
                reservation.getEndDate().toString(),
                reservation.getStatus(),
                reservation.getContract() == null ? null : reservation.getContract().getId(),
                reservation.getContract() != null && reservation.getContract().isSigned()
                );
    }
}
