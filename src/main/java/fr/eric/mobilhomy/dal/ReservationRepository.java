package fr.eric.mobilhomy.dal;

import fr.eric.mobilhomy.bo.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Integer> {
}
