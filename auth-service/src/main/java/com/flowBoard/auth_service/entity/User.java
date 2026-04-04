package com.flowBoard.auth_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    private String fullName;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true)
    private String username;

    private String password;

    private String bio;

    @Enumerated(EnumType.STRING)
    private ROLE role;

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private boolean emailVerified = false;

    private String avatarUrl;
    private String provider;
    private LocalDateTime createdAt;


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        return List.of(()->role.name());
    }

    @Override
    public String getUsername(){
        return email;
    }

    @Override
    public boolean isEnabled(){
        return active;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; }

    @Override
    public boolean isAccountNonLocked() {
        return active; }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; }
}
