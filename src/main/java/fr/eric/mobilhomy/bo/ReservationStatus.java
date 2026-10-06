package fr.eric.mobilhomy.bo;

public enum ReservationStatus {

    PENDING,      // demande envoyée par le vacancier, en attente
    VALIDATED,    // validée par le propriétaire
    REJECTED,     // refusée (tu en auras besoin un jour !)
    CANCELLED,    // annulée
    COMPLETED    // séjour terminé
}
