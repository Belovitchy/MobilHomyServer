package fr.eric.mobilhomy.controller;

import fr.eric.mobilhomy.bo.*;
import fr.eric.mobilhomy.dal.AuthRepository;
import fr.eric.mobilhomy.dal.LinkRepository;
import fr.eric.mobilhomy.dal.MobilhomeRepository;
import fr.eric.mobilhomy.dal.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'intégration de l'API propriétaire : contexte Spring complet, base H2 (profil "test"),
 * chaîne de sécurité réelle et jeu de données du DataSeeder.
 * Chaque test est annulé en fin d'exécution (@Transactional) : ils restent indépendants.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OwnerControllerIntegrationTest {

    private static final String OWNER_LOGIN = "dupont@mail.fr";      // seed : propriétaire de « Océane » et « Azur »
    private static final String VACATIONER_LOGIN = "julie@mail.fr";  // seed : vacancière

    @Autowired private WebApplicationContext context;
    @Autowired private AuthRepository authRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private MobilhomeRepository mobilhomeRepository;
    @Autowired private LinkRepository linkRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private Auth account(String login) {
        return authRepository.findByLogin(login).orElseThrow();
    }

    /** Demande soumise (à traiter) d'un mobil-home donné de Dupont — voir les scénarios du DataSeeder. */
    private Reservation submittedReservation(String mobilhomeName) {
        return reservationRepository.findAll().stream()
                .filter(r -> r.getStatus() == ReservationStatus.COMPLETED)
                .filter(r -> mobilhomeName.equals(r.getMobilhome().getName()))
                .findFirst().orElseThrow();
    }

    /** Seed : la demande d'Inès sur « Azur » ne chevauche aucun séjour validé. */
    private Reservation submittedReservation() {
        return submittedReservation("Azur");
    }

    // ----- Sécurité : qui a le droit d'appeler /api/owner/** ? -----

    @Test
    void sansAuthentification_accesRefuse() throws Exception {
        mvc.perform(get("/api/owner/mobilhomes"))
                .andExpect(result -> {
                    int code = result.getResponse().getStatus();
                    assertTrue(code == 401 || code == 403, "Attendu 401 ou 403, reçu " + code);
                });
    }

    @Test
    void vacancier_accesInterdit() throws Exception {
        mvc.perform(get("/api/owner/reservations").with(user(account(VACATIONER_LOGIN))))
                .andExpect(status().isForbidden());
    }

    // ----- Parcours nominal du propriétaire -----

    @Test
    void proprietaire_listeSesMobilhomes() throws Exception {
        Auth owner = account(OWNER_LOGIN);
        int expected = mobilhomeRepository.findAllByOwnerIdWithImages(owner.getUtilisateur().getId()).size();

        mvc.perform(get("/api/owner/mobilhomes").with(user(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(expected))
                .andExpect(jsonPath("$[0].title").exists())
                .andExpect(jsonPath("$[0].postal_code").exists());
    }

    @Test
    void proprietaire_valideUneDemandeSoumise() throws Exception {
        Reservation r = submittedReservation();

        mvc.perform(put("/api/owner/reservations/{id}/validate", r.getId()).with(user(account(OWNER_LOGIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALIDATED"))
                .andExpect(jsonPath("$.mobil_home_id").value(r.getMobilhome().getId()));
    }

    @Test
    void proprietaire_nePeutPasValiderDeuxFois() throws Exception {
        Reservation r = submittedReservation();
        Auth owner = account(OWNER_LOGIN);

        mvc.perform(put("/api/owner/reservations/{id}/validate", r.getId()).with(user(owner)))
                .andExpect(status().isOk());
        mvc.perform(put("/api/owner/reservations/{id}/validate", r.getId()).with(user(owner)))
                .andExpect(status().isConflict());
    }

    @Test
    void proprietaire_nePeutPasValiderUneDemandeQuiChevaucheUnSejourValide() throws Exception {
        // Seed : la demande de Lucas sur « Océane » chevauche le séjour validé de Thomas.
        Reservation conflit = submittedReservation("Océane");

        mvc.perform(put("/api/owner/reservations/{id}/validate", conflit.getId()).with(user(account(OWNER_LOGIN))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    // ----- Déclaration camping : plaque visible, numéro Siblu saisi après validation -----

    /** Séjour validé de Dupont encore sans numéro Siblu (seed : Thomas sur Océane, à venir). */
    private Reservation validatedWithoutSiblu() {
        return reservationRepository.findAll().stream()
                .filter(r -> r.getStatus() == ReservationStatus.VALIDATED && r.getSibluRef() == null)
                .filter(r -> r.getVacationer() != null) // réservation MobilHomy (le vacancier doit voir le n°)
                .filter(r -> r.getMobilhome().getOwner().getAuth() != null
                        && OWNER_LOGIN.equals(r.getMobilhome().getOwner().getAuth().getLogin()))
                .findFirst().orElseThrow();
    }

    @Test
    void proprietaire_voitLaPlaquePourLaDeclarationCamping() throws Exception {
        mvc.perform(get("/api/owner/reservations").with(user(account(OWNER_LOGIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].immat").exists())
                .andExpect(jsonPath("$[0].siblu_ref").hasJsonPath());
    }

    @Test
    void proprietaire_renseigneLeNumeroSiblu_quiDevientVisiblePourLeVacancier() throws Exception {
        Reservation r = validatedWithoutSiblu();
        Auth vacationer = r.getVacationer().getAuth();

        mvc.perform(put("/api/owner/reservations/{id}/siblu-ref", r.getId())
                        .with(user(account(OWNER_LOGIN)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"siblu_ref\": \"sblu-res-99999\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.siblu_ref").value("SBLU-RES-99999"));

        mvc.perform(get("/api/vacationer/reservations/mine").with(user(vacationer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + r.getId() + ")].siblu_ref").value("SBLU-RES-99999"));
    }

    @Test
    void numeroSiblu_surUneDemandeNonValidee_renvoie409() throws Exception {
        mvc.perform(put("/api/owner/reservations/{id}/siblu-ref", submittedReservation().getId())
                        .with(user(account(OWNER_LOGIN)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"siblu_ref\": \"SBLU-1\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void numeroSiblu_invalide_renvoie400() throws Exception {
        mvc.perform(put("/api/owner/reservations/{id}/siblu-ref", validatedWithoutSiblu().getId())
                        .with(user(account(OWNER_LOGIN)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"siblu_ref\": \"<script>\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void vacancier_neVoitPasLeNumeroSibluTantQueLaReservationNEstPasValidee() throws Exception {
        Reservation demande = submittedReservation();
        demande.setSibluRef("SBLU-PREMATURE"); // cas anormal simulé directement en base
        reservationRepository.saveAndFlush(demande);

        mvc.perform(get("/api/vacationer/reservations/mine").with(user(demande.getVacationer().getAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + demande.getId() + ")].siblu_ref").value(org.hamcrest.Matchers.contains((Object) null)));
    }

    // ----- Réservations externes (Booking, Airbnb…) saisies par le propriétaire -----

    private Integer mobilhomeId(String name) {
        return mobilhomeRepository.findAll().stream().filter(m -> name.equals(m.getName())).findFirst().orElseThrow().getId();
    }

    @Test
    void proprietaire_saisitUneReservationBooking_confirmeeDOffice() throws Exception {
        String body = """
                {"mobil_home_id": %d, "source": "BOOKING", "guest_name": "Marie Blanc", "guest_phone": "0612121212",
                 "start_date": "2030-07-01", "end_date": "2030-07-08", "immat": "mb-123-bk",
                 "occupants": [{"firstname": "Marie", "name": "Blanc", "age": 40}, {"firstname": "Tom", "name": "Blanc", "age": 8}]}
                """.formatted(mobilhomeId("Azur"));

        mvc.perform(post("/api/owner/reservations").with(user(account(OWNER_LOGIN)))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("VALIDATED"))
                .andExpect(jsonPath("$.source").value("BOOKING"))
                .andExpect(jsonPath("$.occupant_name").value("Marie Blanc"))
                .andExpect(jsonPath("$.immat").value("MB-123-BK"))
                .andExpect(jsonPath("$.occupants_count").value(2));
    }

    @Test
    void reservationExterne_surUnePeriodeDejaReservee_renvoie409() throws Exception {
        Reservation validee = reservationRepository.findAll().stream()
                .filter(r -> r.getStatus() == ReservationStatus.VALIDATED && "Azur".equals(r.getMobilhome().getName()))
                .findFirst().orElseThrow();
        String body = """
                {"mobil_home_id": %d, "source": "AIRBNB", "guest_name": "Doublon", "start_date": "%s", "end_date": "%s", "occupants": []}
                """.formatted(validee.getMobilhome().getId(), validee.getStartDate(), validee.getEndDate());

        mvc.perform(post("/api/owner/reservations").with(user(account(OWNER_LOGIN)))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void proprietaire_completeLesOccupantsEtLaPlaqueDUneReservationExterne() throws Exception {
        // Seed : réservation Booking de Kevin Laurent sur Océane, sans plaque ni occupants.
        Reservation kevin = reservationRepository.findAll().stream()
                .filter(r -> "Kevin Laurent".equals(r.getGuestName())).findFirst().orElseThrow();
        String body = """
                {"guest_name": "Kevin Laurent", "immat": "kl-555-bk",
                 "occupants": [{"firstname": "Kevin", "name": "Laurent", "age": 33}, {"firstname": "Léa", "name": "Laurent", "age": 31}]}
                """;

        mvc.perform(put("/api/owner/reservations/{id}/details", kevin.getId()).with(user(account(OWNER_LOGIN)))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.immat").value("KL-555-BK"))
                .andExpect(jsonPath("$.occupants.length()").value(2));
    }

    @Test
    void proprietaire_nePeutPasModifierLesOccupantsDUneReservationMobilhomy() throws Exception {
        String body = "{\"guest_name\": \"X\", \"immat\": \"AA-123-BB\", \"occupants\": []}";

        mvc.perform(put("/api/owner/reservations/{id}/details", validatedWithoutSiblu().getId())
                        .with(user(account(OWNER_LOGIN)))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    // ----- Liens utiles -----

    @Test
    void proprietaire_gereSesLiens() throws Exception {
        Auth owner = account(OWNER_LOGIN);

        mvc.perform(get("/api/owner/links").with(user(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", org.hamcrest.Matchers.hasItem("Déclaration Siblu")));

        mvc.perform(post("/api/owner/links").with(user(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Abritel\", \"url\": \"https://www.abritel.fr/\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void lien_javascript_refuse() throws Exception {
        mvc.perform(post("/api/owner/links").with(user(account(OWNER_LOGIN)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Piège\", \"url\": \"javascript:alert(1)\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creationMobilhome_invalide_renvoie400() throws Exception {
        String invalide = "{\"title\":\"\",\"address\":\"\",\"postal_code\":\"ABC\",\"capacity\":0,\"price\":-5}";

        mvc.perform(post("/api/owner/mobilhomes")
                        .with(user(account(OWNER_LOGIN)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalide))
                .andExpect(status().isBadRequest());
    }

    // ----- Mon compte -----

    @Test
    void proprietaire_consulteSonProfil() throws Exception {
        mvc.perform(get("/api/owner/account").with(user(account(OWNER_LOGIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(OWNER_LOGIN))
                .andExpect(jsonPath("$.firstname").value("Paul"));
    }

    @Test
    void vacancier_neLitPasLeProfilProprietaire() throws Exception {
        mvc.perform(get("/api/owner/account").with(user(account(VACATIONER_LOGIN))))
                .andExpect(status().isForbidden());
    }

    // ----- Upload d'images -----

    @Test
    void uploadImage_parUnVacancier_interdit() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", new byte[]{1, 2, 3});

        mvc.perform(multipart("/api/owner/images").file(file).with(user(account(VACATIONER_LOGIN))))
                .andExpect(status().isForbidden());
    }

    @Test
    void uploadImage_fichierQuiNEstPasUneImage_renvoie400() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png",
                "pas une image, juste du texte".getBytes());

        mvc.perform(multipart("/api/owner/images").file(file).with(user(account(OWNER_LOGIN))))
                .andExpect(status().isBadRequest());
    }

    // ----- Contrôle de propriété (anti-IDOR) -----

    @Test
    void autreProprietaire_neVoitNiNeValideLesReservationsDuPremier() throws Exception {
        Auth other = authRepository.save(Auth.builder()
                .utilisateur(Owner.builder().firstname("Anne").name("Roy").phone("0600000009").build())
                .login("roy@mail.fr")
                .password(passwordEncoder.encode("owner-test-123"))
                .authority(RolesEnum.OWNER)
                .build());
        Reservation r = submittedReservation();

        mvc.perform(get("/api/owner/reservations").with(user(other)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mvc.perform(put("/api/owner/reservations/{id}/validate", r.getId()).with(user(other)))
                .andExpect(status().isNotFound());

        mvc.perform(delete("/api/owner/mobilhomes/{id}", r.getMobilhome().getId()).with(user(other)))
                .andExpect(status().isNotFound());

        Integer dupontLinkId = linkRepository.findByOwnerIdOrderByNameAsc(
                account(OWNER_LOGIN).getUtilisateur().getId()).getFirst().getId();
        mvc.perform(delete("/api/owner/links/{id}", dupontLinkId).with(user(other)))
                .andExpect(status().isNotFound());
    }
}
