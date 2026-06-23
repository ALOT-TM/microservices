package com.fluxusbackend.subscription.application.internal.services;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.SetupIntent;
import com.stripe.param.SetupIntentCreateParams;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StripeValidationService {

    public StripeValidationService() {
        // Inicializa Stripe con tu Clave Secreta de Pruebas desde la variable de entorno
        String secretKey = System.getenv("StripeSecretKey");
        if (secretKey == null || secretKey.trim().isEmpty()) {
            // Valor por defecto temporal para evitar fallos si no está definida en desarrollo
            secretKey = "sk_test_tu_clave_secreta_aqui";
        }
        Stripe.apiKey = secretKey;
    }

    public void verificarTarjeta(String paymentMethodId) {
        try {
            // Configurar los parámetros para validar la tarjeta sin cobrar
            var params = SetupIntentCreateParams.builder()
                .setPaymentMethod(paymentMethodId)
                .setConfirm(true) // Confirma la validación inmediatamente
                .setUsage(SetupIntentCreateParams.Usage.OFF_SESSION) // Indica que planeas usarla después
                .setReturnUrl("https://tu-proyecto.edu/callback") // Requerido para tarjetas con 3D Secure
                .build();

            // Ejecuta la validación en Stripe
            SetupIntent setupIntent = SetupIntent.create(params);

            // Analizar la respuesta
            if ("succeeded".equals(setupIntent.getStatus())) {
                System.out.println("✅ ¡Tarjeta válida y verificada exitosamente!");
            } else if ("requires_action".equals(setupIntent.getStatus())) {
                System.out.println("⚠️ La tarjeta requiere autenticación adicional (ej. 3D Secure / SMS del banco).");
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "La tarjeta requiere autenticación adicional (ej. 3D Secure / SMS del banco).");
            } else {
                System.out.println("❌ Estado de verificación desconocido: " + setupIntent.getStatus());
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Estado de verificación desconocido: " + setupIntent.getStatus());
            }

        } catch (StripeException e) {
            // Si la tarjeta expiró, los fondos son insuficientes para la prueba, o no existe, caerá aquí
            System.err.println("❌ Error de validación: " + e.getMessage());
            System.err.println("Código de error: " + e.getCode());
            
            // Traducimos el error para mostrarlo de forma elegante en la interfaz de usuario
            String mensajeTraducido = traducirErrorStripe(e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, mensajeTraducido, e);
        }
    }

    private String traducirErrorStripe(StripeException e) {
        String code = e.getCode();
        if (code == null) {
            return "Error de validación con Stripe: " + e.getMessage();
        }
        return switch (code) {
            case "card_declined" -> "La tarjeta ha sido declinada. Por favor, usa otra tarjeta.";
            case "expired_card" -> "La tarjeta ha expirado. Revisa la fecha de expiración.";
            case "incorrect_cvc" -> "El código CVC es incorrecto.";
            case "incorrect_number" -> "El número de tarjeta es incorrecto.";
            case "processing_error" -> "Ocurrió un error al procesar la tarjeta. Inténtalo de nuevo.";
            default -> "Error de pago (" + code + "): " + e.getMessage();
        };
    }
}
