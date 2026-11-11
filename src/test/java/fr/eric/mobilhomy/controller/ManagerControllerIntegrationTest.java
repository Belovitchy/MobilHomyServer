package fr.eric.mobilhomy.controller;

import fr.eric.mobilhomy.bo.*;
import fr.eric.mobilhomy.dal.AuthRepository;
import fr.eric.mobilhomy.dal.InvoiceRepository;
import fr.eric.mobilhomy.dal.MobilhomeRepository;
import fr.eric.mobilhomy.dal.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'intégration de l'API gérant (H2 + DataSeeder) :
 * marc@mail.fr gère « Océane » et « Azur », qui ont des séjours de tous les statuts et plusieurs factures.
 * Les attentes sont calculées depuis la base : enrichir le seed ne casse pas ces tests.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ManagerControllerIntegrationTest {

    private static final String MANAGER_LOGIN = "marc@mail.fr";
    private static final String OWNER_LOGIN = "dupont@mail.fr";

    @Autowired private WebApplicationContext context;
    @Autowired private AuthRepository authRepository;
    @Autowired private MobilhomeRepository mobilhomeRepository;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private Auth account(String login) {
        return authRepository.findByLogin(login).orElseThrow();
    }

    private Auth otherManager() {
        return authRepository.save(Auth.builder()
                .utilisateur(Manager.builder().firstname("Léa").name("Petit").phone("0600000010").build())
                .login("lea@mail.fr").password(passwordEncoder.encode("manager-test-123"))
                .authority(RolesEnum.MANAGER).build());
    }

    private Integer oceaneId() {
        return mobilhomeRepository.findAll().stream()
                .filter(m -> "Océane".equals(m.getName())).findFirst().orElseThrow().getId();
    }

    private String invoiceJson(Integer mobilhomeId, String amount, LocalDate date) {
        return """
                {"mobil_home_id": %d, "category": "SERVICE", "description": "Ménage fin de séjour",
                 "amount": %s, "date": "%s"}""".formatted(mobilhomeId, amount, date);
    }

    // ----- Sécurité -----

    @Test
    void proprietaire_nAccedePasALEspaceGerant() throws Exception {
        mvc.perform(get("/api/manager/reservations").with(user(account(OWNER_LOGIN))))
                .andExpect(status().isForbidden());
    }

    // ----- Réservations -----

    @Test
    void gerant_voitUniquementLesSejoursValides_avecLesInfosDAccueil() throws Exception {
        Auth marc = account(MANAGER_LOGIN);
        Integer marcId = marc.getUtilisateur().getId();
        int validated = reservationRepository.findAllByManagerIdAndStatus(marcId, ReservationStatus.VALIDATED).size();
        long others = reservationRepository.findAll().stream()
                .filter(r -> r.getMobilhome().getManager() != null && marcId.equals(r.getMobilhome().getManager().getId()))
                .filter(r -> r.getStatus() != ReservationStatus.VALIDATED)
                .count();
        org.junit.jupiter.api.Assertions.assertTrue(others > 0, "Le seed doit contenir des demandes non validées");

        mvc.perform(get("/api/manager/reservations").with(user(marc)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(validated)) // aucune demande COMPLETED, PENDING, REJECTED…
                .andExpect(jsonPath("$[*].status", everyItem(is("VALIDATED"))))
                .andExpect(jsonPath("$[*].vacationer_email", hasItem("julie@mail.fr")))
                .andExpect(jsonPath("$[*].immat", hasItem("AA-123-BB")))
                .andExpect(jsonPath("$[0].occupants").isArray());
    }

    @Test
    void unAutreGerant_neVoitAucuneReservation() throws Exception {
        mvc.perform(get("/api/manager/reservations").with(user(otherManager())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ----- Factures -----

    @Test
    void gerant_saisitUneFacture_puisLaRetrouveDansSaListe() throws Exception {
        Auth marc = account(MANAGER_LOGIN);
        int before = invoiceRepository.findAllByManagerId(marc.getUtilisateur().getId()).size();

        mvc.perform(post("/api/manager/invoices").with(user(marc))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invoiceJson(oceaneId(), "65.00", LocalDate.now())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ISSUED"))
                .andExpect(jsonPath("$.mobil_home_title").value("Océane"));

        mvc.perform(get("/api/manager/invoices").with(user(marc)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(before + 1));
    }

    @Test
    void facture_montantNegatifOuDateFuture_renvoie400() throws Exception {
        Auth marc = account(MANAGER_LOGIN);

        mvc.perform(post("/api/manager/invoices").with(user(marc))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invoiceJson(oceaneId(), "-5", LocalDate.now())))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/manager/invoices").with(user(marc))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invoiceJson(oceaneId(), "65.00", LocalDate.now().plusDays(1))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void facture_pourUnMobilhomeNonGere_renvoie404() throws Exception {
        Owner dupont = (Owner) account(OWNER_LOGIN).getUtilisateur();
        Mobilhome sansGerant = mobilhomeRepository.save(Mobilhome.builder()
                .owner(dupont).name("Évasion").capacity(8).postalCode("17390").price(BigDecimal.valueOf(110)).build());

        mvc.perform(post("/api/manager/invoices").with(user(account(MANAGER_LOGIN)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invoiceJson(sansGerant.getId(), "65.00", LocalDate.now())))
                .andExpect(status().isNotFound());
    }

    @Test
    void unAutreGerant_nePeutPasAnnulerLaFactureDeMarc() throws Exception {
        Integer marcId = account(MANAGER_LOGIN).getUtilisateur().getId();
        Integer marcInvoiceId = invoiceRepository.findAllByManagerId(marcId).stream()
                .filter(i -> i.getStatus() == InvoiceStatus.ISSUED).findFirst().orElseThrow().getId();

        mvc.perform(put("/api/manager/invoices/{id}/cancel", marcInvoiceId).with(user(otherManager())))
                .andExpect(status().isNotFound());

        mvc.perform(put("/api/manager/invoices/{id}/cancel", marcInvoiceId).with(user(account(MANAGER_LOGIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    // ----- Mon compte -----

    @Test
    void gerant_consulteSonProfil() throws Exception {
        mvc.perform(get("/api/manager/account").with(user(account(MANAGER_LOGIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(MANAGER_LOGIN))
                .andExpect(jsonPath("$.phone").value("0600000002"));
    }
}
