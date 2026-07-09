import type { UserDto } from '@/types/auth'
import type { HouseholdDto, InvitationDto, MemberDto } from '@/types/household'
import type { IngredientDto } from '@/types/ingredient'
import type { MealPlanDto } from '@/types/mealplan'
import type { PantryItemDto } from '@/types/pantry'
import type { RecipeDto, RecipeMiniDto } from '@/types/recipe'
import type { ShoppingListDto } from '@/types/shoppingList'
import type { SuggestionDto } from '@/types/suggestion'
import type { ExternalRecipePreviewDto } from '@/types/externalRecipe'

/**
 * Gemeinsame Test-Fixtures für MSW (Vitest) und Playwright. Bewusst hier
 * statt in mocks/ abgelegt, damit beide Test-Layer ohne Umweg importieren
 * können (Playwright via relativem Pfad, Vitest via @-Alias).
 *
 * Konvention: TEST_* sind Default-Fixtures. Tests, die abweichende Daten
 * brauchen, spreaden über das passende Default-Objekt (`{ ...TEST_RECIPE,
 * title: '...' }`).
 */
export const TEST_USER: UserDto = {
  id: '11111111-1111-1111-1111-111111111111',
  email: 'test@eateasy.local',
  displayName: 'Test User',
  createdAt: '2026-01-01T00:00:00Z',
}

export const TEST_TOKEN = 'test.fake.jwt.token'

export const TEST_HOUSEHOLD: HouseholdDto = {
  id: 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
  name: 'Test-Haushalt',
  defaultDietTags: [],
  autoRestockEnabled: true,
  role: 'OWNER',
  createdAt: '2026-01-01T00:00:00Z',
}

export const TEST_MEMBER: MemberDto = {
  userId: TEST_USER.id,
  email: TEST_USER.email,
  displayName: TEST_USER.displayName,
  role: 'OWNER',
  joinedAt: '2026-01-01T00:00:00Z',
}

export const TEST_INVITATION: InvitationDto = {
  id: 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
  householdId: TEST_HOUSEHOLD.id,
  householdName: TEST_HOUSEHOLD.name,
  email: 'guest@eateasy.local',
  token: 'inv-token-1',
  expiresAt: '2026-12-31T00:00:00Z',
  createdAt: '2026-01-01T00:00:00Z',
}

export const TEST_INGREDIENT: IngredientDto = {
  id: 'cccccccc-cccc-cccc-cccc-cccccccccccc',
  name: 'Tomate',
  defaultUnit: 'PIECE',
  category: 'OBST_GEMUESE',
}

export const TEST_RECIPE_MINI: RecipeMiniDto = {
  id: 'dddddddd-dddd-dddd-dddd-dddddddddddd',
  title: 'Tomatensuppe',
  servings: 4,
  prepMinutes: 30,
  dietTags: [],
}

export const TEST_RECIPE: RecipeDto = {
  id: TEST_RECIPE_MINI.id,
  ownerId: TEST_USER.id,
  householdId: TEST_HOUSEHOLD.id,
  title: TEST_RECIPE_MINI.title,
  description: 'Klassisch und einfach.',
  instructions: 'Tomaten schneiden, kochen, pürieren.',
  servings: TEST_RECIPE_MINI.servings,
  prepMinutes: TEST_RECIPE_MINI.prepMinutes,
  dietTags: [],
  sourceUrl: null,
  externalSource: null,
  favorite: false,
  ingredients: [
    {
      id: 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee',
      ingredientId: TEST_INGREDIENT.id,
      ingredientName: TEST_INGREDIENT.name,
      amount: 6,
      unit: 'PIECE',
      note: null,
    },
  ],
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
}

export const TEST_PANTRY_ITEM: PantryItemDto = {
  id: 'ffffffff-ffff-ffff-ffff-ffffffffffff',
  householdId: TEST_HOUSEHOLD.id,
  ingredientId: TEST_INGREDIENT.id,
  ingredientName: TEST_INGREDIENT.name,
  amount: 5,
  unit: 'PIECE',
  bestBefore: '2026-06-01',
}

export const TEST_MEAL_PLAN: MealPlanDto = {
  id: '99999999-9999-9999-9999-999999999999',
  householdId: TEST_HOUSEHOLD.id,
  weekStart: '2026-05-25',
  entries: [],
}

export const TEST_SHOPPING_LIST: ShoppingListDto = {
  id: '88888888-8888-8888-8888-888888888888',
  householdId: TEST_HOUSEHOLD.id,
  mealPlanId: TEST_MEAL_PLAN.id,
  items: [
    {
      id: 'item-1',
      ingredientId: TEST_INGREDIENT.id,
      ingredientName: TEST_INGREDIENT.name,
      category: TEST_INGREDIENT.category,
      amount: 6,
      unit: 'PIECE',
      checked: false,
    },
    {
      id: 'item-2',
      ingredientId: 'cccccccc-cccc-cccc-cccc-cccccccccccd',
      ingredientName: 'Zwiebel',
      category: 'SONSTIGES',
      amount: 2,
      unit: 'PIECE',
      checked: true,
    },
  ],
  updatedAt: '2026-01-01T00:00:00Z',
}

export const TEST_SUGGESTION: SuggestionDto = {
  recipe: TEST_RECIPE_MINI,
  reason: 'Aus deinem Vorrat machbar.',
  coverage: 0.8,
}

export const TEST_EXTERNAL_PREVIEW: ExternalRecipePreviewDto = {
  source: 'themealdb',
  externalId: 'ext-1',
  title: 'External Recipe',
  thumbnailUrl: null,
  category: null,
  area: null,
}
