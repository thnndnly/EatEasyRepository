package de.eateasy.recipe.service;

import de.eateasy.auth.dto.AuthResponse;
import de.eateasy.auth.dto.RegisterRequest;
import de.eateasy.auth.repository.UserRepository;
import de.eateasy.auth.service.AuthService;
import de.eateasy.common.diet.DietTag;
import de.eateasy.common.exception.BadRequestException;
import de.eateasy.common.exception.ForbiddenException;
import de.eateasy.common.units.Unit;
import de.eateasy.household.dto.HouseholdCreateRequest;
import de.eateasy.household.dto.InvitationCreateRequest;
import de.eateasy.household.dto.InvitationDto;
import de.eateasy.household.repository.HouseholdInvitationRepository;
import de.eateasy.household.repository.HouseholdMembershipRepository;
import de.eateasy.household.repository.HouseholdRepository;
import de.eateasy.household.service.HouseholdService;
import de.eateasy.ingredient.dto.IngredientDto;
import de.eateasy.ingredient.repository.IngredientRepository;
import de.eateasy.ingredient.service.IngredientService;
import de.eateasy.recipe.dto.RecipeCreateRequest;
import de.eateasy.recipe.dto.RecipeDto;
import de.eateasy.recipe.dto.RecipeFilter;
import de.eateasy.recipe.dto.RecipeIngredientRequest;
import de.eateasy.recipe.dto.RecipeUpdateRequest;
import de.eateasy.recipe.repository.RecipeFavoriteRepository;
import de.eateasy.recipe.repository.RecipeRepository;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@QuarkusTest
class RecipeServiceImplTest {

    @Inject
    RecipeService recipeService;

    @Inject
    HouseholdService householdService;

    @Inject
    IngredientService ingredientService;

    @Inject
    AuthService authService;

    @Inject
    RecipeRepository recipeRepository;

    @Inject
    RecipeFavoriteRepository favoriteRepository;

    @Inject
    IngredientRepository ingredientRepository;

    @Inject
    HouseholdInvitationRepository invitationRepository;

    @Inject
    HouseholdMembershipRepository membershipRepository;

    @Inject
    HouseholdRepository householdRepository;

    @Inject
    UserRepository userRepository;

