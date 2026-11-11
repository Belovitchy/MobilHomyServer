package fr.eric.mobilhomy.security.jwt;

import fr.eric.mobilhomy.dal.AuthRepository;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Objects;

@Configuration
@AllArgsConstructor
public class JwtMobilHomyConfig {

    private AuthRepository authRepository;

    @Bean
    UserDetailsService userDetailsService(){
        return login -> authRepository.findByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Bean
    AuthenticationProvider authenticationProvider(UserDetailsService userDetailsService,
                                                  PasswordEncoder passwordEncoder){
        return new AuthenticationProvider() {
            @Override
            public Authentication authenticate(@NonNull Authentication authentication) throws AuthenticationException {
                String username = authentication.getName();
                String password = Objects.requireNonNull(authentication.getCredentials()).toString();

                UserDetails user = userDetailsService.loadUserByUsername(username);
                if (!passwordEncoder.matches(password, user.getPassword())) {
                    throw new BadCredentialsException("Bad credentials");
                }
                // Vérifié APRÈS le mot de passe : on ne révèle pas qu'un compte existe et est suspendu
                // à quelqu'un qui ne connaît pas son mot de passe.
                if (!user.isEnabled()) {
                    throw new DisabledException("Compte suspendu");
                }
                return new UsernamePasswordAuthenticationToken(user, password, user.getAuthorities());
            }

            @Override
            public boolean supports(@NonNull Class<?> authentication) {
                return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
            }
        };
    }
    /**
     * Gestionnaire d'authentification basé sur le fournisseur personnalisé
     */
    @Bean
    AuthenticationManager authenticationManager(AuthenticationProvider authenticationProvider) {
        return new ProviderManager(authenticationProvider);
    }

    /**
     * Encodeur de mot de passe
     */
    @Bean
    PasswordEncoder passwordEncoder(){
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
