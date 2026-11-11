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

    @Query("SELECT i FROM Invoice i LEFT JOIN FETCH i.mobilhome LEFT JOIN FETCH i.manager " +
            "WHERE i.owner.id = :ownerId ORDER BY i.date DESC, i.id DESC")
    List<Invoice> findAllByOwnerId(@Param("ownerId") Integer ownerId);

    /** Ne renvoie la facture que si elle est due par ce propriétaire (contrôle de propriété). */
    Optional<Invoice> findByIdAndOwnerId(Integer id, Integer ownerId);

    /** Ne renvoie la facture que si elle a été saisie par ce gérant (contrôle de propriété). */
    Optional<Invoice> findByIdAndManagerId(Integer id, Integer managerId);

    boolean existsByMobilhomeId(Integer mobilhomeId);

    boolean existsByManagerId(Integer managerId);

    @Query("SELECT COALESCE(SUM(i.amount), 0) FROM Invoice i WHERE i.status = :status")
    java.math.BigDecimal sumAmountByStatus(@Param("status") fr.eric.mobilhomy.bo.InvoiceStatus status);
}
