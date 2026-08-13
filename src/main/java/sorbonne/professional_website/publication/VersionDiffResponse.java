package sorbonne.professional_website.publication;

import java.util.List;

public record VersionDiffResponse(Long fromVersionId, Long toVersionId, int changeCount, List<VersionDiffEntry> changes) {}
