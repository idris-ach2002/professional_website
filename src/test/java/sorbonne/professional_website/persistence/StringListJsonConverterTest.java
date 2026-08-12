package sorbonne.professional_website.persistence;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StringListJsonConverterTest {

    private final StringListJsonConverter converter = new StringListJsonConverter();

    @Test
    void preservesOrderAndCharactersWithoutCreatingCollectionTables() {
        List<String> source = List.of("React, Spring", "Choix → worker asynchrone", "ligne avec \"guillemets\"");

        String stored = converter.convertToDatabaseColumn(source);
        List<String> restored = converter.convertToEntityAttribute(stored);

        assertThat(restored).containsExactlyElementsOf(source);
        assertThat(restored).isNotSameAs(source);
    }

    @Test
    void nullAndBlankDatabaseValuesBecomeIndependentEmptyLists() {
        assertThat(converter.convertToEntityAttribute(null)).isEmpty();
        assertThat(converter.convertToEntityAttribute(" ")).isEmpty();
        assertThat(converter.convertToDatabaseColumn(null)).isEqualTo("[]");
    }

    @Test
    void corruptPersistedJsonFailsLoudly() {
        assertThatThrownBy(() -> converter.convertToEntityAttribute("not-json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalide");
    }
}
