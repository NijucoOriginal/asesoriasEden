package com.jsebastian.eden.EdenSys.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "materias")
@Getter
@Setter
@NoArgsConstructor
public class Materia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombreMateria;

    @Column(nullable = false)
    private int creditos;

    // Dueña de la relación: columna programa_id en materias
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "programa_id", nullable = false)
    private Programa programa;

    // Lado inverso: la dueña es Tema.materiasPertenecientes
    @ManyToMany(mappedBy = "materiasPertenecientes")
    private List<Tema> temas = new ArrayList<>();
}