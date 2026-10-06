package fr.eric.mobilhomy.dal;

import fr.eric.mobilhomy.bo.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Integer> {
}
