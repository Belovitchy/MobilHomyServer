package fr.eric.mobilhomy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Indicateurs de bon fonctionnement de la plateforme (tableau de bord administrateur).
 * Volontairement sans données métier des propriétaires (demandes à traiter, factures) :
 * celles-ci relèvent de leur propre tableau de bord.
 */
public record AdminStatsResponseDto(
        long owners,
        @JsonProperty("owners_suspended") long ownersSuspended,
        long managers,
        long vacationers,
        long mobilhomes,
        long reservations,
        @JsonProperty("reservations_validated") long reservationsValidated
) {
}
