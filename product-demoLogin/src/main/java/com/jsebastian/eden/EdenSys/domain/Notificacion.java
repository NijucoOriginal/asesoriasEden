package com.jsebastian.eden.EdenSys.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificaciones")
@Getter
@Setter
@NoArgsConstructor
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Destinatario de la notificación
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User usuario;

    // Nullable: BIENVENIDA no depende de una asesoría
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asesoria_id")
    private Asesoria asesoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private MotivoNotificacion motivo;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Column(nullable = false, updatable = false)
    private LocalDateTime creadaEn;

    private LocalDateTime leidaEn;

    @PrePersist
    void prePersist() {
        if (creadaEn == null) creadaEn = LocalDateTime.now();
    }
}