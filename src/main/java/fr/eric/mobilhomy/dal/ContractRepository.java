package fr.eric.mobilhomy.dal;

import fr.eric.mobilhomy.bo.Contract;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractRepository extends JpaRepository<Contract, Integer> {
}
