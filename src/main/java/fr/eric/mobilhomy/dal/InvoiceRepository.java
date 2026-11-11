package fr.eric.mobilhomy.dal;

import fr.eric.mobilhomy.bo.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Integer> {

    @Query("SELECT i FROM Invoice i LEFT JOIN FETCH i.mobilhome " +
            "WHERE i.manager.id = :managerId ORDER BY i.date DESC, i.id DESC")
    List<Invoice> findAllByManagerId(@Param("managerId") Integer managerId);

    /** Ne renvoie la facture que si elle a été saisie par ce gérant (contrôle de propriété). */
    Optional<Invoice> findByIdAndManagerId(Integer id, Integer managerId);

    boolean existsByMobilhomeId(Integer mobilhomeId);

    boolean existsByManagerId(Integer managerId);
}
