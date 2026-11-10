package fr.eric.mobilhomy.dal;

import fr.eric.mobilhomy.bo.Manager;
import fr.eric.mobilhomy.bo.OwnerManager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OwnerManagerRepository extends JpaRepository<OwnerManager, Integer> {

    @Query("SELECT om.manager FROM OwnerManager om WHERE om.owner.id = :ownerId ORDER BY om.manager.name")
    List<Manager> findManagersByOwnerId(@Param("ownerId") Integer ownerId);

    Optional<OwnerManager> findByOwnerIdAndManagerId(Integer ownerId, Integer managerId);

    boolean existsByOwnerIdAndManagerId(Integer ownerId, Integer managerId);

    long countByManagerId(Integer managerId);
}
