package fr.eric.mobilhomy.bo;

/**
 * Cycle de vie d'une réservation :
 * <pre>
 *   PENDING ──(le vacancier envoie son dossier complet)──▶ COMPLETED ──▶ VALIDATED | REJECTED
 *   VALIDATED ──(annulation)──▶ CANCELLED
 * </pre>
 * Attention : COMPLETED signifie « dossier complété et soumis », donc « à traiter » par le
 * propriétaire — pas « séjour terminé ». Un séjour passé reste VALIDATED.
 */
public enum ReservationStatus {

    PENDING,      // brouillon : demande créée, dossier (occupants, plaque) encore à compléter
    VALIDATED,    // acceptée par le propriétaire (ou réservation externe, confirmée d'office)
    REJECTED,     // refusée par le propriétaire
    CANCELLED,    // annulée
    COMPLETED     // dossier complété et soumis : à traiter par le propriétaire
}
