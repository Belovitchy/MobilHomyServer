package fr.eric.mobilhomy.dal;

import fr.eric.mobilhomy.bo.Mobilhome;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MobilhomeRepository extends JpaRepository<Mobilhome, Integer> {
}
