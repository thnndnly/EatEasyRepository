package de.eateasy.ingredient.service;

import de.eateasy.common.exception.NotFoundException;
import de.eateasy.common.units.Unit;
import de.eateasy.ingredient.dto.IngredientDto;
import de.eateasy.ingredient.entity.IngredientCategory;
import de.eateasy.ingredient.repository.IngredientRepository;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@QuarkusTest
class IngredientServiceImplTest {

    @Inject
    IngredientService ingredientService;

    @Inject
    IngredientRepository ingredientRepository;

    @BeforeEach
    @Transactional
    void cleanUp() {
        ingredientRepository.deleteAll();
    }

    @Test
    @TestTransaction
    @DisplayName("findOrCreate legt neue Zutat an")
    void findOrCreateNew() {
        IngredientDto dto = ingredientService.findOrCreate("Tomate", Unit.PIECE);

        assertThat(dto.id()).isNotNull();
        assertThat(dto.name()).isEqualTo("Tomate");
        assertThat(dto.defaultUnit()).isEqualTo(Unit.PIECE);
    }

    @Test
    @TestTransaction
    @DisplayName("findOrCreate ist case-insensitive idempotent")
    void findOrCreateIdempotent() {
        IngredientDto first = ingredientService.findOrCreate("Tomate", Unit.PIECE);
        IngredientDto second = ingredientService.findOrCreate("TOMATE", Unit.GRAM);

        assertThat(second.id()).isEqualTo(first.id());
        // Existierende Einheit wird beibehalten — der zweite Aufruf hat sie nicht ueberschrieben.
        assertThat(second.defaultUnit()).isEqualTo(Unit.PIECE);
    }

    @Test
    @TestTransaction
    @DisplayName("search liefert Treffer mit Substring-Match")
    void searchSubstring() {
        ingredientService.findOrCreate("Tomate", Unit.PIECE);
        ingredientService.findOrCreate("Tomatenmark", Unit.GRAM);
        ingredientService.findOrCreate("Zwiebel", Unit.PIECE);

        List<IngredientDto> hits = ingredientService.search("toma", 10);

        assertThat(hits).extracting(IngredientDto::name)
            .containsExactlyInAnyOrder("Tomate", "Tomatenmark");
    }

    @Test
    @TestTransaction
    @DisplayName("findOrCreate legt Zutat mit Default-Kategorie SONSTIGES an")
    void findOrCreateDefaultsToSonstiges() {
        IngredientDto dto = ingredientService.findOrCreate("Tomate", Unit.PIECE);

        assertThat(dto.category()).isEqualTo(IngredientCategory.SONSTIGES);
    }

    @Test
    @TestTransaction
    @DisplayName("updateCategory setzt die Kategorie")
    void updateCategorySetsCategory() {
        IngredientDto created = ingredientService.findOrCreate("Tomate", Unit.PIECE);

        IngredientDto updated = ingredientService.updateCategory(
            created.id(), IngredientCategory.OBST_GEMUESE);

        assertThat(updated.category()).isEqualTo(IngredientCategory.OBST_GEMUESE);
        assertThat(ingredientService.getById(created.id()).category())
            .isEqualTo(IngredientCategory.OBST_GEMUESE);
    }

    @Test
    @TestTransaction
    @DisplayName("updateCategory mit unbekannter ID wirft NotFoundException")
    void updateCategoryUnknownIdThrows() {
        assertThatThrownBy(() ->
            ingredientService.updateCategory(UUID.randomUUID(), IngredientCategory.VORRAT))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    @TestTransaction
    @DisplayName("getByIds liefert Map mit gefundenen IDs")
    void getByIdsReturnsMap() {
        IngredientDto a = ingredientService.findOrCreate("Tomate", Unit.PIECE);
        IngredientDto b = ingredientService.findOrCreate("Zwiebel", Unit.PIECE);

        Map<UUID, IngredientDto> map = ingredientService.getByIds(List.of(a.id(), b.id()));

        assertThat(map).hasSize(2);
        assertThat(map.get(a.id()).name()).isEqualTo("Tomate");
        assertThat(map.get(b.id()).name()).isEqualTo("Zwiebel");
    }
}
