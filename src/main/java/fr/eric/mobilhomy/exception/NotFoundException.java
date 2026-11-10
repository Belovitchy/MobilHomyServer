package fr.eric.mobilhomy.exception;

/**
 * Ressource introuvable OU n'appartenant pas à l'utilisateur connecté.
 * On renvoie volontairement 404 dans les deux cas : un propriétaire ne doit
 * pas pouvoir deviner l'existence des ressources d'un autre (anti-IDOR).
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
