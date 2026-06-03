package sorbonne.professional_website.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName) {
        super(resourceName + " introuvable.");
    }
}