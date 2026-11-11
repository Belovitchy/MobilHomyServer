package fr.eric.mobilhomy.bll;

import fr.eric.mobilhomy.bo.*;
import fr.eric.mobilhomy.dal.*;
import fr.eric.mobilhomy.dto.OwnerManagerRequestDto;
import fr.eric.mobilhomy.dto.OwnerMobilhomeRequestDto;
import fr.eric.mobilhomy.dto.OwnerMobilhomeResponseDto;
import fr.eric.mobilhomy.dto.OwnerReservationResponseDto;
import fr.eric.mobilhomy.dto.SibluRefRequestDto;
import fr.eric.mobilhomy.dto.ExternalReservationRequestDto;
import fr.eric.mobilhomy.dto.ReservationDetailsRequestDto;
import fr.eric.mobilhomy.dto.OwnerOccupantDto;
import fr.eric.mobilhomy.dto.LinkRequestDto;
import fr.eric.mobilhomy.dto.VacationerMailReqDto;
import fr.eric.mobilhomy.dto.VacationerPassReqDto;
import fr.eric.mobilhomy.exception.BusinessRuleException;
import fr.eric.mobilhomy.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires des règles métier du propriétaire :
 * contrôle de propriété (anti-IDOR), transitions de statut, chevauchement de dates.
 */
@ExtendWith(MockitoExtension.class)
class OwnerServiceTest {

    private static final int OWNER_ID = 1;
    private static final int OTHER_OWNER_ID = 2;

    @Mock private MobilhomeRepository mobilhomeRepository;
    @Mock private ReservationRepository reservationRepository;
    @Mock private OwnerManagerRepository ownerManagerRepository;
    @Mock private OwnerRepository ownerRepository;
    @Mock private AuthRepository authRepository;
    @Mock private ImageRepository imageRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private LinkRepository linkRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private OwnerServiceImpl ownerService;

    private Owner owner;
    private Mobilhome mobilhome;

    @BeforeEach
    void setUp() {
        owner = Owner.builder().id(OWNER_ID).firstname("Paul").name("Dupont").build();
        mobilhome = Mobilhome.builder()
                .id(10).owner(owner).name("Océane").capacity(6)
                .postalCode("17390").price(BigDecimal.valueOf(80))
                .build();
    }

    private Reservation reservation(ReservationStatus status) {
        return Reservation.builder()
                .id(50).mobilhome(mobilhome)
                .vacationer(Vacationer.builder().id(3).firstname("Julie").name("Martin").build())
                .startDate(LocalDate.of(2027, 7, 12))
                .endDate(LocalDate.of(2027, 7, 19))
                .status(status)
                .build();
    }

    // ----------------------------------------------------------------
    // Validation / refus des réservations
    // ----------------------------------------------------------------

