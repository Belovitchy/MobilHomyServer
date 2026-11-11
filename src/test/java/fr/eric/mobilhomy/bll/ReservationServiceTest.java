package fr.eric.mobilhomy.bll;

import fr.eric.mobilhomy.bo.Mobilhome;
import fr.eric.mobilhomy.bo.Reservation;
import fr.eric.mobilhomy.bo.ReservationStatus;
import fr.eric.mobilhomy.bo.Vacationer;
import fr.eric.mobilhomy.dal.MobilhomeRepository;
import fr.eric.mobilhomy.dal.ReservationRepository;
import fr.eric.mobilhomy.dal.VacationerRepository;
import fr.eric.mobilhomy.dto.OccupantRequestDto;
import fr.eric.mobilhomy.dto.ReservationVacationerRequestDto;
import fr.eric.mobilhomy.dto.ReservationVacationerResponseDto;
import fr.eric.mobilhomy.dto.ReservationVacationerUpdateRequestDto;
import fr.eric.mobilhomy.exception.BusinessRuleException;
import fr.eric.mobilhomy.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private VacationerRepository vacationerRepository;

    @Mock
    private MobilhomeRepository mobilhomeRepository;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    private Vacationer vacationer;
    private Mobilhome mobilhome;

    @BeforeEach
    void setUp() {
        vacationer = new Vacationer();
        vacationer.setId(1);
        vacationer.setName("Doe");
        vacationer.setFirstname("John");

        mobilhome = new Mobilhome();
        mobilhome.setId(10);
        mobilhome.setName("Mobilhome A");
    }

    @Test
    void addReservation_ThrowsException_WhenMobilhomeIdIsNull() {
        ReservationVacationerRequestDto req = new ReservationVacationerRequestDto(
                null,
                LocalDate.now(),
                LocalDate.now().plusDays(7),
                "AB-123-CD",
                List.of()
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                reservationService.addReservation(1, req)
        );
        assertEquals("Mobilhome ID must not be null", ex.getMessage());
    }

    @Test
    void addReservation_ThrowsException_WhenUserIdIsNull() {
        ReservationVacationerRequestDto req = new ReservationVacationerRequestDto(
                10,
                LocalDate.now(),
                LocalDate.now().plusDays(7),
                "AB-123-CD",
                List.of()
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                reservationService.addReservation(null, req)
        );
        assertEquals("User ID must not be null", ex.getMessage());
    }

    @Test
    void addReservation_Success() {
        ReservationVacationerRequestDto req = new ReservationVacationerRequestDto(
                10,
                LocalDate.now(),
                LocalDate.now().plusDays(7),
                "AB-123-CD",
                List.of(new OccupantRequestDto("Jane", "Doe", 30))
        );

        when(vacationerRepository.findById(1)).thenReturn(Optional.of(vacationer));
        when(mobilhomeRepository.findById(10)).thenReturn(Optional.of(mobilhome));

        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> {
            Reservation r = i.getArgument(0);
            r.setId(50);
            return r;
        });

        ReservationVacationerResponseDto response = reservationService.addReservation(1, req);

        assertNotNull(response);
        assertEquals(50, response.id());
        verify(reservationRepository, times(1)).save(any(Reservation.class));
    }

    @Test
    void getReservationsByUserId_SupportsReservationWithoutContract() {
        Reservation reservation = Reservation.builder()
                .id(50)
                .mobilhome(mobilhome)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(7))
                .status(ReservationStatus.PENDING)
                .build();
        when(reservationRepository.findByVacationerId(1)).thenReturn(List.of(reservation));

        List<ReservationVacationerResponseDto> responses = reservationService.getReservationsByUserId(1);

        assertEquals(1, responses.size());
        assertEquals(50, responses.getFirst().id());

    }

    // ----------------------------------------------------------------
    // Règles revérifiées côté serveur
    // ----------------------------------------------------------------

    private ReservationVacationerRequestDto request(LocalDate start, LocalDate end, int occupants) {
        List<OccupantRequestDto> list = java.util.stream.IntStream.range(0, occupants)
                .mapToObj(i -> new OccupantRequestDto("P" + i, "Doe", 30)).toList();
        return new ReservationVacationerRequestDto(10, start, end, "AB-123-CD", list);
    }

    @Test
    void addReservation_datesInversees_refuse() {
        LocalDate d = LocalDate.now().plusDays(10);
        assertThrows(IllegalArgumentException.class,
                () -> reservationService.addReservation(1, request(d, d.minusDays(2), 1)));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void addReservation_dansLePasse_refuse() {
        LocalDate d = LocalDate.now().minusDays(3);
        assertThrows(IllegalArgumentException.class,
                () -> reservationService.addReservation(1, request(d, d.plusDays(7), 1)));
    }

    @Test
    void addReservation_creneauDejaValide_refuse() {
        LocalDate d = LocalDate.now().plusDays(10);
        when(vacationerRepository.findById(1)).thenReturn(Optional.of(vacationer));
        when(mobilhomeRepository.findById(10)).thenReturn(Optional.of(mobilhome));
        when(reservationRepository.existsOverlap(10, -1, ReservationStatus.VALIDATED, d, d.plusDays(7))).thenReturn(true);

        assertThrows(BusinessRuleException.class,
                () -> reservationService.addReservation(1, request(d, d.plusDays(7), 1)));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void addReservation_tropDOccupants_refuse() {
        mobilhome.setCapacity(4);
        LocalDate d = LocalDate.now().plusDays(10);
        when(vacationerRepository.findById(1)).thenReturn(Optional.of(vacationer));
        when(mobilhomeRepository.findById(10)).thenReturn(Optional.of(mobilhome));

        assertThrows(BusinessRuleException.class,
                () -> reservationService.addReservation(1, request(d, d.plusDays(7), 5)));
    }

    private Reservation draft(ReservationStatus status) {
        return Reservation.builder().id(50).vacationer(vacationer).mobilhome(mobilhome)
                .startDate(LocalDate.now().plusDays(10)).endDate(LocalDate.now().plusDays(17))
                .status(status).build();
    }

    private final ReservationVacationerUpdateRequestDto complete =
            new ReservationVacationerUpdateRequestDto("AB-123-CD", List.of(new OccupantRequestDto("Jane", "Doe", 30)));

    @Test
    void submitReservation_brouillonComplet_passeATraiter() {
        when(reservationRepository.findById(50)).thenReturn(Optional.of(draft(ReservationStatus.PENDING)));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));

        assertEquals(ReservationStatus.COMPLETED, reservationService.submitReservation(1, 50, complete).status());
    }

    @Test
    void submitReservation_sansPlaque_refuse() {
        when(reservationRepository.findById(50)).thenReturn(Optional.of(draft(ReservationStatus.PENDING)));

        assertThrows(IllegalArgumentException.class, () -> reservationService.submitReservation(1, 50,
                new ReservationVacationerUpdateRequestDto(" ", complete.occupants())));
    }

    @Test
    void updateReservation_dejaValidee_refuse() {
        // Sans ce contrôle, modifier une réservation validée la faisait repasser en brouillon.
        Reservation validated = draft(ReservationStatus.VALIDATED);
        when(reservationRepository.findById(50)).thenReturn(Optional.of(validated));

        assertThrows(BusinessRuleException.class, () -> reservationService.updateReservation(1, 50, complete));
        assertEquals(ReservationStatus.VALIDATED, validated.getStatus());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void updateReservation_dUnAutreVacancier_renvoieNotFound() {
        when(reservationRepository.findById(50)).thenReturn(Optional.of(draft(ReservationStatus.PENDING)));

        assertThrows(NotFoundException.class, () -> reservationService.updateReservation(2, 50, complete));
    }

    @Test
    void submitReservation_inexistante_renvoieNotFound() {
        when(reservationRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> reservationService.submitReservation(1, 999, complete));
    }
}
