package de.eateasy.household.service;

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
}
