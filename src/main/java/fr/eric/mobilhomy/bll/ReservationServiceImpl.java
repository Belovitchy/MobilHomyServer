package fr.eric.mobilhomy.bll;

import fr.eric.mobilhomy.bo.*;
import fr.eric.mobilhomy.dal.ContractRepository;
import fr.eric.mobilhomy.dal.MobilhomeRepository;
import fr.eric.mobilhomy.dal.ReservationRepository;
import fr.eric.mobilhomy.dal.VacationerRepository;
import fr.eric.mobilhomy.dto.ReservationVacationerRequestDto;
import fr.eric.mobilhomy.dto.ReservationVacationerResponseDto;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class ReservationServiceImpl implements ReservationService{

    private final ReservationRepository reservationRepository;
    private final VacationerRepository vacationerRepository;
    private final MobilhomeRepository mobilhomeRepository;
    private final ContractRepository contractRepository;

    @Override
    public ReservationVacationerResponseDto addReservation(Integer userId, ReservationVacationerRequestDto req) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        if (req == null || req.mobilhomeId() == null) {
            throw new IllegalArgumentException("Mobilhome ID must not be null");
        }
        Vacationer vacationer = vacationerRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Vacationer not found with id: " + userId));
        Mobilhome mobilhome = mobilhomeRepository.findById(req.mobilhomeId())
                .orElseThrow(() -> new RuntimeException("Mobilhome not found with id: " + req.mobilhomeId()));
        Contract contract = contractRepository.save(Contract.builder()
                .vacationer(vacationer).mobilhome(mobilhome).signed(false).build());
        Reservation res = Reservation.builder()
                .vacationer(vacationer)
                .mobilhome(mobilhome)
                .startDate(req.startDate())
                .endDate(req.endDate())
                .numberOfPerson(req.occupants() == null? 0: req.occupants().size())
                .immat(req.immat())
                .status(ReservationStatus.PENDING)
                .contract(contract)
                .build();
        if (req.occupants() != null) {
            req.occupants().forEach(o -> res.getOccupants().add(Occupant.builder()
                            .firstname(o.firstName())
                            .name(o.lastName())
                            .age(o.age())
                            .reservation(res)
                            .build()));
        }
        Reservation savedReservation = reservationRepository.save(res);
        return ReservationVacationerResponseDto.fromEntity(savedReservation);
    }

    @Override
    public List<ReservationVacationerResponseDto> getReservationsByUserId(Integer userId) {
        if (userId == null) {
            return List.of();
        }
        return reservationRepository.findByVacationerId(userId)
                .stream()
                .map(ReservationVacationerResponseDto::fromEntity)
                .toList();
    }
}
