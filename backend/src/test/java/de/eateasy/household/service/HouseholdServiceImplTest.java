package de.eateasy.household.service;

import de.eateasy.auth.dto.AuthResponse;
import de.eateasy.auth.dto.RegisterRequest;
import de.eateasy.auth.repository.UserRepository;
import de.eateasy.auth.service.AuthService;
import de.eateasy.common.diet.DietTag;
import de.eateasy.common.exception.BadRequestException;
import de.eateasy.common.exception.ConflictException;
import de.eateasy.common.exception.ForbiddenException;
import de.eateasy.household.dto.HouseholdCreateRequest;
import de.eateasy.household.dto.HouseholdDto;
import de.eateasy.household.dto.HouseholdUpdateRequest;
import de.eateasy.household.dto.InvitationCreateRequest;
import de.eateasy.household.dto.InvitationDto;
import de.eateasy.household.dto.MemberDto;
import de.eateasy.household.entity.MembershipRole;
import de.eateasy.household.repository.HouseholdInvitationRepository;
import de.eateasy.household.repository.HouseholdMembershipRepository;
import de.eateasy.household.repository.HouseholdRepository;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@QuarkusTest
class HouseholdServiceImplTest {

    @Inject
    HouseholdService householdService;

    @Inject
    AuthService authService;

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
        invitationRepository.deleteAll();
        membershipRepository.deleteAll();
        householdRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @TestTransaction
    @DisplayName("create legt Haushalt an und macht Aufrufer zum OWNER")
    void createHappyPath() {
        UUID ownerId = registerUser("owner@example.com", "Owner");

        HouseholdDto household = householdService.create(ownerId,
            new HouseholdCreateRequest("Familie Mustermann", List.of(DietTag.VEGETARIAN)));

        assertThat(household.id()).isNotNull();
        assertThat(household.name()).isEqualTo("Familie Mustermann");
        assertThat(household.role()).isEqualTo(MembershipRole.OWNER);
        assertThat(household.defaultDietTags()).containsExactly(DietTag.VEGETARIAN);
    }

    @Test
    @TestTransaction
    @DisplayName("create lehnt unbekannte Diaet-Tags ab")
    void createRejectsUnknownDietTag() {
        UUID ownerId = registerUser("owner@example.com", "Owner");

        assertThatThrownBy(() -> householdService.create(ownerId,
            new HouseholdCreateRequest("Test", List.of("paleo"))))
            .isInstanceOf(BadRequestException.class);
    }

    @Test
    @TestTransaction
    @DisplayName("listForUser liefert alle Haushalte des Users")
    void listForUserReturnsMemberships() {
        UUID ownerId = registerUser("owner@example.com", "Owner");
        householdService.create(ownerId, new HouseholdCreateRequest("Haus 1", null));
        householdService.create(ownerId, new HouseholdCreateRequest("Haus 2", null));

        List<HouseholdDto> households = householdService.listForUser(ownerId);

        assertThat(households).hasSize(2);
        assertThat(households).allMatch(h -> h.role() == MembershipRole.OWNER);
    }

