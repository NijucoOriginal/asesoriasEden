package com.jsebastian.eden.EdenSys.security;

import com.jsebastian.eden.EdenSys.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${frontend.url}")
    private String frontendUrl;

    @Value("${frontend.local.url}")
    private String frontendLocalUrl;

    private final UserRepository userRepository; // inyectar repo para crear UserDetailsService

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByEmail(username)
                .map(user -> new CustomUserDetails(user))
                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException("Usuario no encontrado: " + username));
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService uds, PasswordEncoder encoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(uds);
        authProvider.setPasswordEncoder(encoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Nota: recibimos JwtAuthenticationFilter como parámetro del método bean (NO en el constructor)
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .cors(cors -> cors.configurationSource(request -> {
                    var corsConfig = new org.springframework.web.cors.CorsConfiguration();
                    corsConfig.setAllowedOrigins(List.of(
                            frontendUrl,
                            frontendLocalUrl,
                            "http://localhost:4200",
                            "http://localhost:4201",
                            "http://localhost:5678",
                            "http://n8n:5678"));
                    corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    corsConfig.setAllowedHeaders(List.of("Authorization", "Content-Type"));
                    corsConfig.setAllowCredentials(true);
                    return corsConfig;
                }))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/actuator/**").permitAll()
                        // Registro
                        .requestMatchers(HttpMethod.POST, "/api/usuarios").permitAll()
                        // Autenticación, activación y recuperación
                        .requestMatchers(
                                "/api/auth/activate/**",
                                "/api/auth/login",
                                "/api/auth/resend-activation",
                                "/api/auth/contacto",
                                "/api/auth/recuperar",
                                "/api/auth/recuperar/cambiar").permitAll()
                        // Tutores: públicos (se muestran en la página principal)
                        .requestMatchers(HttpMethod.GET, "/api/tutores", "/api/tutores/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/materias", "/api/materias/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/programas", "/api/programas/**").permitAll()
                        // WebSocket: el token se valida en el handshake
                        .requestMatchers("/ws-chat/**").permitAll()
                        // Cuenta INACTIVA: solo perfil y reactivación
                        .requestMatchers("/api/usuarios/me").hasAnyAuthority("INACTIVO", "ESTUDIANTE", "MODERADOR")
                        .requestMatchers("/api/usuarios/reactivar/**").hasAuthority("INACTIVO")
                        // Cuenta ACTIVA
                        .requestMatchers("/api/usuarios/**", "/api/asesorias/**", "/api/chat/**")
                        .hasAnyAuthority("ESTUDIANTE", "MODERADOR")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}
