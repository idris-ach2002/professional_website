package sorbonne.professional_website.translation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sorbonne.professional_website.translation.entity.ContentTranslation;
import sorbonne.professional_website.translation.entity.TranslationContentType;
import sorbonne.professional_website.translation.entity.TranslationStatus;

import java.util.List;
import java.util.Optional;

public interface ContentTranslationRepository extends JpaRepository<ContentTranslation, Long> {

    List<ContentTranslation> findByContentTypeAndContentKeyAndLocale(
            TranslationContentType contentType,
            String contentKey,
            String locale
    );

    List<ContentTranslation> findByContentTypeAndContentKeyAndLocaleAndStatus(
            TranslationContentType contentType,
            String contentKey,
            String locale,
            TranslationStatus status
    );

    Optional<ContentTranslation> findByContentTypeAndContentKeyAndLocaleAndFieldName(
            TranslationContentType contentType,
            String contentKey,
            String locale,
            String fieldName
    );
}
