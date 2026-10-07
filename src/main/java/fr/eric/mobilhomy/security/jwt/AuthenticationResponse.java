package fr.eric.mobilhomy.security.jwt;

import fr.eric.mobilhomy.bo.RolesEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationResponse {
    private String token;
    private String firstname;
    private String name;
    private RolesEnum authority;
}
