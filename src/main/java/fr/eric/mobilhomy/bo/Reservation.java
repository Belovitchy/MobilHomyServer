package fr.eric.mobilhomy.bo;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Vacancier MobilHomy — null pour une réservation externe saisie par le propriétaire. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vacationer_id")
    private Vacationer vacationer;

    /** Origine : MobilHomy (demande d'un vacancier) ou plateforme externe saisie par le propriétaire. */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ReservationSource source = ReservationSource.MOBILHOMY;

    /** Client d'une réservation externe (pas de compte vacancier). */
    private String guestName;
    private String guestPhone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mobilhome_id")
    private Mobilhome mobilhome;

    private LocalDate startDate;
    private LocalDate endDate;
    private int numberOfPerson;
    private String immat;
    private String sibluRef;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.PENDING;

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Occupant> occupants = new ArrayList<>();

    /** Réservation prise hors MobilHomy : ses données sont saisies et modifiables par le propriétaire. */
    public boolean isExternal() {
        return source != null && source != ReservationSource.MOBILHOMY;
    }

    /** Nom du client : le vacancier MobilHomy, ou le client saisi pour une réservation externe. */
    public String getClientName() {
        return vacationer != null ? vacationer.getFirstname() + " " + vacationer.getName() : guestName;
    }

    /** Téléphone du client (vacancier MobilHomy ou client externe). */
    public String getClientPhone() {
        return vacationer != null ? vacationer.getPhone() : guestPhone;
    }

}
