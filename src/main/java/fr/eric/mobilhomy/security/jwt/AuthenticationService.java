package fr.eric.mobilhomy.security.jwt;

import fr.eric.mobilhomy.bo.Auth;
import fr.eric.mobilhomy.bo.RolesEnum;
import fr.eric.mobilhomy.bo.Vacationer;
import fr.eric.mobilhomy.dal.AuthRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@AllArgsConstructor
public class AuthenticationService {

    private AuthRepository authRepository;
    private AuthenticationManager authenticationManager;
    private JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        // Lève une exception si login/mot de passe incorrects
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getLogin(), request.getPassword()));

        Auth auth = authRepository.findByLogin(request.getLogin()).orElseThrow();
        return new AuthenticationResponse(
                jwtService.generateToken(auth),
                auth.getUtilisateur().getId(),
                auth.getUtilisateur().getFirstname(),
                auth.getUtilisateur().getName(),
                auth.getAuthority());

    }

    public void registerVacationer(VacationerRegisterRequest request) {
        if (authRepository.findByLogin(request.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email déjà utilisé");
        }
        Vacationer vacationer = Vacationer.builder()
                .firstname(request.getFirstname())
                .name(request.getName())
                .phone(request.getPhone())
                .build();

        Auth auth = Auth.builder()
                .login(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .authority(RolesEnum.VACATIONER)
                .utilisateur(vacationer)
                .build();

        authRepository.save(auth);
    }
}
