package fr.eric.mobilhomy.bll;

import fr.eric.mobilhomy.bo.*;
import fr.eric.mobilhomy.dal.*;
import fr.eric.mobilhomy.dto.*;
import fr.eric.mobilhomy.exception.BusinessRuleException;
import fr.eric.mobilhomy.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Règles métier du gérant : visibilité des réservations, factures, contrôle de propriété. */
@ExtendWith(MockitoExtension.class)
class ManagerServiceTest {

    private static final int MANAGER_ID = 3;
    private static final int OTHER_MANAGER_ID = 4;

    @Mock private ManagerRepository managerRepository;
    @Mock private MobilhomeRepository mobilhomeRepository;
    @Mock private ReservationRepository reservationRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private AuthRepository authRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ManagerServiceImpl managerService;

    private Owner owner;
    private Manager manager;
    private Mobilhome oceane;

    @BeforeEach
    void setUp() {
        owner = Owner.builder().id(1).firstname("Paul").name("Dupont").build();
        manager = Manager.builder().id(MANAGER_ID).firstname("Marc").name("Durand").phone("0600000002").build();
        oceane = Mobilhome.builder().id(10).owner(owner).manager(manager).name("Océane").location("La Réserve").build();
    }

    private InvoiceRequestDto invoiceRequest(Integer mobilhomeId) {
        return new InvoiceRequestDto(mobilhomeId, InvoiceCategory.SERVICE, "  Ménage fin de séjour  ",
                new BigDecimal("65.00"), LocalDate.now());
    }

    // ----------------------------------------------------------------
    // Réservations
    // ----------------------------------------------------------------

    @Test
    void getReservations_neDemandeQueLesReservationsValidees_avecLesCoordonneesDuVacancier() {
        Vacationer julie = Vacationer.builder().id(7).firstname("Julie").name("Martin").phone("0600000003").build();
        julie.setAuth(Auth.builder().login("julie@mail.fr").build());
        Reservation r = Reservation.builder().id(50).mobilhome(oceane).vacationer(julie)
                .startDate(LocalDate.of(2027, 7, 12)).endDate(LocalDate.of(2027, 7, 19))
                .immat("AA-123-BB").status(ReservationStatus.VALIDATED).build();
        when(reservationRepository.findAllByManagerIdAndStatus(MANAGER_ID, ReservationStatus.VALIDATED))
                .thenReturn(List.of(r));

        List<ManagerReservationResponseDto> res = managerService.getReservations(MANAGER_ID);

        assertEquals(1, res.size());
        assertEquals("0600000003", res.getFirst().vacationerPhone());
        assertEquals("julie@mail.fr", res.getFirst().vacationerEmail());
        assertEquals("AA-123-BB", res.getFirst().immat());
        // Jamais d'appel pour un autre statut : pas de coordonnées pour une demande non confirmée.
        verify(reservationRepository).findAllByManagerIdAndStatus(MANAGER_ID, ReservationStatus.VALIDATED);
        verifyNoMoreInteractions(reservationRepository);
    }

    // ----------------------------------------------------------------
    // Factures
    // ----------------------------------------------------------------

    @Test
    void createInvoice_rattacheAuMobilhomeEtAuProprietaire_statutEmise() {
        when(mobilhomeRepository.findByIdAndManagerId(10, MANAGER_ID)).thenReturn(Optional.of(oceane));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> {
            Invoice inv = i.getArgument(0);
            inv.setId(100);
            return inv;
        });

        InvoiceResponseDto res = managerService.createInvoice(MANAGER_ID, invoiceRequest(10));

        ArgumentCaptor<Invoice> saved = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(saved.capture());
        Invoice inv = saved.getValue();
        assertSame(owner, inv.getOwner());        // c'est le propriétaire qui paie
        assertSame(manager, inv.getManager());
        assertEquals(InvoiceType.OUTGOING, inv.getType());
        assertEquals(InvoiceStatus.ISSUED, inv.getStatus());
        assertEquals("Ménage fin de séjour", inv.getDescription());
        assertEquals(100, res.id());
        assertEquals("Océane", res.mobilhomeTitle());
    }

    @Test
    void createInvoice_pourUnMobilhomeQuIlNeGerePas_renvoieNotFound() {
        when(mobilhomeRepository.findByIdAndManagerId(99, MANAGER_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> managerService.createInvoice(MANAGER_ID, invoiceRequest(99)));
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void cancelInvoice_emise_passeEnAnnulee() {
        Invoice inv = Invoice.builder().id(100).mobilhome(oceane).status(InvoiceStatus.ISSUED).build();
        when(invoiceRepository.findByIdAndManagerId(100, MANAGER_ID)).thenReturn(Optional.of(inv));
        when(invoiceRepository.save(inv)).thenReturn(inv);

        InvoiceResponseDto res = managerService.cancelInvoice(MANAGER_ID, 100);

        assertEquals(InvoiceStatus.CANCELLED, res.status());
    }

    @Test
    void cancelInvoice_dejaPayee_refuse() {
        Invoice inv = Invoice.builder().id(100).mobilhome(oceane).status(InvoiceStatus.PAID).build();
        when(invoiceRepository.findByIdAndManagerId(100, MANAGER_ID)).thenReturn(Optional.of(inv));

        assertThrows(BusinessRuleException.class, () -> managerService.cancelInvoice(MANAGER_ID, 100));
        assertEquals(InvoiceStatus.PAID, inv.getStatus());
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void cancelInvoice_dUnAutreGerant_renvoieNotFound() {
        when(invoiceRepository.findByIdAndManagerId(100, OTHER_MANAGER_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> managerService.cancelInvoice(OTHER_MANAGER_ID, 100));
    }

    // ----------------------------------------------------------------
    // Mon compte
    // ----------------------------------------------------------------

    @Test
    void updateAccount_sansTelephone_refuse() {
        when(managerRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));

        assertThrows(IllegalArgumentException.class,
                () -> managerService.updateAccount(MANAGER_ID, new VacationerAccountReqDto("Marc", "Durand", " ")));
        verify(managerRepository, never()).save(any());
    }

    @Test
    void updatePassword_motDePasseActuelFaux_refuse() {
        manager.setAuth(Auth.builder().login("marc@mail.fr").password("{bcrypt}old").build());
        when(managerRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));
        when(passwordEncoder.matches("faux", "{bcrypt}old")).thenReturn(false);

        assertThrows(BadCredentialsException.class,
                () -> managerService.updatePassword(MANAGER_ID, new VacationerPassReqDto("faux", "nouveau-mdp-123")));
        verify(authRepository, never()).save(any());
    }
}
