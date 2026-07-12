package com.yas.payment.service.provider.handler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.yas.payment.model.CapturedPayment;
import com.yas.payment.model.enumeration.PaymentMethod;
import com.yas.payment.model.enumeration.PaymentStatus;
import com.yas.payment.paypal.service.PaypalService;
import com.yas.payment.paypal.viewmodel.PaypalCapturePaymentRequest;
import com.yas.payment.paypal.viewmodel.PaypalCapturePaymentResponse;
import com.yas.payment.service.PaymentProviderService;
import com.yas.payment.viewmodel.CapturePaymentRequestVm;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaypalHandlerTest {

    @Mock
    private PaymentProviderService paymentProviderService;
    @Mock
    private PaypalService paypalService;

    private PaypalHandler paypalHandler;

    @BeforeEach
    void setUp() {
        paypalHandler = new PaypalHandler(paymentProviderService, paypalService);
    }

    @Test
    void capturePayment_whenPaypalOrderNotApproved_returnsCancelledInsteadOfThrowing() {
        // Cancelled/never-approved PayPal orders only come back with a failureMessage --
        // paymentStatus and paymentMethod are null, which used to make
        // PaymentStatus.valueOf(null)/PaymentMethod.valueOf(null) throw an NPE.
        when(paypalService.capturePayment(any(PaypalCapturePaymentRequest.class)))
            .thenReturn(PaypalCapturePaymentResponse.builder()
                .failureMessage("{\"details\":[{\"issue\":\"ORDER_NOT_APPROVED\"}]}")
                .build());

        CapturedPayment result = assertDoesNotThrow(
            () -> paypalHandler.capturePayment(new CapturePaymentRequestVm("PAYPAL", "TOKEN123")));

        assertEquals(PaymentStatus.CANCELLED, result.getPaymentStatus());
        assertEquals(PaymentMethod.PAYPAL, result.getPaymentMethod());
    }

    @Test
    void capturePayment_whenPaypalCaptureSucceeds_returnsCompleted() {
        when(paypalService.capturePayment(any(PaypalCapturePaymentRequest.class)))
            .thenReturn(PaypalCapturePaymentResponse.builder()
                .checkoutId("chk-1")
                .amount(BigDecimal.TEN)
                .paymentFee(BigDecimal.ONE)
                .gatewayTransactionId("gw-1")
                .paymentMethod("PAYPAL")
                .paymentStatus("COMPLETED")
                .build());

        CapturedPayment result = paypalHandler.capturePayment(new CapturePaymentRequestVm("PAYPAL", "TOKEN456"));

        assertEquals(PaymentStatus.COMPLETED, result.getPaymentStatus());
        assertEquals(PaymentMethod.PAYPAL, result.getPaymentMethod());
        assertEquals("chk-1", result.getCheckoutId());
    }
}
