package fr.eric.mobilhomy.security.jwt;

import fr.eric.mobilhomy.bo.Auth;
import fr.eric.mobilhomy.dal.AuthRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthenticationService {

    private AuthRepository authRepository;
    private AuthenticationManager authenticationManager;
    private JwtService jwtService;

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        // Lève une exception si login/mot de passe incorrects
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getLogin(), request.getPassword()));

        Auth auth = authRepository.findByLogin(request.getLogin()).orElseThrow();
        return new AuthenticationResponse(
                jwtService.generateToken(auth),
                auth.getUtilisateur().getFirstname(),
                auth.getUtilisateur().getName(),
                auth.getAuthority());

    }
}
