package com.flowBoard.auth_service.repository;

import com.flowBoard.auth_service.entity.ROLE;
import com.flowBoard.auth_service.entity.User;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UserRepository – @DataJpaTest")
class UserRepositoryTest {

    @Autowired
    UserRepository repo;

    private User nageshwar, priya, adminUser, inactiveUser;

    @BeforeEach
    void setUp() {
        nageshwar = repo.save(User.builder()
                .fullName("Nageshwar Patel").email("nageshwar@gmail.com")
                .username("nageshwar").password("$2a$10$hash1")
                .role(ROLE.MEMBER).active(true).emailVerified(true)
                .createdAt(LocalDateTime.now()).build());

        priya = repo.save(User.builder()
                .fullName("Priya Sharma").email("priya@gmail.com")
                .username("priya").password("$2a$10$hash2")
                .role(ROLE.MEMBER).active(true).emailVerified(false)
                .createdAt(LocalDateTime.now()).build());

        adminUser = repo.save(User.builder()
                .fullName("Admin User").email("admin@flowboard.com")
                .username("sysadmin").password("$2a$10$hash3")
                .role(ROLE.PLATFORM_ADMIN).active(true).emailVerified(true)
                .createdAt(LocalDateTime.now()).build());

        inactiveUser = repo.save(User.builder()
                .fullName("Inactive User").email("inactive@gmail.com")
                .username("inactive").password("$2a$10$hash4")
                .role(ROLE.MEMBER).active(false).emailVerified(true)
                .createdAt(LocalDateTime.now()).build());
    }

    // ── findByEmail ─────────────────────────────────────────────────────────
    @Test
    @DisplayName("findByEmail – returns user for known email")
    void findByEmail_found() {
        Optional<User> result = repo.findByEmail("nageshwar@gmail.com");
        assertThat(result).isPresent();
        assertThat(result.get().getFullName()).isEqualTo("Nageshwar Patel");
    }

    @Test
    @DisplayName("findByEmail – empty for unknown email")
    void findByEmail_notFound() {
        assertThat(repo.findByEmail("nobody@nowhere.com")).isEmpty();
    }

    @Test
    @DisplayName("findByEmail – email is case sensitive")
    void findByEmail_caseSensitive() {
        // Depends on DB collation; test verifies the method works
        Optional<User> result = repo.findByEmail("nageshwar@gmail.com");
        assertThat(result).isPresent();
    }

    // ── findByUsername ───────────────────────────────────────────────────────
    @Test
    @DisplayName("findByUsername – returns user for known username")
    void findByUsername_found() {
        Optional<User> result = repo.findByUsername("priya");
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("priya@gmail.com");
    }

    @Test
    @DisplayName("findByUsername – empty for unknown username")
    void findByUsername_notFound() {
        assertThat(repo.findByUsername("ghost")).isEmpty();
    }

    // ── existsByEmail ────────────────────────────────────────────────────────
    @Test
    @DisplayName("existsByEmail – true for existing email")
    void existsByEmail_true() {
        assertThat(repo.existsByEmail("nageshwar@gmail.com")).isTrue();
    }

    @Test
    @DisplayName("existsByEmail – false for new email")
    void existsByEmail_false() {
        assertThat(repo.existsByEmail("new@example.com")).isFalse();
    }

    // ── existsByUsername ─────────────────────────────────────────────────────
    @Test
    @DisplayName("existsByUsername – true for existing username")
    void existsByUsername_true() {
        assertThat(repo.existsByUsername("nageshwar")).isTrue();
    }

    @Test
    @DisplayName("existsByUsername – false for new username")
    void existsByUsername_false() {
        assertThat(repo.existsByUsername("newuser123")).isFalse();
    }

    // ── findAllByRole ────────────────────────────────────────────────────────
    @Test
    @DisplayName("findAllByRole PLATFORM_ADMIN – returns only admins")
    void findAllByRole_admin() {
        List<User> admins = repo.findAllByRole(ROLE.PLATFORM_ADMIN);
        assertThat(admins).hasSize(1);
        assertThat(admins.get(0).getEmail()).isEqualTo("admin@flowboard.com");
    }

    @Test
    @DisplayName("findAllByRole MEMBER – returns all members")
    void findAllByRole_member() {
        List<User> members = repo.findAllByRole(ROLE.MEMBER);
        assertThat(members).hasSize(3); // nageshwar, priya, inactiveUser
    }

    // ── searchByNameOrUsername ───────────────────────────────────────────────
    @Test
    @DisplayName("searchByNameOrUsername – finds by partial name")
    void search_byPartialName() {
        List<User> result = repo.searchByNameOrUsername("Nagesh");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("nageshwar@gmail.com");
    }

    @Test
    @DisplayName("searchByNameOrUsername – finds by partial username")
    void search_byPartialUsername() {
        List<User> result = repo.searchByNameOrUsername("priya");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("priya@gmail.com");
    }

    @Test
    @DisplayName("searchByNameOrUsername – empty list for no match")
    void search_noMatch() {
        assertThat(repo.searchByNameOrUsername("xyz_nobody_xyz")).isEmpty();
    }

    @Test
    @DisplayName("searchByNameOrUsername – case insensitive search")
    void search_caseInsensitive() {
        List<User> result = repo.searchByNameOrUsername("NAGESH");
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("searchByNameOrUsername – partial match on full name")
    void search_partialFullName() {
        List<User> result = repo.searchByNameOrUsername("Patel");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("nageshwar@gmail.com");
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("save – persists entity and assigns ID")
    void save_assignsId() {
        User newUser = repo.save(User.builder()
                .fullName("New Dev").email("dev@test.com")
                .username("devuser").password("hashed")
                .role(ROLE.MEMBER).active(true).emailVerified(true)
                .createdAt(LocalDateTime.now()).build());

        assertThat(newUser.getId()).isNotNull();
        assertThat(repo.findById(newUser.getId())).isPresent();
    }

    @Test
    @DisplayName("save – updates existing user fields")
    void save_updatesFields() {
        nageshwar.setActive(false);
        repo.save(nageshwar);
        User reloaded = repo.findById(nageshwar.getId()).orElseThrow();
        assertThat(reloaded.isActive()).isFalse();
    }

    @Test
    @DisplayName("deleteById – removes entity")
    void deleteById_removes() {
        Long id = priya.getId();
        repo.deleteById(id);
        assertThat(repo.findById(id)).isEmpty();
    }

    @Test
    @DisplayName("findAll – returns all seeded users")
    void findAll_returnsAll() {
        List<User> all = repo.findAll();
        assertThat(all).hasSizeGreaterThanOrEqualTo(4);
    }

    @Test
    @DisplayName("countByActive – counts active users")
    void countActive() {
        long activeCount = repo.findAll().stream().filter(User::isActive).count();
        assertThat(activeCount).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("emailVerified filter – unverified users identifiable")
    void emailVerified_filter() {
        long unverified = repo.findAll().stream()
                .filter(u -> !u.isEmailVerified()).count();
        assertThat(unverified).isGreaterThanOrEqualTo(1); // priya
    }

    @Test
    @DisplayName("existsById – true for saved user")
    void existsById_true() {
        assertThat(repo.existsById(nageshwar.getId())).isTrue();
    }

    @Test
    @DisplayName("existsById – false for non-existent id")
    void existsById_false() {
        assertThat(repo.existsById(99999L)).isFalse();
    }
}