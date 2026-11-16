package com.jsebastian.eden.EdenSys.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "conversaciones",
        uniqueConstraints = @UniqueConstraint(name = "uk_conversacion_par",
                columnNames = {"usuario1_id", "usuario2_id"}))
@Getter
@Setter
@NoArgsConstructor
public class Conversacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Participantes. El servicio debe guardar siempre el id menor en usuario1
    // para que el par sea único sin importar quién inicie la conversación.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario1_id", nullable = false)
    private User usuario1;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario2_id", nullable = false)
    private User usuario2;

    @Column(nullable = false, updatable = false)
    private LocalDateTime creadaEn;

    private LocalDateTime ultimoMensajeEn;

    @OneToMany(mappedBy = "conversacion", fetch = FetchType.LAZY)
    @OrderBy("enviadoEn ASC")
    private List<Mensaje> mensajes = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (creadaEn == null) creadaEn = LocalDateTime.now();
    }
}