    @Test
    void validateReservation_passeLaDemandeEnValidee() {
        Reservation r = reservation(ReservationStatus.COMPLETED);
        when(reservationRepository.findByIdAndMobilhomeOwnerId(50, OWNER_ID)).thenReturn(Optional.of(r));
        when(reservationRepository.existsOverlap(10, 50, ReservationStatus.VALIDATED, r.getStartDate(), r.getEndDate()))
                .thenReturn(false);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));

        OwnerReservationResponseDto res = ownerService.validateReservation(OWNER_ID, 50);

        assertEquals(ReservationStatus.VALIDATED, res.status());
        assertEquals(new BigDecimal("560"), res.totalPrice()); // 7 nuits × 80 €
        verify(reservationRepository).save(r);
    }

    @Test
    void validateReservation_dUnAutreProprietaire_renvoieNotFound() {
        // Le repository filtre sur l'owner : pour un autre propriétaire, la réservation « n'existe pas ».
        when(reservationRepository.findByIdAndMobilhomeOwnerId(50, OTHER_OWNER_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> ownerService.validateReservation(OTHER_OWNER_ID, 50));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void validateReservation_brouillon_refuseLaTransition() {
        Reservation r = reservation(ReservationStatus.PENDING);
        when(reservationRepository.findByIdAndMobilhomeOwnerId(50, OWNER_ID)).thenReturn(Optional.of(r));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> ownerService.validateReservation(OWNER_ID, 50));
        assertTrue(ex.getMessage().contains("PENDING"));
        assertEquals(ReservationStatus.PENDING, r.getStatus());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void validateReservation_dejaValidee_refuseLaTransition() {
        Reservation r = reservation(ReservationStatus.VALIDATED);
        when(reservationRepository.findByIdAndMobilhomeOwnerId(50, OWNER_ID)).thenReturn(Optional.of(r));

        assertThrows(BusinessRuleException.class, () -> ownerService.validateReservation(OWNER_ID, 50));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void validateReservation_chevauchement_refuse() {
        Reservation r = reservation(ReservationStatus.COMPLETED);
        when(reservationRepository.findByIdAndMobilhomeOwnerId(50, OWNER_ID)).thenReturn(Optional.of(r));
        when(reservationRepository.existsOverlap(10, 50, ReservationStatus.VALIDATED, r.getStartDate(), r.getEndDate()))
                .thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> ownerService.validateReservation(OWNER_ID, 50));
        assertEquals(ReservationStatus.COMPLETED, r.getStatus());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void rejectReservation_passeLaDemandeEnRefusee() {
        Reservation r = reservation(ReservationStatus.COMPLETED);
        when(reservationRepository.findByIdAndMobilhomeOwnerId(50, OWNER_ID)).thenReturn(Optional.of(r));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));

        OwnerReservationResponseDto res = ownerService.rejectReservation(OWNER_ID, 50);

        assertEquals(ReservationStatus.REJECTED, res.status());
    }

    // ----------------------------------------------------------------
    // Numéro Siblu (déclaration au camping)
    // ----------------------------------------------------------------

    @Test
    void updateSibluRef_reservationValidee_enregistreLeNumeroNormalise() {
        Reservation r = reservation(ReservationStatus.VALIDATED);
        r.setImmat("AA-123-BB");
        when(reservationRepository.findByIdAndMobilhomeOwnerId(50, OWNER_ID)).thenReturn(Optional.of(r));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));

        OwnerReservationResponseDto res = ownerService.updateSibluRef(OWNER_ID, 50, new SibluRefRequestDto("  sblu-res-10421 "));

        assertEquals("SBLU-RES-10421", res.sibluRef());
        assertEquals("AA-123-BB", res.immat()); // la plaque est visible : elle sert à la déclaration
    }

    @Test
    void updateSibluRef_demandeNonValidee_refuse() {
        Reservation r = reservation(ReservationStatus.COMPLETED);
        when(reservationRepository.findByIdAndMobilhomeOwnerId(50, OWNER_ID)).thenReturn(Optional.of(r));

        assertThrows(BusinessRuleException.class,
                () -> ownerService.updateSibluRef(OWNER_ID, 50, new SibluRefRequestDto("SBLU-1")));
        assertNull(r.getSibluRef());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void updateSibluRef_dUnAutreProprietaire_renvoieNotFound() {
        when(reservationRepository.findByIdAndMobilhomeOwnerId(50, OTHER_OWNER_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> ownerService.updateSibluRef(OTHER_OWNER_ID, 50, new SibluRefRequestDto("SBLU-1")));
    }

    // ----------------------------------------------------------------
    // Réservations externes (Booking, Airbnb…)
    // ----------------------------------------------------------------

    private ExternalReservationRequestDto external(ReservationSource source, int occupants) {
        List<OwnerOccupantDto> list = java.util.stream.IntStream.range(0, occupants)
                .mapToObj(i -> new OwnerOccupantDto(" Prénom" + i, "Roux ", 30)).toList();
        return new ExternalReservationRequestDto(10, source, " Nathalie Roux ", "", LocalDate.of(2027, 7, 1),
                LocalDate.of(2027, 7, 8), "nr-318-bk", list);
    }

    @Test
    void createExternalReservation_confirmeeDOffice_avecOccupantsEtPlaqueNormalisee() {
        when(mobilhomeRepository.findByIdAndOwnerId(10, OWNER_ID)).thenReturn(Optional.of(mobilhome));
        when(reservationRepository.existsOverlap(eq(10), eq(-1), eq(ReservationStatus.VALIDATED), any(), any())).thenReturn(false);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));

        OwnerReservationResponseDto res = ownerService.createExternalReservation(OWNER_ID, external(ReservationSource.BOOKING, 3));

        assertEquals(ReservationStatus.VALIDATED, res.status());
        assertEquals(ReservationSource.BOOKING, res.source());
        assertEquals("Nathalie Roux", res.occupantName());
        assertEquals("NR-318-BK", res.immat());
        assertEquals(3, res.occupantsCount());
        assertEquals("Prénom0", res.occupants().getFirst().firstname());
        assertNull(res.guestPhone()); // chaîne vide → null
    }

    @Test
    void createExternalReservation_sourceMobilhomy_refusee() {
        assertThrows(IllegalArgumentException.class,
                () -> ownerService.createExternalReservation(OWNER_ID, external(ReservationSource.MOBILHOMY, 1)));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createExternalReservation_chevauchement_refuse() {
        when(mobilhomeRepository.findByIdAndOwnerId(10, OWNER_ID)).thenReturn(Optional.of(mobilhome));
        when(reservationRepository.existsOverlap(eq(10), eq(-1), eq(ReservationStatus.VALIDATED), any(), any())).thenReturn(true);

        assertThrows(BusinessRuleException.class,
                () -> ownerService.createExternalReservation(OWNER_ID, external(ReservationSource.BOOKING, 2)));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createExternalReservation_plusDOccupantsQueDeCouchages_refuse() {
        when(mobilhomeRepository.findByIdAndOwnerId(10, OWNER_ID)).thenReturn(Optional.of(mobilhome)); // capacité 6

        assertThrows(BusinessRuleException.class,
                () -> ownerService.createExternalReservation(OWNER_ID, external(ReservationSource.AIRBNB, 7)));
    }

    @Test
    void updateReservationDetails_reservationMobilhomy_refuse() {
        Reservation r = reservation(ReservationStatus.VALIDATED); // source MOBILHOMY par défaut
        when(reservationRepository.findByIdAndMobilhomeOwnerId(50, OWNER_ID)).thenReturn(Optional.of(r));

        assertThrows(BusinessRuleException.class, () -> ownerService.updateReservationDetails(OWNER_ID, 50,
                new ReservationDetailsRequestDto("X", null, "AA-123-BB", List.of())));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void updateReservationDetails_reservationExterne_remplaceLesOccupants() {
        Reservation r = reservation(ReservationStatus.VALIDATED);
        r.setSource(ReservationSource.BOOKING);
        r.setVacationer(null);
        r.getOccupants().add(Occupant.builder().firstname("Ancien").name("Occupant").age(50).reservation(r).build());
        when(reservationRepository.findByIdAndMobilhomeOwnerId(50, OWNER_ID)).thenReturn(Optional.of(r));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));

        OwnerReservationResponseDto res = ownerService.updateReservationDetails(OWNER_ID, 50,
                new ReservationDetailsRequestDto("Kevin Laurent", "0600000099", "kl-555-bk",
                        List.of(new OwnerOccupantDto("Kevin", "Laurent", 33), new OwnerOccupantDto("Léa", "Laurent", 31))));

        assertEquals(2, res.occupantsCount());
        assertEquals("KL-555-BK", res.immat());
        assertEquals("Kevin Laurent", res.occupantName());
        assertEquals("0600000099", res.guestPhone());
        assertTrue(r.getOccupants().stream().noneMatch(o -> "Ancien".equals(o.getFirstname())));
    }

    // ----------------------------------------------------------------
    // Factures
    // ----------------------------------------------------------------

    @Test
    void payInvoice_enAttente_passeEnPayee() {
        Invoice inv = Invoice.builder().id(100).owner(owner).mobilhome(mobilhome).status(InvoiceStatus.ISSUED).build();
        when(invoiceRepository.findByIdAndOwnerId(100, OWNER_ID)).thenReturn(Optional.of(inv));
        when(invoiceRepository.save(inv)).thenReturn(inv);

        assertEquals(InvoiceStatus.PAID, ownerService.payInvoice(OWNER_ID, 100).status());
    }

    @Test
    void payInvoice_annulee_refuse() {
        Invoice inv = Invoice.builder().id(100).owner(owner).mobilhome(mobilhome).status(InvoiceStatus.CANCELLED).build();
        when(invoiceRepository.findByIdAndOwnerId(100, OWNER_ID)).thenReturn(Optional.of(inv));

        assertThrows(BusinessRuleException.class, () -> ownerService.payInvoice(OWNER_ID, 100));
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void payInvoice_dUnAutreProprietaire_renvoieNotFound() {
        when(invoiceRepository.findByIdAndOwnerId(100, OTHER_OWNER_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> ownerService.payInvoice(OTHER_OWNER_ID, 100));
    }

    // ----------------------------------------------------------------
    // Liens utiles
    // ----------------------------------------------------------------

    @Test
    void updateLink_dUnAutreProprietaire_renvoieNotFound() {
        when(linkRepository.findByIdAndOwnerId(9, OTHER_OWNER_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> ownerService.updateLink(OTHER_OWNER_ID, 9, new LinkRequestDto("Booking", "https://admin.booking.com/")));
        verify(linkRepository, never()).save(any());
    }

    @Test
    void createLink_rattacheLeLienAuProprietaire() {
        when(ownerRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(linkRepository.save(any(Link.class))).thenAnswer(i -> i.getArgument(0));

        ownerService.createLink(OWNER_ID, new LinkRequestDto(" Déclaration Siblu ", "https://www.siblu.fr/ "));

        verify(linkRepository).save(argThat(l -> l.getOwner() == owner
                && "Déclaration Siblu".equals(l.getName()) && "https://www.siblu.fr/".equals(l.getUrl())));
    }

    // ----------------------------------------------------------------
    // Mobil-homes
    // ----------------------------------------------------------------

    @Test
    void deleteMobilhome_avecReservationsActives_refuse() {
        when(mobilhomeRepository.findByIdAndOwnerId(10, OWNER_ID)).thenReturn(Optional.of(mobilhome));
        when(reservationRepository.existsByMobilhomeIdAndStatusIn(eq(10), anyCollection())).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> ownerService.deleteMobilhome(OWNER_ID, 10));
        verify(mobilhomeRepository, never()).delete(any());
    }

    @Test
    void deleteMobilhome_avecFactures_refuse() {
        when(mobilhomeRepository.findByIdAndOwnerId(10, OWNER_ID)).thenReturn(Optional.of(mobilhome));
        when(reservationRepository.existsByMobilhomeIdAndStatusIn(eq(10), anyCollection())).thenReturn(false);
        when(invoiceRepository.existsByMobilhomeId(10)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> ownerService.deleteMobilhome(OWNER_ID, 10));
        verify(mobilhomeRepository, never()).delete(any());
    }

    @Test
    void deleteMobilhome_sansReservationActive_supprime() {
        when(mobilhomeRepository.findByIdAndOwnerId(10, OWNER_ID)).thenReturn(Optional.of(mobilhome));
        when(reservationRepository.existsByMobilhomeIdAndStatusIn(eq(10), anyCollection())).thenReturn(false);

        ownerService.deleteMobilhome(OWNER_ID, 10);

        verify(mobilhomeRepository).delete(mobilhome);
    }

    @Test
    void updateMobilhome_dUnAutreProprietaire_renvoieNotFound() {
        when(mobilhomeRepository.findByIdAndOwnerId(10, OTHER_OWNER_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> ownerService.updateMobilhome(OTHER_OWNER_ID, 10, mobilhomeRequest(null)));
        verify(mobilhomeRepository, never()).save(any());
    }

    @Test
    void updateMobilhome_gerantQuiNEstPasLeSien_renvoieNotFound() {
        when(mobilhomeRepository.findByIdAndOwnerId(10, OWNER_ID)).thenReturn(Optional.of(mobilhome));
        when(ownerManagerRepository.findByOwnerIdAndManagerId(OWNER_ID, 99)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> ownerService.updateMobilhome(OWNER_ID, 10, mobilhomeRequest(99)));
        verify(mobilhomeRepository, never()).save(any());
    }

    @Test
    void createMobilhome_rattacheAuProprietaireEtCalculeLeDepartement() {
        when(ownerRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(mobilhomeRepository.save(any(Mobilhome.class))).thenAnswer(i -> {
            Mobilhome m = i.getArgument(0);
            m.setId(11);
            return m;
        });

        OwnerMobilhomeResponseDto res = ownerService.createMobilhome(OWNER_ID, mobilhomeRequest(null));

        assertEquals(11, res.id());
        assertEquals("17", res.department());
        assertEquals(List.of("/images/azur-1.webp"), res.images());
    }

    private OwnerMobilhomeRequestDto mobilhomeRequest(Integer managerId) {
        return new OwnerMobilhomeRequestDto(
                "Azur", "2 chambres", "Le Bois Soleil", "Rue des Pins", "17110", "17",
                4, BigDecimal.valueOf(100), null, managerId, List.of("/images/azur-1.webp"));
    }

    @Test
    void departmentOf_gereCorseEtOutreMer() {
        assertEquals("17", OwnerMobilhomeResponseDto.departmentOf("17390"));
        assertEquals("2A", OwnerMobilhomeResponseDto.departmentOf("20000"));
        assertEquals("2B", OwnerMobilhomeResponseDto.departmentOf("20200"));
        assertEquals("974", OwnerMobilhomeResponseDto.departmentOf("97400"));
        assertNull(OwnerMobilhomeResponseDto.departmentOf(null));
    }

    // ----------------------------------------------------------------
    // Gérants
    // ----------------------------------------------------------------

    @Test
    void createManager_emailDejaUtilise_refuse() {
        when(authRepository.existsByLogin("marc@mail.fr")).thenReturn(true);

        OwnerManagerRequestDto req = new OwnerManagerRequestDto("Marc", "Durand", "Marc@Mail.fr ", "0600000002", "motdepasse");

        assertThrows(BusinessRuleException.class, () -> ownerService.createManager(OWNER_ID, req));
        verify(authRepository, never()).save(any());
    }

    @Test
    void createManager_sansMotDePasse_refuse() {
        OwnerManagerRequestDto req = new OwnerManagerRequestDto("Marc", "Durand", "marc@mail.fr", "0600000002", null);

        assertThrows(IllegalArgumentException.class, () -> ownerService.createManager(OWNER_ID, req));
        verifyNoInteractions(authRepository);
    }

    @Test
    void createManager_creeLeCompteEtLeLienAvecLeProprietaire() {
        when(authRepository.existsByLogin("marc@mail.fr")).thenReturn(false);
        when(ownerRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(passwordEncoder.encode("motdepasse")).thenReturn("{bcrypt}hash");

        OwnerManagerRequestDto req = new OwnerManagerRequestDto("Marc", "Durand", "marc@mail.fr", "0600000002", "motdepasse");
        ownerService.createManager(OWNER_ID, req);

        verify(authRepository).save(argThat(a ->
                a.getAuthority() == RolesEnum.MANAGER
                        && "{bcrypt}hash".equals(a.getPassword())
                        && a.getUtilisateur() instanceof Manager));
        verify(ownerManagerRepository).save(argThat(link -> link.getOwner() == owner));
    }

    // ----------------------------------------------------------------
    // Mon compte
    // ----------------------------------------------------------------

    private void giveOwnerAnAccount() {
        Auth auth = Auth.builder().login("dupont@mail.fr").password("{bcrypt}old").authority(RolesEnum.OWNER)
                .utilisateur(owner).build();
        owner.setAuth(auth);
        when(ownerRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
    }

    @Test
    void updatePassword_motDePasseActuelFaux_refuse() {
        giveOwnerAnAccount();
        when(passwordEncoder.matches("mauvais", "{bcrypt}old")).thenReturn(false);

        assertThrows(org.springframework.security.authentication.BadCredentialsException.class,
                () -> ownerService.updatePassword(OWNER_ID, new VacationerPassReqDto("mauvais", "nouveau-mdp-123")));
        verify(authRepository, never()).save(any());
    }

    @Test
    void updatePassword_tropCourt_refuseSansToucherAuCompte() {
        assertThrows(IllegalArgumentException.class,
                () -> ownerService.updatePassword(OWNER_ID, new VacationerPassReqDto("actuel", "court")));
        verifyNoInteractions(passwordEncoder, authRepository);
    }

    @Test
    void updatePassword_succes_enregistreLeNouveauHash() {
        giveOwnerAnAccount();
        when(passwordEncoder.matches("actuel", "{bcrypt}old")).thenReturn(true);
        when(passwordEncoder.encode("nouveau-mdp-123")).thenReturn("{bcrypt}new");

        ownerService.updatePassword(OWNER_ID, new VacationerPassReqDto("actuel", "nouveau-mdp-123"));

        assertEquals("{bcrypt}new", owner.getAuth().getPassword());
        verify(authRepository).save(owner.getAuth());
    }

    @Test
    void updateEmail_dejaUtilise_refuse() {
        giveOwnerAnAccount();
        when(passwordEncoder.matches("actuel", "{bcrypt}old")).thenReturn(true);
        when(authRepository.existsByLogin("julie@mail.fr")).thenReturn(true);

        assertThrows(BusinessRuleException.class,
                () -> ownerService.updateEmail(OWNER_ID, new VacationerMailReqDto("Julie@mail.fr", "actuel")));
        assertEquals("dupont@mail.fr", owner.getAuth().getLogin());
    }

    @Test
    void deleteManager_dUnAutreProprietaire_renvoieNotFound() {
        when(ownerManagerRepository.findByOwnerIdAndManagerId(OTHER_OWNER_ID, 5)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> ownerService.deleteManager(OTHER_OWNER_ID, 5));
        verify(ownerManagerRepository, never()).delete(any());
    }
}
