package fr.eric.mobilhomy.dal;

import fr.eric.mobilhomy.bo.Mobilhome;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MobilhomeRepository extends JpaRepository<Mobilhome, Integer> {

    @Query("SELECT DISTINCT m FROM Mobilhome m LEFT JOIN FETCH m.images WHERE m.postalCode LIKE CONCAT(:department, '%')")
    List<Mobilhome> findAllByDepartement(@Param("department") String department);

    @Query("SELECT DISTINCT m FROM Mobilhome m LEFT JOIN FETCH m.images WHERE m.capacity >= :capacity")
    List<Mobilhome> findAllByCapacity(@Param("capacity") int capacity);

    @Query("SELECT DISTINCT m FROM Mobilhome m LEFT JOIN FETCH m.images WHERE m.postalCode LIKE CONCAT(:department, '%') AND m.capacity >= :capacity")
    List<Mobilhome> search(@Param("department") String department, @Param("capacity") int capacity);

    // ----- Espace propriétaire -----

    @Query("SELECT DISTINCT m FROM Mobilhome m LEFT JOIN FETCH m.images WHERE m.owner.id = :ownerId ORDER BY m.name")
    List<Mobilhome> findAllByOwnerIdWithImages(@Param("ownerId") Integer ownerId);

    /** Ne renvoie le mobil-home que s'il appartient au propriétaire (contrôle de propriété). */
    Optional<Mobilhome> findByIdAndOwnerId(Integer id, Integer ownerId);

    List<Mobilhome> findByOwnerIdAndManagerId(Integer ownerId, Integer managerId);
}
