package fr.eric.mobilhomy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.eric.mobilhomy.bo.Mobilhome;
import fr.eric.mobilhomy.bo.Reservation;
import fr.eric.mobilhomy.bo.ReservationStatus;
import fr.eric.mobilhomy.bo.ReservationSource;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Vue propriétaire d'une réservation — noms JSON alignés sur OwnerReservation (Angular).
 * Volontairement sans téléphone ni email du vacancier (minimisation RGPD) : le propriétaire
 * valide sur les dates et la composition du groupe. La plaque est fournie car elle est
 * nécessaire à la déclaration du séjour au camping, qui délivre en retour le numéro Siblu.
 */
public record OwnerReservationResponseDto(
        Integer id,
        @JsonProperty("mobil_home_id") Integer mobilhomeId,
        @JsonProperty("mobil_home_title") String mobilhomeTitle,
        @JsonProperty("start_date") String startDate,
        @JsonProperty("end_date") String endDate,
        ReservationStatus status,
        /* MOBILHOMY ou plateforme externe (BOOKING, AIRBNB…) : seules les externes sont modifiables. */
        ReservationSource source,
        @JsonProperty("occupant_name") String occupantName,
        /* Téléphone du client externe (saisi par le propriétaire lui-même) ; null pour un vacancier MobilHomy. */
        @JsonProperty("guest_phone") String guestPhone,
        @JsonProperty("occupants_count") int occupantsCount,
        @JsonProperty("total_price") BigDecimal totalPrice,
        String immat,
        @JsonProperty("siblu_ref") String sibluRef,
        List<OccupantResponseDto> occupants
) {
    public static OwnerReservationResponseDto fromEntity(Reservation r) {
        Mobilhome m = r.getMobilhome();
        List<OccupantResponseDto> occupants = r.getOccupants() != null
                ? r.getOccupants().stream().map(OccupantResponseDto::fromOccupant).toList()
                : List.of();
        return new OwnerReservationResponseDto(
                r.getId(),
                m.getId(),
                m.getName(),
                r.getStartDate() != null ? r.getStartDate().toString() : null,
                r.getEndDate() != null ? r.getEndDate().toString() : null,
                r.getStatus(),
                r.getSource(),
                r.getClientName(),
                r.isExternal() ? r.getGuestPhone() : null,
                occupants.isEmpty() ? r.getNumberOfPerson() : occupants.size(),
                totalPrice(r),
                r.getImmat(),
                r.getSibluRef(),
                occupants
        );
    }

    /** Prix total = nombre de nuits × prix par nuit (null si une donnée manque). */
    static BigDecimal totalPrice(Reservation r) {
        BigDecimal price = r.getMobilhome().getPrice();
        if (price == null || r.getStartDate() == null || r.getEndDate() == null) {
            return null;
        }
        long nights = Math.max(0, ChronoUnit.DAYS.between(r.getStartDate(), r.getEndDate()));
        return price.multiply(BigDecimal.valueOf(nights));
    }
}
