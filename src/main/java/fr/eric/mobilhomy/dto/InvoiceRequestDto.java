package fr.eric.mobilhomy.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.eric.mobilhomy.bo.InvoiceCategory;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Saisie d'une facture de fonctionnement par le gérant. */
public record InvoiceRequestDto(
        @NotNull(message = "Mobil-home obligatoire")
        @JsonProperty("mobil_home_id") @JsonAlias("mobilhomeId")
        Integer mobilhomeId,

        @NotNull(message = "Catégorie obligatoire")
        InvoiceCategory category,

        @NotBlank @Size(max = 255)
        String description,

        @NotNull @Positive @DecimalMax("99999.99") @Digits(integer = 5, fraction = 2)
        BigDecimal amount,

        /* Date de la prestation / de l'achat : pas de facture « dans le futur ». */
        @NotNull @PastOrPresent
        LocalDate date
) {
}
