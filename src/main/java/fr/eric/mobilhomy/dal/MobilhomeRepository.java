package fr.eric.mobilhomy.dal;

import fr.eric.mobilhomy.bo.Mobilhome;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MobilhomeRepository extends JpaRepository<Mobilhome, Integer> {

    @Query("SELECT DISTINCT m FROM Mobilhome m LEFT JOIN FETCH m.images WHERE m.postalCode LIKE CONCAT(:department, '%')")
    List<Mobilhome> findAllByDepartement(@Param("department") String department);

    @Query("SELECT DISTINCT m FROM Mobilhome m LEFT JOIN FETCH m.images WHERE m.capacity >= :capacity")
    List<Mobilhome> findAllByCapacity(@Param("capacity") int capacity);

    @Query("SELECT DISTINCT m FROM Mobilhome m LEFT JOIN FETCH m.images WHERE m.postalCode LIKE CONCAT(:department, '%') AND m.capacity >= :capacity")
    List<Mobilhome> search(@Param("department") String department, @Param("capacity") int capacity);
}
