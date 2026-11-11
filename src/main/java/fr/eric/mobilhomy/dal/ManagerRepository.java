package fr.eric.mobilhomy.dal;

import fr.eric.mobilhomy.bo.Manager;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ManagerRepository extends JpaRepository<Manager, Integer> {
}
