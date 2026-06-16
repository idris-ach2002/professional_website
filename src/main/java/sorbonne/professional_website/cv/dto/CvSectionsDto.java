package sorbonne.professional_website.cv.dto;

public record CvSectionsDto(
        Boolean profile,
        Boolean skills,
        Boolean experiences,
        Boolean education,
        Boolean projects,
        Boolean qualities,
        Boolean languages
) {
}
