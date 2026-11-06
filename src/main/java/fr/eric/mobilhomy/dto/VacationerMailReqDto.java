package fr.eric.mobilhomy.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record VacationerMailReqDto(
        @NotBlank @Email String newEmail,
        @NotBlank String currentPassword
) {
}
