package com.jsebastian.eden.EdenSys.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "resenias",
        uniqueConstraints = @UniqueConstraint(name = "uk_resenia_asesoria", columnNames = "asesoria_id"))
@Getter
@Setter
@NoArgsConstructor
public class Resena {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Una reseña por asesoría. El autor es asesoria.participante y el tutor es asesoria.asesor
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asesoria_id", nullable = false, unique = true)
    private Asesoria asesoria;

    @Min(1)
    @Max(5)
    @Column(nullable = false)
    private int calificacion;

    @Column(length = 1000)
    private String descripcion;
}