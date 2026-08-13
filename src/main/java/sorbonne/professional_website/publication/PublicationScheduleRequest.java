package sorbonne.professional_website.publication;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record PublicationScheduleRequest(@NotNull OffsetDateTime publishAt) {}
