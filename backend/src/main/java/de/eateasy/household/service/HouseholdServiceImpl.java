package de.eateasy.household.service;

import de.eateasy.auth.dto.UserDto;
import de.eateasy.auth.service.AuthService;
import de.eateasy.common.diet.DietTag;
import de.eateasy.common.exception.BadRequestException;
import de.eateasy.common.exception.ConflictException;
import de.eateasy.common.exception.ForbiddenException;
import de.eateasy.common.exception.NotFoundException;
import de.eateasy.household.dto.HouseholdCreateRequest;
import de.eateasy.household.dto.HouseholdDto;
import de.eateasy.household.dto.HouseholdUpdateRequest;
import de.eateasy.household.dto.InvitationCreateRequest;
import de.eateasy.household.dto.InvitationDto;
import de.eateasy.household.dto.MemberDto;
import de.eateasy.household.entity.Household;
import de.eateasy.household.entity.HouseholdInvitation;
import de.eateasy.household.entity.HouseholdMembership;
import de.eateasy.household.entity.MembershipRole;
import de.eateasy.household.repository.HouseholdInvitationRepository;
import de.eateasy.household.repository.HouseholdMembershipRepository;
import de.eateasy.household.repository.HouseholdRepository;
import de.eateasy.notification.service.NotificationService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class HouseholdServiceImpl implements HouseholdService {

    private static final Duration INVITATION_TTL = Duration.ofDays(7);
    private static final int TOKEN_BYTES = 24; // 24 Bytes → 32 Zeichen Base64URL

    private final HouseholdRepository householdRepository;
    private final HouseholdMembershipRepository membershipRepository;
    private final HouseholdInvitationRepository invitationRepository;
    private final AuthService authService;
    private final NotificationService notificationService;
    private final SecureRandom random = new SecureRandom();

    public HouseholdServiceImpl(HouseholdRepository householdRepository,
                                HouseholdMembershipRepository membershipRepository,
                                HouseholdInvitationRepository invitationRepository,
                                AuthService authService,
                                NotificationService notificationService) {
        this.householdRepository = householdRepository;
        this.membershipRepository = membershipRepository;
        this.invitationRepository = invitationRepository;
        this.authService = authService;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public HouseholdDto create(UUID userId, HouseholdCreateRequest request) {
        // Sanity-Check: Aufrufer muss als User existieren — sonst koennten wir
        // dangling Memberships anlegen, falls das JWT auf einen geloeschten
        // User zeigt.
        authService.getUser(userId);

        String[] tags = DietTag.validate(request.defaultDietTags());
        Household household = new Household(request.name().trim(), tags);
        householdRepository.persist(household);

        HouseholdMembership membership = new HouseholdMembership(userId, household.getId(), MembershipRole.OWNER);
        membershipRepository.persist(membership);

        return HouseholdDto.from(household, MembershipRole.OWNER);
    }

    @Override
    public List<HouseholdDto> listForUser(UUID userId) {
        return membershipRepository.findByUser(userId).stream()
            .map(m -> {
                Household household = householdRepository.findByIdOptional(m.getHouseholdId())
                    .orElseThrow(() -> new NotFoundException("Haushalt nicht gefunden: " + m.getHouseholdId()));
                return HouseholdDto.from(household, m.getRole());
            })
            .toList();
    }

    @Override
    public HouseholdDto get(UUID userId, UUID householdId) {
        HouseholdMembership membership = assertMembership(userId, householdId);
        Household household = loadHousehold(householdId);
        return HouseholdDto.from(household, membership.getRole());
    }

    @Override
    @Transactional
    public HouseholdDto update(UUID userId, UUID householdId, HouseholdUpdateRequest request) {
        assertOwner(userId, householdId);
        Household household = loadHousehold(householdId);

        if (request.name() != null && !request.name().isBlank()) {
            household.setName(request.name().trim());
        }
        if (request.defaultDietTags() != null) {
            household.setDefaultDietTags(DietTag.validate(request.defaultDietTags()));
        }

        return HouseholdDto.from(household, MembershipRole.OWNER);
    }

    @Override
    @Transactional
    public InvitationDto invite(UUID userId, UUID householdId, InvitationCreateRequest request) {
        assertOwner(userId, householdId);
        Household household = loadHousehold(householdId);
        UserDto inviter = authService.getUser(userId);

        String email = request.email().trim().toLowerCase();

        // Falls die Email schon einem registrierten User gehoert und der bereits
        // im Haushalt ist → Konflikt, sonst lohnt der Token nicht.
        boolean existingMember = authService.findByEmail(email)
            .map(existing -> membershipRepository.existsByUserAndHousehold(existing.id(), householdId))
            .orElse(false);
        if (existingMember) {
            throw new ConflictException("User ist bereits Mitglied dieses Haushalts");
        }

        String token = generateToken();
        Instant expiresAt = Instant.now().plus(INVITATION_TTL);
        HouseholdInvitation invitation = new HouseholdInvitation(householdId, email, token, expiresAt);
        invitationRepository.persist(invitation);

        // Mail-Versand — Fehler werden im Service geloggt, Invitation bleibt
        // unabhaengig davon persistiert (Token-Weitergabe als Fallback moeglich).
        notificationService.sendInvitation(email, household.getName(),
            inviter.displayName(), token);

        return InvitationDto.from(invitation, household.getName());
    }

    @Override
    @Transactional
    public HouseholdDto acceptInvitation(UUID userId, String token) {
        HouseholdInvitation invitation = invitationRepository.findByToken(token)
            .orElseThrow(() -> new NotFoundException("Einladung nicht gefunden"));

        Instant now = Instant.now();
        if (invitation.isAccepted()) {
            throw new ConflictException("Einladung wurde bereits eingeloest");
        }
        if (invitation.isExpired(now)) {
            throw new ConflictException("Einladung ist abgelaufen");
        }

        UserDto user = authService.getUser(userId);
        if (!user.email().equalsIgnoreCase(invitation.getEmail())) {
            throw new ForbiddenException("Diese Einladung ist fuer eine andere Email-Adresse");
        }

        if (membershipRepository.existsByUserAndHousehold(userId, invitation.getHouseholdId())) {
            invitation.markAccepted(now);
            Household household = loadHousehold(invitation.getHouseholdId());
            return HouseholdDto.from(household, MembershipRole.MEMBER);
        }

        HouseholdMembership membership = new HouseholdMembership(
            userId, invitation.getHouseholdId(), MembershipRole.MEMBER);
        membershipRepository.persist(membership);
        invitation.markAccepted(now);

        Household household = loadHousehold(invitation.getHouseholdId());
        return HouseholdDto.from(household, MembershipRole.MEMBER);
    }

    @Override
    public List<MemberDto> listMembers(UUID userId, UUID householdId) {
        assertMembership(userId, householdId);
        return membershipRepository.findByHousehold(householdId).stream()
            .map(m -> {
                UserDto user = authService.getUser(m.getUserId());
                return new MemberDto(
                    user.id(),
                    user.email(),
                    user.displayName(),
                    m.getRole(),
                    m.getJoinedAt());
            })
            .toList();
    }

    @Override
    @Transactional
    public void removeMember(UUID userId, UUID householdId, UUID memberId) {
        assertOwner(userId, householdId);
        if (userId.equals(memberId)) {
            throw new BadRequestException("Owner kann sich nicht selbst entfernen");
        }

        HouseholdMembership membership = membershipRepository
            .findByUserAndHousehold(memberId, householdId)
            .orElseThrow(() -> new NotFoundException("Mitglied nicht im Haushalt"));

        membershipRepository.delete(membership);
    }

    @Override
    public boolean isMember(UUID userId, UUID householdId) {
        return membershipRepository.existsByUserAndHousehold(userId, householdId);
    }

    @Override
    public List<UUID> listHouseholdIdsForUser(UUID userId) {
        return membershipRepository.findByUser(userId).stream()
            .map(HouseholdMembership::getHouseholdId)
            .toList();
    }

    // --- Helpers ---------------------------------------------------------

    private HouseholdMembership assertMembership(UUID userId, UUID householdId) {
        return membershipRepository.findByUserAndHousehold(userId, householdId)
            .orElseThrow(() -> new ForbiddenException("Kein Zugriff auf diesen Haushalt"));
    }

    private void assertOwner(UUID userId, UUID householdId) {
        HouseholdMembership membership = assertMembership(userId, householdId);
        if (membership.getRole() != MembershipRole.OWNER) {
            throw new ForbiddenException("Nur der Owner darf diese Aktion ausfuehren");
        }
    }

    private Household loadHousehold(UUID householdId) {
        return householdRepository.findByIdOptional(householdId)
            .orElseThrow(() -> new NotFoundException("Haushalt nicht gefunden: " + householdId));
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
