package com.jsebastian.eden.EdenSys.services;

import com.jsebastian.eden.EdenSys.repository.AsesoriaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AsesoriaScheduler {

    private static final int DIAS_PLAZO_RESPUESTA = 7;

    private final AsesoriaRepository asesoriaRepository;
    private final AsesoriaExpiracionService expiracionService;

    // Cada 15 minutos. Sin transacción aquí: cada asesoría se procesa en su propia transacción.
    @Scheduled(fixedRate = 15 * 60 * 1000)
    public void procesar() {
        LocalDateTime ahora = LocalDateTime.now();

        for (Long id : asesoriaRepository.findIdsVencidasParaExpirar(ahora.minusDays(DIAS_PLAZO_RESPUESTA))) {
            try {
                expiracionService.expirar(id);
            } catch (Exception e) {
                log.error("Error expirando asesoría {}", id, e);
            }
        }

        for (Long id : asesoriaRepository.findIdsAceptadasParaFinalizar(ahora)) {
            try {
                expiracionService.finalizar(id);
            } catch (Exception e) {
                log.error("Error finalizando asesoría {}", id, e);
            }
        }
    }
}