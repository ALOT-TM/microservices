package com.fluxusbackend.notificationservice.application.service;

import com.fluxusbackend.notificationservice.domain.model.NotificationEvent;
import com.fluxusbackend.notificationservice.domain.model.NotificationType;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmailNotification(NotificationEvent event) throws MessagingException {
        Objects.requireNonNull(event, "NotificationEvent cannot be null");
        Objects.requireNonNull(event.recipient(), "Recipient email cannot be null");
        
        NotificationType type;
        try {
            type = NotificationType.valueOf(event.notificationType().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            log.warn("Unknown or null notification type: {}. Defaulting to DONABLE styling.", event.notificationType());
            type = NotificationType.DONABLE;
        }

        String subject = getSubjectForType(type, event.productName());
        String body = buildHtmlBody(type, event);

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        
        helper.setFrom(fromEmail);
        helper.setTo(event.recipient());
        helper.setSubject(subject);
        helper.setText(body, true);

        mailSender.send(mimeMessage);
    }

    private String getSubjectForType(NotificationType type, String productName) {
        String product = Optional.ofNullable(productName).orElse("Lote");
        return switch (type) {
            case DONABLE -> "🟢 ¡Nueva Merma Disponible! - " + product;
            case RECOGIDO -> "🔵 Confirmación de Recojo - Lote Registrado";
            case SOLICITADO -> "🟠 Nueva Solicitud de Donación - Acción Requerida";
            case ASIGNADO -> "🟣 ¡Solicitud Aprobada! - Agenda tu Recojo";
            case EN_RIESGO -> "🔴 ALERTA CRÍTICA: Lote por Vencer - " + product;
            case PASSWORD_RECOVERY -> "🔒 Restablece tu contraseña - Fluxus BERS";
        };
    }

    private String buildHtmlBody(NotificationType type, NotificationEvent event) {
        String title = getTitleText(type);
        String themeColor = getThemeColor(type);
        String themeBackground = getThemeBackground(type);
        String textDarkColor = getTextDarkColor(type);
        String greeting = getGreeting(type);
        String mainMessage = getMainMessage(type, event.productName());
        
        String batchDetailsHtml = buildBatchDetailsHtml(event);
        String nextStepsHtml = getNextStepsHtml(type);

        return """
        <!DOCTYPE html>
        <html lang="es">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>%s</title>
            <style>
                body {
                    margin: 0;
                    padding: 0;
                    font-family: 'Inter', system-ui, -apple-system, sans-serif;
                    background-color: #f3f4f6;
                    color: #1f2937;
                    -webkit-font-smoothing: antialiased;
                }
                .wrapper {
                    width: 100%%;
                    background-color: #f3f4f6;
                    padding: 40px 0;
                }
                .container {
                    max-width: 600px;
                    margin: 0 auto;
                    background-color: #ffffff;
                    border-radius: 16px;
                    overflow: hidden;
                    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
                }
                .header {
                    background: linear-gradient(135deg, %s 0%%, #1f2937 100%%);
                    padding: 32px;
                    text-align: center;
                }
                .header h1 {
                    color: #ffffff;
                    margin: 0;
                    font-size: 24px;
                    font-weight: 700;
                    letter-spacing: -0.5px;
                }
                .content {
                    padding: 32px;
                }
                .badge {
                    display: inline-block;
                    padding: 6px 12px;
                    border-radius: 9999px;
                    background-color: %s;
                    color: %s;
                    font-size: 12px;
                    font-weight: 600;
                    margin-bottom: 20px;
                    text-transform: uppercase;
                    letter-spacing: 0.5px;
                }
                .greeting {
                    font-size: 18px;
                    font-weight: 600;
                    margin-top: 0;
                    margin-bottom: 12px;
                    color: #111827;
                }
                .message {
                    font-size: 15px;
                    line-height: 1.6;
                    color: #4b5563;
                    margin-bottom: 28px;
                }
                .card {
                    background-color: #f9fafb;
                    border: 1px solid #e5e7eb;
                    border-radius: 12px;
                    padding: 20px;
                    margin-bottom: 28px;
                }
                .card-title {
                    font-size: 14px;
                    font-weight: 700;
                    color: #374151;
                    margin-top: 0;
                    margin-bottom: 16px;
                    text-transform: uppercase;
                    letter-spacing: 0.5px;
                    border-bottom: 1px solid #e5e7eb;
                    padding-bottom: 8px;
                }
                .detail-row {
                    display: flex;
                    justify-content: space-between;
                    margin-bottom: 10px;
                    font-size: 14px;
                }
                .detail-row:last-child {
                    margin-bottom: 0;
                }
                .detail-label {
                    color: #6b7280;
                    font-weight: 500;
                }
                .detail-value {
                    color: #111827;
                    font-weight: 600;
                    text-align: right;
                }
                .next-steps {
                    background-color: #f8fafc;
                    border-left: 4px solid %s;
                    padding: 16px;
                    border-radius: 0 8px 8px 0;
                    margin-bottom: 28px;
                }
                .next-steps-title {
                    font-size: 14px;
                    font-weight: 700;
                    color: %s;
                    margin: 0 0 6px 0;
                }
                .next-steps-text {
                    font-size: 13px;
                    line-height: 1.5;
                    color: #475569;
                    margin: 0;
                }
                .button-container {
                    text-align: center;
                    margin-bottom: 8px;
                }
                .btn {
                    display: inline-block;
                    padding: 12px 24px;
                    background-color: %s;
                    color: #ffffff;
                    text-decoration: none;
                    font-size: 14px;
                    font-weight: 600;
                    border-radius: 8px;
                    transition: background-color 0.2s;
                }
                .footer {
                    background-color: #f9fafb;
                    padding: 24px;
                    text-align: center;
                    border-top: 1px solid #e5e7eb;
                    font-size: 12px;
                    color: #9ca3af;
                }
                .footer p {
                    margin: 4px 0;
                }
                .footer a {
                    color: #6b7280;
                    text-decoration: underline;
                }
            </style>
        </head>
        <body>
            <div class="wrapper">
                <div class="container">
                    <div class="header">
                        <h1>Fluxus BERS</h1>
                    </div>
                    <div class="content">
                        <div class="badge">%s</div>
                        <p class="greeting">%s</p>
                        <p class="message">%s</p>
                        
                        %s
                        
                        %s
                        
                        <div class="button-container">
                            <a href="http://localhost:5173" class="btn">Ir a la Plataforma</a>
                        </div>
                    </div>
                    <div class="footer">
                        <p><strong>Fluxus BERS</strong> — Reducción de Mermas Alimenticias</p>
                        <p>Este es un correo automático. Por favor no respondas directamente a este mensaje.</p>
                        <p><a href="#">Centro de Ayuda</a> | <a href="#">Términos de Servicio</a></p>
                    </div>
                </div>
            </div>
        </body>
        </html>
        """.formatted(
            title,
            themeColor,
            themeBackground,
            textDarkColor,
            themeColor,
            textDarkColor,
            themeColor,
            type.name(),
            greeting,
            mainMessage,
            batchDetailsHtml,
            nextStepsHtml
        );
    }

    private String getTitleText(NotificationType type) {
        return switch (type) {
            case DONABLE -> "Nueva Merma Disponible";
            case RECOGIDO -> "Cierre de Recojo de Lote";
            case SOLICITADO -> "Solicitud de Donación Recibida";
            case ASIGNADO -> "Solicitud de Donación Aprobada";
            case EN_RIESGO -> "Lote en Riesgo de Vencimiento";
            case PASSWORD_RECOVERY -> "Recuperación de Contraseña";
        };
    }

    private String getThemeColor(NotificationType type) {
        return switch (type) {
            case DONABLE -> "#10B981"; // Emerald Green
            case RECOGIDO -> "#3B82F6"; // Blue
            case SOLICITADO -> "#F59E0B"; // Amber/Orange
            case ASIGNADO -> "#6366F1"; // Indigo
            case EN_RIESGO -> "#EF4444"; // Red
            case PASSWORD_RECOVERY -> "#4F46E5"; // Indigo / Slate
        };
    }

    private String getThemeBackground(NotificationType type) {
        return switch (type) {
            case DONABLE -> "#ECFDF5";
            case RECOGIDO -> "#EFF6FF";
            case SOLICITADO -> "#FEF3C7";
            case ASIGNADO -> "#EEF2FF";
            case EN_RIESGO -> "#FEF2F2";
            case PASSWORD_RECOVERY -> "#F5F3FF";
        };
    }

    private String getTextDarkColor(NotificationType type) {
        return switch (type) {
            case DONABLE -> "#065F46";
            case RECOGIDO -> "#1E40AF";
            case SOLICITADO -> "#92400E";
            case ASIGNADO -> "#3730A3";
            case EN_RIESGO -> "#991B1B";
            case PASSWORD_RECOVERY -> "#4338CA";
        };
    }

    private String getGreeting(NotificationType type) {
        return switch (type) {
            case DONABLE, ASIGNADO -> "Estimado Representante de la ONG,";
            case RECOGIDO, SOLICITADO -> "Estimado Administrador Retail,";
            case EN_RIESGO -> "Estimado Usuario de Fluxus,";
            case PASSWORD_RECOVERY -> "Hola,";
        };
    }

    private String getMainMessage(NotificationType type, String productName) {
        String product = Optional.ofNullable(productName).orElse("un lote de productos");
        return switch (type) {
            case DONABLE -> "Nos complace informarte que un nuevo lote de merma de tipo <strong>" + product + "</strong> está disponible en el catálogo para donación. ¡Puedes solicitarlo desde la plataforma!";
            case RECOGIDO -> "Te confirmamos que el lote de merma de tipo <strong>" + product + "</strong> ha sido retirado con éxito de tus instalaciones por la ONG asignada, completando así el proceso logístico.";
            case SOLICITADO -> "Queremos informarte que una Organización No Gubernamental (ONG) ha enviado una solicitud formal para adquirir tu lote disponible de <strong>" + product + "</strong>. Por favor, revisa y aprueba esta solicitud.";
            case ASIGNADO -> "¡Excelentes noticias! Tu solicitud para el lote de merma <strong>" + product + "</strong> ha sido aprobada formalmente por el Retail. Ahora puedes proceder con la coordinación del recojo.";
            case EN_RIESGO -> "⚠️ <strong>Alerta de prioridad crítica:</strong> Se ha detectado que el lote de merma <strong>" + product + "</strong> está a menos de 3 días de su fecha de vencimiento y aún no ha sido recolectado. Es urgente coordinar su retiro inmediato.";
            case PASSWORD_RECOVERY -> "Hemos recibido una solicitud para restablecer la contraseña de tu cuenta en Fluxus BERS. Si tú no realizaste esta solicitud, puedes ignorar este correo de forma segura.";
        };
    }

    private String getNextStepsHtml(NotificationType type) {
        return switch (type) {
            case DONABLE -> """
                <div class="next-steps">
                    <p class="next-steps-title">¿Qué debes hacer?</p>
                    <p class="next-steps-text">Ingresa a la sección "Donaciones Disponibles" en Fluxus, revisa los detalles del lote y postula para obtener la donación.</p>
                </div>
                """;
            case RECOGIDO -> """
                <div class="next-steps">
                    <p class="next-steps-title">Paso Siguiente</p>
                    <p class="next-steps-text">El espacio físico ha quedado liberado. Ya puedes descargar el acta de recojo y proceder con el registro fiscal de la donación.</p>
                </div>
                """;
            case SOLICITADO -> """
                <div class="next-steps">
                    <p class="next-steps-title">Acción Requerida</p>
                    <p class="next-steps-text">Ve a la plataforma para evaluar la solicitud de la organización beneficiaria y agendar el horario para el recojo del lote.</p>
                </div>
                """;
            case ASIGNADO -> """
                <div class="next-steps">
                    <p class="next-steps-title">Próximos Pasos</p>
                    <p class="next-steps-text">Coordina el medio de transporte correspondiente. Recuerda realizar el recojo en las ventanas horarias estipuladas por la tienda de origen.</p>
                </div>
                """;
            case EN_RIESGO -> """
                <div class="next-steps">
                    <p class="next-steps-title">¡Atención Inmediata!</p>
                    <p class="next-steps-text">Por favor comunícate directamente con la contraparte para agilizar el retiro del lote antes de que se cumpla la fecha límite.</p>
                </div>
                """;
            case PASSWORD_RECOVERY -> """
                <div class="next-steps">
                    <p class="next-steps-title">Código de Recuperación</p>
                    <p class="next-steps-text">Ingresa el código que se muestra a continuación en la pantalla de verificación para restablecer tu contraseña. Recuerda que este token expira en 10 minutos.</p>
                </div>
                """;
        };
    }

    private String buildBatchDetailsHtml(NotificationEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"card\">");
        String cardTitle = (event.notificationType() != null && event.notificationType().equalsIgnoreCase("PASSWORD_RECOVERY"))
                ? "Información de Recuperación" : "Detalles del Lote";
        sb.append("<p class=\"card-title\">").append(cardTitle).append("</p>");
        
        if (event.batchId() != null && !event.batchId().isBlank()) {
            sb.append("<div class=\"detail-row\">");
            sb.append("<span class=\"detail-label\">ID del Lote:</span>");
            sb.append("<span class=\"detail-value\">").append(event.batchId()).append("</span>");
            sb.append("</div>");
        }

        if (event.productName() != null && !event.productName().isBlank()) {
            sb.append("<div class=\"detail-row\">");
            sb.append("<span class=\"detail-label\">Producto:</span>");
            sb.append("<span class=\"detail-value\">").append(event.productName()).append("</span>");
            sb.append("</div>");
        }

        if (event.details() != null && !event.details().isEmpty()) {
            for (Map.Entry<String, String> entry : event.details().entrySet()) {
                sb.append("<div class=\"detail-row\">");
                sb.append("<span class=\"detail-label\">").append(translateKey(entry.getKey())).append(":</span>");
                sb.append("<span class=\"detail-value\">").append(entry.getValue()).append("</span>");
                sb.append("</div>");
            }
        }
        
        sb.append("</div>");
        return sb.toString();
    }

    private String translateKey(String key) {
        if (key == null) return "";
        return switch (key.toLowerCase()) {
            case "quantity", "cantidad" -> "Cantidad";
            case "expirationdate", "vencimiento" -> "Fecha de Vencimiento";
            case "location", "ubicacion", "tienda" -> "Ubicación / Tienda";
            case "ongname", "ong" -> "Organización Asignada";
            case "retailname", "retail" -> "Tienda Origen";
            case "category", "categoria" -> "Categoría";
            case "token" -> "Código de Validación";
            case "username" -> "Nombre de Usuario";
            default -> Character.toUpperCase(key.charAt(0)) + key.substring(1);
        };
    }
}
