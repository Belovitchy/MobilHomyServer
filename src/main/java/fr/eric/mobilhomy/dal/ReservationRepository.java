package fr.eric.mobilhomy.dal;

import fr.eric.mobilhomy.bo.Reservation;
import fr.eric.mobilhomy.bo.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Integer> {
    List<Reservation> findByVacationerId(Integer vacationerId);

    // ----- Espace propriétaire -----

    @Query("SELECT r FROM Reservation r JOIN FETCH r.mobilhome m LEFT JOIN FETCH r.vacationer " +
            "WHERE m.owner.id = :ownerId ORDER BY r.startDate")
    List<Reservation> findAllByOwnerId(@Param("ownerId") Integer ownerId);

    @Query("SELECT r FROM Reservation r JOIN FETCH r.mobilhome m LEFT JOIN FETCH r.vacationer " +
            "WHERE m.owner.id = :ownerId AND r.status = :status ORDER BY r.startDate")
    List<Reservation> findAllByOwnerIdAndStatus(@Param("ownerId") Integer ownerId,
                                                @Param("status") ReservationStatus status);

    /** Ne renvoie la réservation que si elle porte sur un mobil-home du propriétaire. */
    Optional<Reservation> findByIdAndMobilhomeOwnerId(Integer id, Integer ownerId);

    boolean existsByMobilhomeIdAndStatusIn(Integer mobilhomeId, Collection<ReservationStatus> statuses);

    /**
     * Chevauchement de périodes : [start, end[ chevauche [s, e[ si start < e ET end > s.
     * La date de départ n'est pas une nuit occupée : un départ le 19 et une arrivée le 19 ne se chevauchent pas.
     */
    @Query("SELECT COUNT(r) > 0 FROM Reservation r " +
            "WHERE r.mobilhome.id = :mobilhomeId AND r.id <> :excludedId AND r.status = :status " +
            "AND r.startDate < :endDate AND r.endDate > :startDate")
    boolean existsOverlap(@Param("mobilhomeId") Integer mobilhomeId,
                          @Param("excludedId") Integer excludedId,
                          @Param("status") ReservationStatus status,
                          @Param("startDate") LocalDate startDate,
                          @Param("endDate") LocalDate endDate);
}
