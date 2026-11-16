package com.jsebastian.eden.EdenSys.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "asesorias")
@Getter
@Setter
@NoArgsConstructor
public class Asesoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tutor (dueño de la materia que dicta)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asesor_id", nullable = false)
    private User asesor;

    @Version
    private Long version;

    // Estudiante que solicita la asesoría
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participante_id", nullable = false)
    private User participante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "materia_id", nullable = false)
    private Materia materia;

    @ManyToMany
    @JoinTable(
            name = "asesoria_tema",
            joinColumns = @JoinColumn(name = "asesoria_id"),
            inverseJoinColumns = @JoinColumn(name = "tema_id")
    )
    private List<Tema> temas = new ArrayList<>();

    // ---- Propuesta vigente (la última enviada por cualquiera de las partes) ----
    @Column(nullable = false)
    private LocalDateTime fechaInicioPropuesta;

    @Column(nullable = false)
    private LocalDateTime fechaCierrePropuesta;

    // ---- Fechas confirmadas (solo al pasar a ACEPTADA) ----
    private LocalDateTime fechaInicioConfirmada;

    private LocalDateTime fechaCierreConfirmada;

    // ---- Precio ----
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(precision = 12, scale = 2)
    private BigDecimal comision;

    // ---- Estado y trazabilidad ----
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoAsesoria estado;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MotivoContrapropuesta motivoContrapropuesta;

    // Quien recibió la última propuesta. Se reinicia el plazo de expiración desde aquí.
    @Column(nullable = false)
    private LocalDateTime creadaEn;

    @Column(nullable = false)
    private LocalDateTime actualizadaEn;

    private LocalDateTime aceptadaEn;

    // Solo uno de los dos tiene valor según el estado
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rechazada_por_id")
    private User rechazadaPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelada_por_id")
    private User canceladaPor;

    @PrePersist
    void prePersist() {
        LocalDateTime ahora = LocalDateTime.now();
        if (creadaEn == null) creadaEn = ahora;
        if (actualizadaEn == null) actualizadaEn = ahora;
    }
}