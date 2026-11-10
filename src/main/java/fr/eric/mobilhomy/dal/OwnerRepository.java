package fr.eric.mobilhomy.dal;

import fr.eric.mobilhomy.bo.Owner;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OwnerRepository extends JpaRepository<Owner, Integer> {
}
