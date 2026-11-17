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
            e.printStackTrace();
            throw new IllegalStateException("No se pudo enviar el correo a " + destino, e);
        }

    }

    private String plantillaHtml(String mensaje) {
        return """
        <html>
          <body style="margin:0; padding:0; font-family: Arial, sans-serif; background-color:#f4f4f4;">
            <div style="max-width:600px; margin:auto; background:white; border-radius:10px; overflow:hidden;">
              <div style="background-color:#10bb82; padding:20px; text-align:center;">
                <h1 style="color:white; margin:0;">EDÉN</h1>
                <p style="color:white; margin:0;">Conectamos estudiantes con tutores universitarios</p>
              </div>
              <div style="padding:30px;">
                <p style="color:#2c3e50; font-size:16px; white-space: pre-line;">%s</p>
              </div>
              <div style="background-color:#2c3e50; padding:20px; text-align:center;">
                <p style="color:white; font-size:12px; margin:0;">© 2026 EDÉN Asesorías</p>
              </div>
            </div>
          </body>
        </html>
        """.formatted(mensaje);
    }
}