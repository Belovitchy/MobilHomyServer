package fr.eric.mobilhomy.bo;

/**
 * Origine d'une réservation. Les réservations prises sur une autre plateforme (souvent Booking, dont le
 * calendrier iCal est synchronisé avec Abritel, Airbnb…) sont saisies par le propriétaire lui-même :
 * il renseigne le client, la plaque et les occupants nécessaires à la déclaration au camping.
 */
public enum ReservationSource {
    MOBILHOMY,  // demande faite par un vacancier sur la plateforme
    BOOKING,
    AIRBNB,
    ABRITEL,
    OTHER       // téléphone, bouche-à-oreille…
}
