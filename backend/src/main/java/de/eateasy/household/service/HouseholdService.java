package de.eateasy.household.service;

import de.eateasy.common.exception.ForbiddenException;
import de.eateasy.household.dto.HouseholdCreateRequest;
import de.eateasy.household.dto.HouseholdDto;
import de.eateasy.household.dto.HouseholdUpdateRequest;
import de.eateasy.household.dto.InvitationCreateRequest;
import de.eateasy.household.dto.InvitationDto;
import de.eateasy.household.dto.MemberDto;

import java.util.List;
import java.util.UUID;

public interface HouseholdService {

    /** Legt einen neuen Haushalt an, der Aufrufer wird automatisch OWNER. */
    HouseholdDto create(UUID userId, HouseholdCreateRequest request);

    /** Liefert alle Haushalte, in denen der User Mitglied ist. */
    List<HouseholdDto> listForUser(UUID userId);

    /** Liefert einen einzelnen Haushalt; 403 wenn der User kein Mitglied ist. */
    HouseholdDto get(UUID userId, UUID householdId);

    /** Aendert Name oder default_diet_tags; nur OWNER. */
    HouseholdDto update(UUID userId, UUID householdId, HouseholdUpdateRequest request);

    /** Erstellt Einladungs-Token; nur OWNER. */
    InvitationDto invite(UUID userId, UUID householdId, InvitationCreateRequest request);

    /** Loest einen Token ein und macht den Aufrufer zum MEMBER. */
    HouseholdDto acceptInvitation(UUID userId, String token);

    /** Liefert alle Mitglieder; jedes Mitglied darf die Liste sehen. */
    List<MemberDto> listMembers(UUID userId, UUID householdId);

    /** Entfernt ein Mitglied; nur OWNER, nicht sich selbst. */
    void removeMember(UUID userId, UUID householdId, UUID memberId);

    /**
     * Pruefe stillschweigend, ob der User Mitglied des Haushalts ist. Wirft
     * keine Exception — fuer Auth-Checks in anderen Komponenten, die selbst
     * entscheiden, was bei {@code false} passiert.
     */
    boolean isMember(UUID userId, UUID householdId);

    /**
     * Liefert die IDs aller Haushalte, in denen der User Mitglied ist.
     * Wird z. B. von {@code RecipeService} fuer Sichtbarkeits-Queries genutzt.
     */
    List<UUID> listHouseholdIdsForUser(UUID userId);

    /**
     * Ob fuer den Haushalt das Auto-Nachbuchen aktiv ist. Ohne Auth-Check —
     * der Aufrufer (z. B. {@code ShoppingListService.toggleChecked}) sichert den
     * Kontext selbst ab, analog zu {@code PantryService.getInventory}.
     */
    boolean isAutoRestockEnabled(UUID householdId);

    /**
     * Hartes Guard: wirft {@link ForbiddenException}, wenn der User nicht
     * Mitglied des angegebenen Haushalts ist. Vereinheitlicht das Pattern, das
     * sonst in jeder fremden Komponente per Hand wiederholt wuerde.
     */
    default void assertMember(UUID userId, UUID householdId) {
        if (!isMember(userId, householdId)) {
            throw new ForbiddenException("Kein Zugriff auf diesen Haushalt");
        }
    }
}
