export interface ExternalRecipePreviewDto {
  source: string
  externalId: string
  title: string
  thumbnailUrl: string | null
  category: string | null
  area: string | null
}

export interface RecipeImportRequest {
  source: string
  externalId: string
  householdId?: string | null
}