    @Test
    @TestTransaction
    @DisplayName("get wirft Forbidden bei fehlender Mitgliedschaft")
    void getForbiddenForNonMember() {
        UUID ownerId = registerUser("owner@example.com", "Owner");
        UUID strangerId = registerUser("stranger@example.com", "Stranger");
        UUID householdId = householdService.create(ownerId,
            new HouseholdCreateRequest("Privat", null)).id();

        assertThatThrownBy(() -> householdService.get(strangerId, householdId))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @TestTransaction
    @DisplayName("update als Owner aendert Name und Tags")
    void updateAsOwnerSucceeds() {
        UUID ownerId = registerUser("owner@example.com", "Owner");
        UUID householdId = householdService.create(ownerId,
            new HouseholdCreateRequest("Alt", null)).id();

        HouseholdDto updated = householdService.update(ownerId, householdId,
            new HouseholdUpdateRequest("Neu", List.of(DietTag.VEGAN, DietTag.GLUTEN_FREE)));

        assertThat(updated.name()).isEqualTo("Neu");
        assertThat(updated.defaultDietTags()).containsExactlyInAnyOrder(DietTag.VEGAN, DietTag.GLUTEN_FREE);
    }

    @Test
    @TestTransaction
    @DisplayName("update als Member wirft Forbidden")
    void updateAsMemberForbidden() {
        UUID ownerId = registerUser("owner@example.com", "Owner");
        UUID memberId = registerUser("member@example.com", "Member");
        UUID householdId = householdService.create(ownerId,
            new HouseholdCreateRequest("Test", null)).id();
        InvitationDto invitation = householdService.invite(ownerId, householdId,
            new InvitationCreateRequest("member@example.com"));
        householdService.acceptInvitation(memberId, invitation.token());

        assertThatThrownBy(() -> householdService.update(memberId, householdId,
            new HouseholdUpdateRequest("Hijack", null)))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @TestTransaction
    @DisplayName("invite erzeugt Token mit 7 Tagen Gueltigkeit")
    void inviteCreatesToken() {
        UUID ownerId = registerUser("owner@example.com", "Owner");
        UUID householdId = householdService.create(ownerId,
            new HouseholdCreateRequest("Test", null)).id();

        InvitationDto invitation = householdService.invite(ownerId, householdId,
            new InvitationCreateRequest("guest@example.com"));

        assertThat(invitation.token()).hasSize(32);
        assertThat(invitation.email()).isEqualTo("guest@example.com");
        assertThat(invitation.expiresAt()).isAfter(invitation.createdAt());
        assertThat(invitation.householdName()).isEqualTo("Test");
    }

    @Test
    @TestTransaction
    @DisplayName("invite blockiert bereits eingetragene Mitglieder")
    void inviteRejectsExistingMember() {
        UUID ownerId = registerUser("owner@example.com", "Owner");
        UUID memberId = registerUser("member@example.com", "Member");
        UUID householdId = householdService.create(ownerId,
            new HouseholdCreateRequest("Test", null)).id();
        InvitationDto invitation = householdService.invite(ownerId, householdId,
            new InvitationCreateRequest("member@example.com"));
        householdService.acceptInvitation(memberId, invitation.token());

        assertThatThrownBy(() -> householdService.invite(ownerId, householdId,
            new InvitationCreateRequest("member@example.com")))
            .isInstanceOf(ConflictException.class);
    }

    @Test
    @TestTransaction
    @DisplayName("acceptInvitation legt Membership an und markiert Token verbraucht")
    void acceptInvitationHappyPath() {
        UUID ownerId = registerUser("owner@example.com", "Owner");
        UUID guestId = registerUser("guest@example.com", "Guest");
        UUID householdId = householdService.create(ownerId,
            new HouseholdCreateRequest("Test", null)).id();
        InvitationDto invitation = householdService.invite(ownerId, householdId,
            new InvitationCreateRequest("guest@example.com"));

        HouseholdDto joined = householdService.acceptInvitation(guestId, invitation.token());

        assertThat(joined.id()).isEqualTo(householdId);
        assertThat(joined.role()).isEqualTo(MembershipRole.MEMBER);
        assertThat(membershipRepository.existsByUserAndHousehold(guestId, householdId)).isTrue();
        assertThat(invitationRepository.findByToken(invitation.token()).orElseThrow().getAcceptedAt())
            .isNotNull();
    }

    @Test
    @TestTransaction
    @DisplayName("acceptInvitation lehnt fremde Email ab")
    void acceptInvitationWrongEmail() {
        UUID ownerId = registerUser("owner@example.com", "Owner");
        UUID otherId = registerUser("other@example.com", "Other");
        UUID householdId = householdService.create(ownerId,
            new HouseholdCreateRequest("Test", null)).id();
        InvitationDto invitation = householdService.invite(ownerId, householdId,
            new InvitationCreateRequest("guest@example.com"));

        assertThatThrownBy(() -> householdService.acceptInvitation(otherId, invitation.token()))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @TestTransaction
    @DisplayName("acceptInvitation lehnt zweite Einloesung ab")
    void acceptInvitationTwiceRejected() {
        UUID ownerId = registerUser("owner@example.com", "Owner");
        UUID guestId = registerUser("guest@example.com", "Guest");
        UUID householdId = householdService.create(ownerId,
            new HouseholdCreateRequest("Test", null)).id();
        InvitationDto invitation = householdService.invite(ownerId, householdId,
            new InvitationCreateRequest("guest@example.com"));
        householdService.acceptInvitation(guestId, invitation.token());

        assertThatThrownBy(() -> householdService.acceptInvitation(guestId, invitation.token()))
            .isInstanceOf(ConflictException.class);
    }

    @Test
    @TestTransaction
    @DisplayName("listMembers liefert User-Infos in MemberDto")
    void listMembersReturnsUserInfo() {
        UUID ownerId = registerUser("owner@example.com", "Owner");
        UUID memberId = registerUser("member@example.com", "Member");
        UUID householdId = householdService.create(ownerId,
            new HouseholdCreateRequest("Test", null)).id();
        InvitationDto invitation = householdService.invite(ownerId, householdId,
            new InvitationCreateRequest("member@example.com"));
        householdService.acceptInvitation(memberId, invitation.token());

        List<MemberDto> members = householdService.listMembers(ownerId, householdId);

        assertThat(members).extracting(MemberDto::email)
            .containsExactlyInAnyOrder("owner@example.com", "member@example.com");
        assertThat(members).extracting(MemberDto::role)
            .containsExactlyInAnyOrder(MembershipRole.OWNER, MembershipRole.MEMBER);
    }

    @Test
    @TestTransaction
    @DisplayName("removeMember loescht Membership; Owner kann sich nicht selbst entfernen")
    void removeMemberRules() {
        UUID ownerId = registerUser("owner@example.com", "Owner");
        UUID memberId = registerUser("member@example.com", "Member");
        UUID householdId = householdService.create(ownerId,
            new HouseholdCreateRequest("Test", null)).id();
        InvitationDto invitation = householdService.invite(ownerId, householdId,
            new InvitationCreateRequest("member@example.com"));
        householdService.acceptInvitation(memberId, invitation.token());

        householdService.removeMember(ownerId, householdId, memberId);
        assertThat(membershipRepository.existsByUserAndHousehold(memberId, householdId)).isFalse();

        assertThatThrownBy(() -> householdService.removeMember(ownerId, householdId, ownerId))
            .isInstanceOf(BadRequestException.class);
    }

    private UUID registerUser(String email, String displayName) {
        AuthResponse response = authService.register(
            new RegisterRequest(email, "secret12", displayName));
        return response.user().id();
    }
}
