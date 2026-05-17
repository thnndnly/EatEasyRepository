package de.eateasy.integration.service;

import de.eateasy.auth.dto.AuthResponse;
import de.eateasy.auth.dto.RegisterRequest;
import de.eateasy.auth.repository.UserRepository;
import de.eateasy.auth.service.AuthService;
import de.eateasy.common.exception.BadRequestException;
import de.eateasy.common.exception.NotFoundException;
import de.eateasy.household.repository.HouseholdInvitationRepository;
import de.eateasy.household.repository.HouseholdMembershipRepository;
import de.eateasy.household.repository.HouseholdRepository;
import de.eateasy.integration.client.TheMealDbClient;
import de.eateasy.integration.client.TheMealDbResponse;
import de.eateasy.integration.client.TheMealDbResponse.TheMealDbMeal;
import de.eateasy.integration.dto.ExternalRecipePreviewDto;
import de.eateasy.integration.dto.RecipeImportRequest;
import de.eateasy.ingredient.repository.IngredientRepository;
import de.eateasy.recipe.dto.RecipeDto;
import de.eateasy.recipe.repository.RecipeRepository;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
class RecipeImportServiceImplTest {

    @Inject
    RecipeImportService importService;

    @Inject
    AuthService authService;

    @InjectMock
    @RestClient
    TheMealDbClient theMealDbClient;

    @Inject
    RecipeRepository recipeRepository;

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
    @DisplayName("search liefert Previews aus TheMealDB-Response")
    void searchReturnsPreviews() {
        when(theMealDbClient.search("pasta")).thenReturn(
            new TheMealDbResponse(List.of(stubMeal("1", "Spaghetti", "Pasta", "Italian"))));

        List<ExternalRecipePreviewDto> previews = importService.search("themealdb", "pasta");

        assertThat(previews).hasSize(1);
        assertThat(previews.get(0).title()).isEqualTo("Spaghetti");
        assertThat(previews.get(0).source()).isEqualTo("themealdb");
        assertThat(previews.get(0).externalId()).isEqualTo("1");
    }

    @Test
    @DisplayName("search bei keinem Treffer liefert leere Liste")
    void searchEmpty() {
        when(theMealDbClient.search(any())).thenReturn(new TheMealDbResponse(null));

        List<ExternalRecipePreviewDto> previews = importService.search("themealdb", "xyz");

        assertThat(previews).isEmpty();
    }

    @Test
    @DisplayName("search lehnt unbekannte Quelle ab")
    void searchRejectsUnknownSource() {
        assertThatThrownBy(() -> importService.search("spoonacular", "x"))
            .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("importRecipe legt Recipe an und setzt source_url + external_source")
    void importCreatesRecipe() {
        UUID userId = registerUser("alice@example.com");
        when(theMealDbClient.lookup("52772")).thenReturn(
            new TheMealDbResponse(List.of(stubMeal("52772", "Teriyaki Chicken Casserole",
                "Chicken", "Japanese",
                "Steps here.",
                "https://example.com/recipe",
                "soy sauce", "1/2 cup",
                "rice", "200g"))));

        RecipeDto recipe = importService.importRecipe(userId,
            new RecipeImportRequest("themealdb", "52772", null));

        assertThat(recipe.title()).isEqualTo("Teriyaki Chicken Casserole");
        assertThat(recipe.externalSource()).isEqualTo("themealdb");
        assertThat(recipe.sourceUrl()).isEqualTo("https://example.com/recipe");
        assertThat(recipe.ingredients()).hasSize(2);
    }

    @Test
    @DisplayName("importRecipe mit unbekannter externalId wirft NotFound")
    void importNotFound() {
        UUID userId = registerUser("alice@example.com");
        when(theMealDbClient.lookup("999")).thenReturn(new TheMealDbResponse(null));

        assertThatThrownBy(() -> importService.importRecipe(userId,
            new RecipeImportRequest("themealdb", "999", null)))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("importRecipe ohne Source-URL nutzt themealdb-Detail-Fallback")
    void importFallsBackToThemealdbUrl() {
        UUID userId = registerUser("alice@example.com");
        when(theMealDbClient.lookup("123")).thenReturn(
            new TheMealDbResponse(List.of(stubMeal("123", "Foo", "Other", "Local",
                "Bake it.", null, "salt", "1g"))));

        RecipeDto recipe = importService.importRecipe(userId,
            new RecipeImportRequest("themealdb", "123", null));

        assertThat(recipe.sourceUrl()).isEqualTo("https://www.themealdb.com/meal/123");
    }

    // --- Helpers ---------------------------------------------------------

    private UUID registerUser(String email) {
        AuthResponse response = authService.register(
            new RegisterRequest(email, "secret12", email));
        return response.user().id();
    }

    /** Erzeugt einen Meal mit Standard-Source-URL. */
    private static TheMealDbMeal stubMeal(String id, String title, String category, String area) {
        return stubMeal(id, title, category, area, "Steps.", null, "salt", "1g");
    }

    private static TheMealDbMeal stubMeal(String id, String title, String category, String area,
                                          String instructions, String sourceUrl,
                                          String... ingredientPairs) {
        String[] ing = new String[20];
        String[] meas = new String[20];
        for (int i = 0; i < ingredientPairs.length / 2 && i < 20; i++) {
            ing[i] = ingredientPairs[i * 2];
            meas[i] = ingredientPairs[i * 2 + 1];
        }
        return new TheMealDbMeal(
            id, title, category, area, instructions, null, sourceUrl,
            ing[0], ing[1], ing[2], ing[3], ing[4], ing[5], ing[6], ing[7], ing[8], ing[9],
            ing[10], ing[11], ing[12], ing[13], ing[14], ing[15], ing[16], ing[17], ing[18], ing[19],
            meas[0], meas[1], meas[2], meas[3], meas[4], meas[5], meas[6], meas[7], meas[8], meas[9],
            meas[10], meas[11], meas[12], meas[13], meas[14], meas[15], meas[16], meas[17], meas[18], meas[19]);
    }
}
