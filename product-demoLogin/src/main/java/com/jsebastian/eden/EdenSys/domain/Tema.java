package com.jsebastian.eden.EdenSys.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "temas")
@Getter
@Setter
@NoArgsConstructor
public class Tema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombreTema;

    // Dueña de la relación N:M. Tabla puente: tema_materia
    @ManyToMany
    @JoinTable(
            name = "tema_materia",
            joinColumns = @JoinColumn(name = "tema_id"),
            inverseJoinColumns = @JoinColumn(name = "materia_id")
    )
    private List<Materia> materiasPertenecientes = new ArrayList<>();
}