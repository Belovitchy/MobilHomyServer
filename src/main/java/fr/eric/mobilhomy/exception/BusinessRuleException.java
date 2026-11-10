package fr.eric.mobilhomy.exception;

/**
 * Règle métier violée (transition de statut interdite, chevauchement de dates,
 * email déjà utilisé...). Traduite en 409 Conflict par ApiExceptionHandler.
 */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
