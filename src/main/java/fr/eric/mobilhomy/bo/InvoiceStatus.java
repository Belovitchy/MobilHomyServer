package fr.eric.mobilhomy.bo;

public enum InvoiceStatus {
    ISSUED,     // émise, en attente de paiement
    PAID,       // payée
    CANCELLED   // annulée (erreur, désistement...)
}
