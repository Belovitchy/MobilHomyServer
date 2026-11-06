package fr.eric.mobilhomy.dto;

import jakarta.validation.constraints.NotBlank;

public record VacationerPassReqDto(
        @NotBlank String currentPassword,
        @NotBlank String newPassword
) {
}
