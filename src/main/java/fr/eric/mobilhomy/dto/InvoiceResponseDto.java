package fr.eric.mobilhomy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.eric.mobilhomy.bo.Invoice;
import fr.eric.mobilhomy.bo.InvoiceCategory;
import fr.eric.mobilhomy.bo.InvoiceStatus;
import fr.eric.mobilhomy.bo.Mobilhome;

import java.math.BigDecimal;

public record InvoiceResponseDto(
        Integer id,
        @JsonProperty("mobil_home_id") Integer mobilhomeId,
        @JsonProperty("mobil_home_title") String mobilhomeTitle,
        String description,
        BigDecimal amount,
        String date,
        InvoiceCategory category,
        InvoiceStatus status
) {
    public static InvoiceResponseDto fromEntity(Invoice i) {
        Mobilhome m = i.getMobilhome();
        return new InvoiceResponseDto(
                i.getId(),
                m != null ? m.getId() : null,
                m != null ? m.getName() : null,
                i.getDescription(),
                i.getAmount(),
                i.getDate() != null ? i.getDate().toString() : null,
                i.getCategory(),
                i.getStatus()
        );
    }
}
