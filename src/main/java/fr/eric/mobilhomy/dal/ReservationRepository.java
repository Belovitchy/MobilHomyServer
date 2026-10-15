package fr.eric.mobilhomy.dal;

import fr.eric.mobilhomy.bo.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Integer> {
    List<Reservation> findByVacationerId(Integer vacationerId);
}
