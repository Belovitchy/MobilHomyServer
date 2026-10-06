package fr.eric.mobilhomy.dal;

import fr.eric.mobilhomy.bo.Occupant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OccupantRepository extends JpaRepository<Occupant, Integer> {
}
