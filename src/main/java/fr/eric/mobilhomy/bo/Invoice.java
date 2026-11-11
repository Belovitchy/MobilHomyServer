package fr.eric.mobilhomy.bo;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Facture de fonctionnement saisie par un gérant pour un mobil-home qu'il gère
 * (ménage, petite réparation, gaz, consommables…). Elle est à la charge du propriétaire
 * du mobil-home : type OUTGOING (dépense), statut ISSUED tant qu'elle n'est pas réglée.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Propriétaire qui doit la facture (= propriétaire du mobil-home au moment de la saisie). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    @ToString.Exclude
    private Owner owner;

    /** Gérant qui a saisi la facture. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    @ToString.Exclude
    private Manager manager;

    /** Mobil-home concerné. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mobilhome_id")
    @ToString.Exclude
    private Mobilhome mobilhome;

    private String description;

    @Column(precision = 10, scale = 2)
    private BigDecimal amount;

    /** Date de la prestation ou de l'achat. */
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    private InvoiceCategory category;

    @Enumerated(EnumType.STRING)
    private InvoiceType type;

    @Enumerated(EnumType.STRING)
    private InvoiceStatus status;

}
