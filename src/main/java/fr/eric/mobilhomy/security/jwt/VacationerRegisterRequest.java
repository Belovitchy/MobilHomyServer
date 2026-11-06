package fr.eric.mobilhomy.security.jwt;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VacationerRegisterRequest {

    @NotNull
    private String firstname;
    @NotNull
    private String name;
    @NotNull
    private String email;
    @Nullable
    private String phone;
    @NotNull
    private String password;
}
