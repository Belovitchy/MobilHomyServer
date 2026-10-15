package fr.eric.mobilhomy.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record ReservationVacationerRequestDto(
        @NotNull(message = "mobilhomeId is required")
        @JsonProperty("mobilhome_id")
        @JsonAlias({"mobilhomeId", "mobilhome_id"})
        Integer mobilhomeId,

        @JsonProperty("start_date")
        @JsonAlias({"startDate", "start_date"})
        LocalDate startDate,

        @JsonProperty("end_date")
        @JsonAlias({"endDate", "end_date"})
        LocalDate endDate,

        String immat,

        List<OccupantRequestDto> occupants
) {
}
