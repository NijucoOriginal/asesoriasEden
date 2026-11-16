package com.jsebastian.eden.EdenSys.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ---- Datos de cuenta (antes en Persona) ----

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    // Hash BCrypt, nunca texto plano
    @Column(nullable = false)
    private String contrasena;

    @Column(nullable = false, length = 50)
    private String nombre;

    @Column(nullable = false, length = 50)
    private String apellido;

    @Column(nullable = false, unique = true, length = 20)
    private String documentoIdentidad;

    @Column(nullable = false, length = 20)
    private String telefono;

    // Seguridad: autoridad usada por Spring Security
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    // Ciclo de vida de la cuenta
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoUsuario estado;

    // ---- Activación ----

    @Column(length = 6)
    private String codigoActivacion;

    private LocalDateTime fechaCreacionCodigo;

    // ---- Perfil ----

    // URL segura de Cloudinary. Puede ser larga, por eso TEXT.
    @Column(columnDefinition = "TEXT")
    private String imagenPerfil;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaIngreso;

    // ---- Académico ----

    // Un usuario tiene un programa académico
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "programa_id")
    private Programa programaAcademico;

    // Materias que el usuario puede enseñar (N:M). Tabla: user_materia_dicta
    @ManyToMany
    @JoinTable(
            name = "user_materia_dicta",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "materia_id")
    )
    private Set<Materia> materiasQueDicta = new HashSet<>();

    // Tutores favoritos (N:M consigo mismo). Tabla: user_tutor_favorito
    @ManyToMany
    @JoinTable(
            name = "user_tutor_favorito",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "tutor_id")
    )
    private Set<User> tutoresFavoritos = new HashSet<>();

    // Asesorías como tutor (lado inverso de Asesoria.asesor)
    @OneToMany(mappedBy = "asesor")
    private List<Asesoria> asesoriasDadas = new ArrayList<>();

    // Asesorías como estudiante (lado inverso de Asesoria.participante)
    @OneToMany(mappedBy = "participante")
    private List<Asesoria> asesoriasRecibidas = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (fechaIngreso == null) {
            fechaIngreso = LocalDateTime.now();
        }
    }
}