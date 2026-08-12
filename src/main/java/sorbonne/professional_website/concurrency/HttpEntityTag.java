package sorbonne.professional_website.concurrency;

import sorbonne.professional_website.exception.PreconditionFailedException;
import sorbonne.professional_website.exception.PreconditionRequiredException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Strong ETag contract for admin mutations. The tag encodes only a stable
 * aggregate id + revision and is therefore safe to compare across JVMs.
 */
public final class HttpEntityTag {

    private static final Pattern TAG = Pattern.compile("\\\"([a-z]+)-(\\d+)-(\\d+)\\\"");

    private HttpEntityTag() {
    }

    public static String version(long id, long revision) {
        return "\"version-" + id + "-" + revision + "\"";
    }

    public static String owner(long id, long revision) {
        return "\"owner-" + id + "-" + revision + "\"";
    }

    public static long requireRevision(String rawIfMatch, String expectedKind, long expectedId) {
        if (rawIfMatch == null || rawIfMatch.isBlank()) {
            throw new PreconditionRequiredException("La mutation exige un en-tête If-Match issu de la dernière lecture.");
        }
        if ("*".equals(rawIfMatch.trim())) {
            throw new PreconditionFailedException("If-Match '*' n'est pas autorisé pour les mutations administrateur.");
        }

        Matcher matcher = TAG.matcher(rawIfMatch.trim());
        if (!matcher.matches()) {
            throw new PreconditionFailedException("En-tête If-Match invalide ou obsolète.");
        }

        String kind = matcher.group(1);
        final long id;
        final long revision;
        try {
            id = Long.parseLong(matcher.group(2));
            revision = Long.parseLong(matcher.group(3));
        } catch (NumberFormatException exception) {
            throw new PreconditionFailedException("En-tête If-Match invalide ou obsolète.");
        }
        if (!expectedKind.equals(kind) || id != expectedId) {
            throw new PreconditionFailedException("L'ETag ne correspond pas à la ressource modifiée.");
        }
        return revision;
    }
}
