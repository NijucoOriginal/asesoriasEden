package com.jsebastian.eden.EdenSys.services;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CorreoService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Si el envío falla lanza excepción, para que la transacción que llama haga rollback.
     */
    public void enviarCorreo(String destino, String asunto, String mensaje) {
        try {
            MimeMessage mail = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mail, true, "UTF-8");
            helper.setTo(destino);
            helper.setSubject(asunto);
            helper.setFrom(fromEmail);
            helper.setText(plantillaHtml(mensaje), true);
            mailSender.send(mail);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo enviar el correo a " + destino, e);
        }
    }

    // Copia aquí, sin cambios, el método plantillaHtml(String mensaje) que está en UserServiceImpl.
    private String plantillaHtml(String mensaje) {
        // ...
        throw new UnsupportedOperationException("Pega aquí la plantilla de UserServiceImpl");
    }
}