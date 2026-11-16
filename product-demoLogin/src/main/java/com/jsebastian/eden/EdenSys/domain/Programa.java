package com.jsebastian.eden.EdenSys.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "programas")
@Getter
@Setter
@NoArgsConstructor
public class Programa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombrePrograma;

    @Column(nullable = false)
    private int duracionSemestres;

    @Column(nullable = false)
    private int cantidadCreditos;

    // Lado inverso: la dueña de la relación es Materia.programa
    @OneToMany(mappedBy = "programa")
    private List<Materia> pensum = new ArrayList<>();
}