package com.jsebastian.eden.EdenSys.security;

import com.jsebastian.eden.EdenSys.domain.EstadoUsuario;
import com.jsebastian.eden.EdenSys.domain.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return switch (user.getEstado()) {
            case ACTIVO -> List.of(new SimpleGrantedAuthority(user.getRol().name()));
            case INACTIVO -> List.of(new SimpleGrantedAuthority("INACTIVO"));
            case PENDIENTE -> List.of();
        };
    }

    @Override
    public String getPassword() {
        return user.getContrasena();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    // Una cuenta PENDIENTE no puede usar la API. ACTIVO e INACTIVO sí, con permisos distintos.
    @Override
    public boolean isEnabled() {
        return user.getEstado() != EstadoUsuario.PENDIENTE;
    }
}