package de.eateasy.ingredient.service;

import de.eateasy.common.exception.BadRequestException;
import de.eateasy.common.exception.NotFoundException;
import de.eateasy.common.units.Unit;
import de.eateasy.ingredient.dto.IngredientDto;
import de.eateasy.ingredient.entity.Ingredient;
import de.eateasy.ingredient.entity.IngredientCategory;
import de.eateasy.ingredient.repository.IngredientRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class IngredientServiceImpl implements IngredientService {

    private static final int DEFAULT_SEARCH_LIMIT = 20;
    private static final int MAX_SEARCH_LIMIT = 100;

    private final IngredientRepository ingredientRepository;

    public IngredientServiceImpl(IngredientRepository ingredientRepository) {
        this.ingredientRepository = ingredientRepository;
    }

    @Override
    @Transactional
    public IngredientDto findOrCreate(String name, Unit defaultUnit) {
        String normalized = name == null ? "" : name.trim();
        return ingredientRepository.findByNameIgnoreCase(normalized)
            .map(IngredientDto::from)
            .orElseGet(() -> {
                Ingredient created = new Ingredient(normalized, defaultUnit);
                ingredientRepository.persist(created);
                return IngredientDto.from(created);
            });
    }

    @Override
    public List<IngredientDto> search(String query, int limit) {
        int effectiveLimit = limit <= 0 ? DEFAULT_SEARCH_LIMIT : Math.min(limit, MAX_SEARCH_LIMIT);
        return ingredientRepository.search(query, effectiveLimit).stream()
            .map(IngredientDto::from)
            .toList();
    }

    @Override
    public IngredientDto getById(UUID id) {
        return ingredientRepository.findByIdOptional(id)
            .map(IngredientDto::from)
            .orElseThrow(() -> new NotFoundException("Zutat nicht gefunden: " + id));
    }

    @Override
    public Map<UUID, IngredientDto> getByIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return ingredientRepository.findByIds(ids).stream()
            .map(IngredientDto::from)
            .collect(Collectors.toMap(IngredientDto::id, Function.identity()));
    }

    @Override
    @Transactional
    public IngredientDto updateCategory(UUID id, IngredientCategory category) {
        Ingredient ingredient = ingredientRepository.findByIdOptional(id)
            .orElseThrow(() -> new NotFoundException("Zutat nicht gefunden: " + id));
        ingredient.setCategory(category);
        return IngredientDto.from(ingredient);
    }

    @Override
    @Transactional
    public UUID resolveOrCreate(UUID id, String name, Unit defaultUnit) {
        if (id != null) {
            // Validate existence — throws NotFoundException bei unbekannter ID.
            getById(id);
            return id;
        }
        if (name == null || name.isBlank()) {
            throw new BadRequestException("ingredientId oder ingredientName muss gesetzt sein");
        }
        return findOrCreate(name, defaultUnit).id();
    }
}
