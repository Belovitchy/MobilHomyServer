package fr.eric.mobilhomy.dal;

import fr.eric.mobilhomy.bo.Vacationer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VacationerRepository extends JpaRepository<Vacationer, Integer> {
}
