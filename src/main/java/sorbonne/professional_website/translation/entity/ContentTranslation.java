package sorbonne.professional_website.translation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "content_translation",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_content_translation",
                columnNames = {"content_type", "content_key", "locale", "field_name"}
        ),
        indexes = {
                @Index(name = "idx_content_translation_lookup", columnList = "content_type,content_key,locale,status"),
                @Index(name = "idx_content_translation_locale", columnList = "locale,status")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "content_translation_seq")
    @SequenceGenerator(
            name = "content_translation_seq",
            sequenceName = "content_translation_seq",
            allocationSize = 1
    )
    @Column(name = "translation_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 40)
    private TranslationContentType contentType;

    @Column(name = "content_key", nullable = false, length = 160)
    private String contentKey;

    @Column(nullable = false, length = 12)
    private String locale;

    @Column(name = "field_name", nullable = false, length = 80)
    private String fieldName;

    @Column(name = "translated_text", nullable = false, columnDefinition = "TEXT")
    private String translatedText;

    @Column(name = "source_hash", nullable = false, length = 64)
    private String sourceHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TranslationStatus status = TranslationStatus.DRAFT;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
