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
        assertEquals(50, responses.get(0).id());

    }
}
