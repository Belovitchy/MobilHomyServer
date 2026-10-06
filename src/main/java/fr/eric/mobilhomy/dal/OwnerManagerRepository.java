package fr.eric.mobilhomy.dal;

import fr.eric.mobilhomy.bo.OwnerManager;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OwnerManagerRepository extends JpaRepository<OwnerManager, Integer> {
}
