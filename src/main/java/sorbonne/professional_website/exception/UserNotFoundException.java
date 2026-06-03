package sorbonne.professional_website.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long userId) {
        super("Utilisateur introuvable.");
    }
}