    @BeforeEach
    @Transactional
    void cleanUp() {
        recipeRepository.deleteAll();
        ingredientRepository.deleteAll();
        invitationRepository.deleteAll();
        membershipRepository.deleteAll();
        householdRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @TestTransaction
    @DisplayName("create legt Rezept mit neuen Zutaten via findOrCreate an")
    void createWithNewIngredients() {
        UUID userId = registerUser("alice@example.com", "Alice");

        RecipeDto recipe = recipeService.create(userId, new RecipeCreateRequest(
            "Tomatensuppe",
            "Klassiker",
            "1. Zwiebel anbraten\n2. Tomaten dazu",
            4,
            30,
            List.of(DietTag.VEGAN),
            null,
            List.of(
                new RecipeIngredientRequest(null, "Tomate", new BigDecimal("500"), Unit.GRAM, null),
                new RecipeIngredientRequest(null, "Zwiebel", new BigDecimal("1"), Unit.PIECE, "fein gehackt"))));

        assertThat(recipe.id()).isNotNull();
        assertThat(recipe.title()).isEqualTo("Tomatensuppe");
        assertThat(recipe.servings()).isEqualTo(4);
        assertThat(recipe.dietTags()).containsExactly(DietTag.VEGAN);
        assertThat(recipe.ingredients()).hasSize(2);
        assertThat(recipe.ingredients()).extracting(ri -> ri.ingredientName())
            .containsExactlyInAnyOrder("Tomate", "Zwiebel");
    }

    @Test
    @TestTransaction
    @DisplayName("create wirft BadRequest bei leerer Zutatenliste über DTO-Validation kommt — hier explizit Service-Side")
    void createRejectsUnknownDietTag() {
        UUID userId = registerUser("alice@example.com", "Alice");

        assertThatThrownBy(() -> recipeService.create(userId, new RecipeCreateRequest(
            "Test", null, "Steps", 2, null,
            List.of("paleo"),
            null,
            List.of(new RecipeIngredientRequest(null, "Tomate", BigDecimal.ONE, Unit.PIECE, null)))))
            .isInstanceOf(BadRequestException.class);
    }

    @Test
    @TestTransaction
    @DisplayName("create mit fremdem Haushalt wirft Forbidden")
    void createWithForeignHouseholdForbidden() {
        UUID alice = registerUser("alice@example.com", "Alice");
        UUID bob = registerUser("bob@example.com", "Bob");
        UUID bobsHouse = householdService.create(bob, new HouseholdCreateRequest("Bob Haus", null)).id();

        assertThatThrownBy(() -> recipeService.create(alice, new RecipeCreateRequest(
            "Test", null, "Steps", 2, null, null,
            bobsHouse,
            List.of(new RecipeIngredientRequest(null, "Tomate", BigDecimal.ONE, Unit.PIECE, null)))))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @TestTransaction
    @DisplayName("list liefert eigene + Haushalts-Rezepte, nicht fremde")
    void listVisibility() {
        UUID alice = registerUser("alice@example.com", "Alice");
        UUID bob = registerUser("bob@example.com", "Bob");
        UUID house = householdService.create(alice, new HouseholdCreateRequest("Familie", null)).id();
        InvitationDto invitation = householdService.invite(alice, house,
            new InvitationCreateRequest("bob@example.com"));
        householdService.acceptInvitation(bob, invitation.token());

        // Alice: privat
        recipeService.create(alice, recipe("Privat-Alice", null));
        // Alice: in Haushalt
        recipeService.create(alice, recipe("Haus-Alice", house));
        // Bob: privat (Alice darf NICHT sehen)
        recipeService.create(bob, recipe("Privat-Bob", null));

        List<RecipeDto> aliceList = recipeService.list(alice, new RecipeFilter(null, null, null, false));

        assertThat(aliceList).extracting(RecipeDto::title)
            .containsExactlyInAnyOrder("Privat-Alice", "Haus-Alice");
    }

    @Test
    @TestTransaction
    @DisplayName("list filtert nach Diaet-Tags (AND-Match)")
    void listFilterByDietTags() {
        UUID alice = registerUser("alice@example.com", "Alice");

        recipeService.create(alice, recipeWithTags("Vegan-Recipe", List.of(DietTag.VEGAN, DietTag.VEGETARIAN)));
        recipeService.create(alice, recipeWithTags("Veggie-Recipe", List.of(DietTag.VEGETARIAN)));
        recipeService.create(alice, recipeWithTags("Plain-Recipe", List.of()));

        List<RecipeDto> hits = recipeService.list(alice,
            new RecipeFilter(null, List.of(DietTag.VEGAN), null, false));

        assertThat(hits).extracting(RecipeDto::title).containsExactly("Vegan-Recipe");
    }

    @Test
    @TestTransaction
    @DisplayName("list filtert nach Volltext-Query im Titel")
    void listFilterByQuery() {
        UUID alice = registerUser("alice@example.com", "Alice");
        recipeService.create(alice, recipe("Tomatensuppe", null));
        recipeService.create(alice, recipe("Linsensuppe", null));
        recipeService.create(alice, recipe("Pizza", null));

        List<RecipeDto> hits = recipeService.list(alice, new RecipeFilter("suppe", null, null, false));

        assertThat(hits).extracting(RecipeDto::title)
            .containsExactlyInAnyOrder("Tomatensuppe", "Linsensuppe");
    }

    @Test
    @TestTransaction
    @DisplayName("update als Owner aendert Zutaten")
    void updateAsOwner() {
        UUID alice = registerUser("alice@example.com", "Alice");
        RecipeDto created = recipeService.create(alice, recipe("Original", null));

        RecipeDto updated = recipeService.update(alice, created.id(), new RecipeUpdateRequest(
            "Geaendert", "Neue Beschreibung", "Neue Steps",
            6, 45, List.of(DietTag.GLUTEN_FREE), null,
            List.of(new RecipeIngredientRequest(null, "Mehl", new BigDecimal("250"), Unit.GRAM, null))));

        assertThat(updated.title()).isEqualTo("Geaendert");
        assertThat(updated.servings()).isEqualTo(6);
        assertThat(updated.ingredients()).hasSize(1);
        assertThat(updated.ingredients().get(0).ingredientName()).isEqualTo("Mehl");
    }

    @Test
    @TestTransaction
    @DisplayName("update als Nicht-Owner wirft Forbidden")
    void updateAsNonOwner() {
        UUID alice = registerUser("alice@example.com", "Alice");
        UUID bob = registerUser("bob@example.com", "Bob");
        RecipeDto created = recipeService.create(alice, recipe("Alice Recipe", null));

        assertThatThrownBy(() -> recipeService.update(bob, created.id(), new RecipeUpdateRequest(
            "Hijack", null, "Steps", 1, null, null, null,
            List.of(new RecipeIngredientRequest(null, "X", BigDecimal.ONE, Unit.PIECE, null)))))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @TestTransaction
    @DisplayName("delete als Owner loescht; als Nicht-Owner Forbidden")
    void deleteRules() {
        UUID alice = registerUser("alice@example.com", "Alice");
        UUID bob = registerUser("bob@example.com", "Bob");
        RecipeDto created = recipeService.create(alice, recipe("Recipe", null));

        assertThatThrownBy(() -> recipeService.delete(bob, created.id()))
            .isInstanceOf(ForbiddenException.class);

        recipeService.delete(alice, created.id());
        assertThat(recipeRepository.findByIdOptional(created.id())).isEmpty();
    }

    @Test
    @TestTransaction
    @DisplayName("create mit ingredientId nutzt existierende Zutat")
    void createWithExistingIngredientId() {
        UUID alice = registerUser("alice@example.com", "Alice");
        IngredientDto existing = ingredientService.findOrCreate("Tomate", Unit.PIECE);

        RecipeDto recipe = recipeService.create(alice, new RecipeCreateRequest(
            "Test", null, "Steps", 2, null, null, null,
            List.of(new RecipeIngredientRequest(existing.id(), null, BigDecimal.ONE, Unit.PIECE, null))));

        assertThat(recipe.ingredients()).hasSize(1);
        assertThat(recipe.ingredients().get(0).ingredientId()).isEqualTo(existing.id());
    }

    @Test
    @TestTransaction
    @DisplayName("setFavorite markiert Rezept; get und list tragen das Flag")
    void setFavoriteMarksRecipe() {
        UUID alice = registerUser("alice@example.com", "Alice");
        RecipeDto created = recipeService.create(alice, recipe("Lieblingsrezept", null));
        assertThat(created.favorite()).isFalse();

        recipeService.setFavorite(alice, created.id(), true);

        assertThat(recipeService.get(alice, created.id()).favorite()).isTrue();
        List<RecipeDto> all = recipeService.list(alice, new RecipeFilter(null, null, null, false));
        assertThat(all).extracting(RecipeDto::favorite).containsExactly(true);
    }

    @Test
    @TestTransaction
    @DisplayName("setFavorite ist idempotent und laesst sich zuruecknehmen")
    void setFavoriteIdempotentAndRemovable() {
        UUID alice = registerUser("alice@example.com", "Alice");
        RecipeDto created = recipeService.create(alice, recipe("Rezept", null));

        recipeService.setFavorite(alice, created.id(), true);
        recipeService.setFavorite(alice, created.id(), true);
        assertThat(recipeService.get(alice, created.id()).favorite()).isTrue();

        recipeService.setFavorite(alice, created.id(), false);
        recipeService.setFavorite(alice, created.id(), false);
        assertThat(recipeService.get(alice, created.id()).favorite()).isFalse();
    }

    @Test
    @TestTransaction
    @DisplayName("insertIfAbsent ist gegen die Unique-Constraint-Race idempotent (kein 500)")
    void insertIfAbsentIsIdempotentAgainstUniqueConstraint() {
        UUID alice = registerUser("alice@example.com", "Alice");
        RecipeDto created = recipeService.create(alice, recipe("Rezept", null));

        // Simuliert die TOCTOU-Race: zwei parallele Requests sehen beide "kein Favorit"
        // und inserten. Der Upsert (ON CONFLICT DO NOTHING) darf beim zweiten Aufruf
        // NICHT die Unique-Constraint verletzen (sonst unbehandelter 500).
        boolean firstInserted = favoriteRepository.insertIfAbsent(alice, created.id());
        boolean secondInserted = favoriteRepository.insertIfAbsent(alice, created.id());

        assertThat(firstInserted).isTrue();
        assertThat(secondInserted).isFalse();
        assertThat(favoriteRepository.findRecipeIdsByUser(alice)).containsExactly(created.id());
        assertThat(recipeService.get(alice, created.id()).favorite()).isTrue();
    }

    @Test
    @TestTransaction
    @DisplayName("list mit favoritesOnly liefert nur Favoriten; Favoriten sind pro User")
    void listFavoritesOnlyPerUser() {
        UUID alice = registerUser("alice@example.com", "Alice");
        UUID bob = registerUser("bob@example.com", "Bob");
        UUID house = householdService.create(alice, new HouseholdCreateRequest("Familie", null)).id();
        InvitationDto invitation = householdService.invite(alice, house,
            new InvitationCreateRequest("bob@example.com"));
        householdService.acceptInvitation(bob, invitation.token());

        RecipeDto fav = recipeService.create(alice, recipe("Favorit", house));
        recipeService.create(alice, recipe("Kein Favorit", house));
        recipeService.setFavorite(alice, fav.id(), true);

        List<RecipeDto> aliceFavs = recipeService.list(alice, new RecipeFilter(null, null, null, true));
        assertThat(aliceFavs).extracting(RecipeDto::title).containsExactly("Favorit");

        // Bob sieht dasselbe Haushaltsrezept, hat aber selbst keinen Favoriten.
        List<RecipeDto> bobFavs = recipeService.list(bob, new RecipeFilter(null, null, null, true));
        assertThat(bobFavs).isEmpty();
    }

    @Test
    @TestTransaction
    @DisplayName("setFavorite auf fremdes privates Rezept wirft Forbidden")
    void setFavoriteForbiddenForForeignRecipe() {
        UUID alice = registerUser("alice@example.com", "Alice");
        UUID bob = registerUser("bob@example.com", "Bob");
        RecipeDto bobsPrivate = recipeService.create(bob, recipe("Privat-Bob", null));

        assertThatThrownBy(() -> recipeService.setFavorite(alice, bobsPrivate.id(), true))
            .isInstanceOf(ForbiddenException.class);
    }

    // --- Helpers ---------------------------------------------------------

    private UUID registerUser(String email, String displayName) {
        AuthResponse response = authService.register(
            new RegisterRequest(email, "secret12", displayName));
        return response.user().id();
    }

    private static RecipeCreateRequest recipe(String title, UUID householdId) {
        return new RecipeCreateRequest(
            title, "Beschreibung", "Steps", 2, 15, null, householdId,
            List.of(new RecipeIngredientRequest(null, "Salz", new BigDecimal("5"), Unit.GRAM, null)));
    }

    private static RecipeCreateRequest recipeWithTags(String title, List<String> tags) {
        return new RecipeCreateRequest(
            title, "Beschreibung", "Steps", 2, 15, tags, null,
            List.of(new RecipeIngredientRequest(null, "Salz", new BigDecimal("5"), Unit.GRAM, null)));
    }
}
