export const DIET_TAGS = [
  'vegan',
  'vegetarian',
  'gluten_free',
  'halal',
  'low_carb',
  'dairy_free',
] as const

export type DietTag = (typeof DIET_TAGS)[number]

export const DIET_TAG_LABELS: Record<DietTag, string> = {
  vegan: 'Vegan',
  vegetarian: 'Vegetarisch',
  gluten_free: 'Glutenfrei',
  halal: 'Halal',
  low_carb: 'Low Carb',
  dairy_free: 'Laktosefrei',
}
