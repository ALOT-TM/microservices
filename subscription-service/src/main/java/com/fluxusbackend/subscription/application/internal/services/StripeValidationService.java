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
        String secretKey = System.getenv("StripeSecretKey");
        if (secretKey == null || secretKey.trim().isEmpty()) {
            secretKey = "sk_test_key"; // Clave de prueba de Stripe
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
                System.out.println("¡Tarjeta válida y verificada exitosamente!");
            } else if ("requires_action".equals(setupIntent.getStatus())) {
                String errorMsg = "La tarjeta requiere autenticación adicional (ej. 3D Secure / SMS del banco).";
                System.out.println(errorMsg);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMsg);
            } else {
                String errorMsg = "Estado de verificación desconocido: " + setupIntent.getStatus();
                System.out.println(errorMsg);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMsg);
            }

        } catch (StripeException e) {
            // Si la tarjeta expiró, los fondos son insuficientes para la prueba, o no existe, caerá aquí
            String errorMsg = "Error de validación con Stripe: " + e.getMessage();
            String codeMsg = "Código de error: " + e.getCode();
            System.err.println(errorMsg);
            System.err.println(codeMsg);
            
            // Traducimos el error para mostrarlo de forma elegante en la interfaz de usuario
            String mensajeTraducido = traducirErrorStripe(e);
            String fullMessage = codeMsg + "\n" + mensajeTraducido;
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fullMessage, e);
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
            case "incorrect_cvc" -> "El codigo CVC es incorrecto.";
            case "incorrect_number" -> "El numero de tarjeta es incorrecto.";
            case "processing_error" -> "Ocurrio un error al procesar la tarjeta. Inténtalo de nuevo.";
            default -> "Error de pago (" + code + "): " + e.getMessage();
        };
    }
}
