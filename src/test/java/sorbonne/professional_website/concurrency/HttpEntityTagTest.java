package sorbonne.professional_website.concurrency;

import org.junit.jupiter.api.Test;
import sorbonne.professional_website.exception.PreconditionFailedException;
import sorbonne.professional_website.exception.PreconditionRequiredException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpEntityTagTest {

    @Test
    void strongTagsRoundTripTheirRevision() {
        assertThat(HttpEntityTag.version(42L, 7L)).isEqualTo("\"version-42-7\"");
        assertThat(HttpEntityTag.owner(8L, 3L)).isEqualTo("\"owner-8-3\"");
        assertThat(HttpEntityTag.requireRevision("\"version-42-7\"", "version", 42L)).isEqualTo(7L);
    }

    @Test
    void missingPreconditionIsRejectedExplicitly() {
        assertThatThrownBy(() -> HttpEntityTag.requireRevision(null, "version", 42L))
                .isInstanceOf(PreconditionRequiredException.class);
    }

    @Test
    void wildcardMalformedAndCrossResourceTagsAreRejected() {
        assertThatThrownBy(() -> HttpEntityTag.requireRevision("*", "version", 42L))
                .isInstanceOf(PreconditionFailedException.class);
        assertThatThrownBy(() -> HttpEntityTag.requireRevision("W/\"version-42-7\"", "version", 42L))
                .isInstanceOf(PreconditionFailedException.class);
        assertThatThrownBy(() -> HttpEntityTag.requireRevision("\"version-41-7\"", "version", 42L))
                .isInstanceOf(PreconditionFailedException.class);
        assertThatThrownBy(() -> HttpEntityTag.requireRevision("\"owner-42-7\"", "version", 42L))
                .isInstanceOf(PreconditionFailedException.class);
    }

    @Test
    void oversizedNumericTagIsRejectedAsAPreconditionFailure() {
        assertThatThrownBy(() -> HttpEntityTag.requireRevision(
                "\"version-999999999999999999999999999999-1\"",
                "version",
                1L
        )).isInstanceOf(PreconditionFailedException.class);
    }
}
