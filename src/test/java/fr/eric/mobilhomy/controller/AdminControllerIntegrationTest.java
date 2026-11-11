package fr.eric.mobilhomy.controller;

import com.jayway.jsonpath.JsonPath;
import fr.eric.mobilhomy.bo.Auth;
import fr.eric.mobilhomy.dal.AuthRepository;
import fr.eric.mobilhomy.dal.MobilhomeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Espace administrateur + effet réel de la suspension d'un compte sur l'authentification JWT
 * (vraie connexion /api/auth/login, vrai jeton Bearer, vraie chaîne de filtres).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminControllerIntegrationTest {

    private static final String ADMIN_LOGIN = "admin@mail.fr";
    private static final String OWNER_LOGIN = "dupont@mail.fr";

    @Autowired private WebApplicationContext context;
    @Autowired private AuthRepository authRepository;
    @Autowired private MobilhomeRepository mobilhomeRepository;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private Auth account(String login) {
        return authRepository.findByLogin(login).orElseThrow();
    }

    private String loginJson(String login, String password) {
        return "{\"login\": \"%s\", \"password\": \"%s\"}".formatted(login, password);
    }

    /** Connexion réelle : renvoie le JWT délivré par /api/auth/login. */
    private String jwt(String login, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginJson(login, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    @Test
    void unProprietaire_nAccedePasALEspaceAdmin() throws Exception {
        mvc.perform(get("/api/admin/owners").with(user(account(OWNER_LOGIN))))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_listeLesProprietairesAvecLeursCompteurs() throws Exception {
        Auth dupont = account(OWNER_LOGIN);
        long mobilhomes = mobilhomeRepository.countByOwnerId(dupont.getUtilisateur().getId());

        mvc.perform(get("/api/admin/owners").with(user(account(ADMIN_LOGIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.email == 'dupont@mail.fr')].mobilhomes_count").value((int) mobilhomes))
                .andExpect(jsonPath("$[?(@.email == 'petit@mail.fr')].enabled").value(false)); // seed : compte suspendu
    }

    @Test
    void admin_consulteLesIndicateurs() throws Exception {
        mvc.perform(get("/api/admin/stats").with(user(account(ADMIN_LOGIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owners").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.owners_suspended").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.mobilhomes").value(org.hamcrest.Matchers.greaterThan(0)))
                // Les données métier des propriétaires n'ont rien à faire ici
                .andExpect(jsonPath("$.reservations_to_process").doesNotExist())
                .andExpect(jsonPath("$.invoices_pending_amount").doesNotExist());
    }

    @Test
    void admin_creeUnProprietaire_quiPeutEnsuiteSeConnecter() throws Exception {
        mvc.perform(post("/api/admin/owners").with(user(account(ADMIN_LOGIN)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstname\": \"Anne\", \"name\": \"Roy\", \"email\": \"anne.roy@mail.fr\", \"password\": \"provisoire8\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.enabled").value(true));

        jwt("anne.roy@mail.fr", "provisoire8");
    }

    @Test
    void suspension_couperImmediatementLesJetonsEtLaConnexion_puisReactivation() throws Exception {
        String token = jwt(OWNER_LOGIN, "owner");
        Integer dupontId = account(OWNER_LOGIN).getUtilisateur().getId();

        // Avant : le jeton fonctionne
        mvc.perform(get("/api/owner/mobilhomes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mvc.perform(put("/api/admin/owners/{id}/suspend", dupontId).with(user(account(ADMIN_LOGIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        // Après : le même jeton, pourtant non expiré, est refusé…
        mvc.perform(get("/api/owner/mobilhomes").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        // …et la connexion aussi, avec un message explicite
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginJson(OWNER_LOGIN, "owner")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("suspendu")));

        mvc.perform(put("/api/admin/owners/{id}/reactivate", dupontId).with(user(account(ADMIN_LOGIN))))
                .andExpect(status().isOk());
        jwt(OWNER_LOGIN, "owner");
    }

    @Test
    void compteSuspendu_mauvaisMotDePasse_neRevelePasLaSuspension() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginJson("petit@mail.fr", "faux")))
                .andExpect(status().isForbidden())
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertFalse(
                        result.getResponse().getContentAsString().contains("suspendu")));
    }
}
