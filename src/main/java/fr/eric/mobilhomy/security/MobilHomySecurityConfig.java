package fr.eric.mobilhomy.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import lombok.AllArgsConstructor;

import fr.eric.mobilhomy.security.jwt.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
@AllArgsConstructor
public class MobilHomySecurityConfig {

    private JwtAuthenticationFilter jwtAuthenticationFilter;
    private AuthenticationProvider authenticationProvider;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // API REST Stateless -> CSRF inutile
        http.csrf(csrf -> csrf.disable());

        http.authorizeHttpRequests(auth -> auth
                // Authentification : tout le monde
                .requestMatchers("/api/auth/**").permitAll()
                // Consultation libre des annonces (accès sans compte, cf. cahier des charges)
                .requestMatchers(HttpMethod.GET, "/api/mobilhomes/**").permitAll()
                // Vacancier : demandes de réservation
                .requestMatchers("/api/reservations/**").hasAuthority("VACATIONER")
                // Propriétaire : mobil-homes, gérants, validation, iCal
                .requestMatchers("/api/proprietaire/**").hasAuthority("OWNER")
                // Gérant : réservations, factures
                .requestMatchers("/api/gerant/**").hasAuthority("MANAGER")
                // Administrateur : comptes propriétaires
                .requestMatchers("/api/admin/**").hasAuthority("ADMIN")
                .anyRequest().authenticated()
        );

        http.authenticationProvider(authenticationProvider);
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // Session Stateless
        http.sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }
}