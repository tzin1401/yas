package com.yas.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.order.mapper.OrderMapper;
import com.yas.order.model.Order;
import com.yas.order.model.OrderAddress;
import com.yas.order.model.OrderItem;
import com.yas.order.model.enumeration.DeliveryMethod;
import com.yas.order.model.enumeration.DeliveryStatus;
import com.yas.order.model.enumeration.OrderStatus;
import com.yas.order.model.enumeration.PaymentMethod;
import com.yas.order.model.enumeration.PaymentStatus;
import com.yas.order.model.request.OrderRequest;
import com.yas.order.repository.OrderItemRepository;
import com.yas.order.repository.OrderRepository;
import com.yas.order.viewmodel.order.OrderItemPostVm;
import com.yas.order.viewmodel.order.OrderPostVm;
import com.yas.order.viewmodel.order.PaymentOrderStatusVm;
import com.yas.order.viewmodel.orderaddress.OrderAddressPostVm;
import com.yas.order.viewmodel.promotion.PromotionUsageVm;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.Pair;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private ProductService productService;
    @Mock private CartService cartService;
    @Mock private OrderMapper orderMapper;
    @Mock private PromotionService promotionService;

    @InjectMocks private OrderService orderService;

    @Test
    void createOrderShouldPersistOrderItemsUpdateStockCartAndPromotionUsage() {
        OrderPostVm postVm = orderPostVm();
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(123L);
            return order;
        });
        when(orderItemRepository.saveAll(any())).thenAnswer(invocation -> {
            Iterable<OrderItem> items = invocation.getArgument(0);
            return List.copyOf((java.util.Collection<OrderItem>) items);
        });
        when(orderRepository.findById(123L)).thenReturn(Optional.of(order(123L)));

        var result = orderService.createOrder(postVm);

        assertThat(result.id()).isEqualTo(123L);
        assertThat(result.orderStatus()).isEqualTo(OrderStatus.PENDING);
        verify(productService).subtractProductStockQuantity(result);
        verify(cartService).deleteCartItems(result);
        verify(promotionService).updateUsagePromotion(anyList());
        verify(orderRepository, times(2)).save(any(Order.class));
    }

    @Test
    void getOrderWithItemsByIdShouldReturnOrderVmOrThrowWhenMissing() {
        Order order = order(10L);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findAllByOrderId(10L)).thenReturn(List.of(orderItem(1L, 10L)));

        var result = orderService.getOrderWithItemsById(10L);

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.orderItemVms()).hasSize(1);

        when(orderRepository.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> orderService.getOrderWithItemsById(404L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getAllOrderShouldReturnEmptyOrMappedPage() {
        when(orderRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));

        var empty = orderService.getAllOrder(
            Pair.of(ZonedDateTime.now().minusDays(1), ZonedDateTime.now()),
            "phone",
            List.of(),
            Pair.of("VN", "0909"),
            "buyer@example.com",
            Pair.of(0, 10));

        assertThat(empty.orderList()).isNull();
        assertThat(empty.totalElements()).isZero();

        when(orderRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(order(10L))));

        var page = orderService.getAllOrder(
            Pair.of(ZonedDateTime.now().minusDays(1), ZonedDateTime.now()),
            "phone",
            List.of(OrderStatus.ACCEPTED),
            Pair.of("VN", "0909"),
            "buyer@example.com",
            Pair.of(0, 10));

        assertThat(page.orderList()).hasSize(1);
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    void latestCheckoutAndPaymentMethodsShouldHandleHappyPaths() {
        Order order = order(10L);
        when(orderRepository.getLatestOrders(any())).thenReturn(List.of(order));
        when(orderRepository.findByCheckoutId("checkout-1")).thenReturn(Optional.of(order));
        when(orderItemRepository.findAllByOrderId(10L)).thenReturn(List.of(orderItem(1L, 10L)));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(orderService.getLatestOrders(0)).isEmpty();
        assertThat(orderService.getLatestOrders(5)).hasSize(1);
        assertThat(orderService.findOrderByCheckoutId("checkout-1").getId()).isEqualTo(10L);
        assertThat(orderService.findOrderVmByCheckoutId("checkout-1").orderItems()).hasSize(1);

        PaymentOrderStatusVm statusVm = PaymentOrderStatusVm.builder()
            .orderId(10L)
            .paymentId(99L)
            .paymentStatus(PaymentStatus.COMPLETED.name())
            .build();

        var result = orderService.updateOrderPaymentStatus(statusVm);

        assertThat(result.orderStatus()).isEqualTo(OrderStatus.PAID.getName());
        assertThat(order.getPaymentId()).isEqualTo(99L);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
    }

    @Test
    void acceptRejectAndPaymentShouldThrowWhenOrderMissing() {
        Order order = order(10L);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.acceptOrder(10L);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.ACCEPTED);

        orderService.rejectOrder(10L, "out of stock");
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.REJECT);
        assertThat(order.getRejectReason()).isEqualTo("out of stock");

        when(orderRepository.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> orderService.acceptOrder(404L)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> orderService.rejectOrder(404L, "missing")).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> orderService.updateOrderPaymentStatus(
            PaymentOrderStatusVm.builder().orderId(404L).paymentStatus(PaymentStatus.COMPLETED.name()).build()))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void exportCsvShouldReturnHeaderWhenNoOrders() throws IOException {
        OrderRequest request = new OrderRequest();
        request.setCreatedFrom(ZonedDateTime.now().minusDays(1));
        request.setCreatedTo(ZonedDateTime.now());
        request.setProductName("phone");
        request.setOrderStatus(List.of());
        request.setBillingCountry("VN");
        request.setBillingPhoneNumber("0909");
        request.setEmail("buyer@example.com");
        request.setPageNo(0);
        request.setPageSize(10);
        when(orderRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));

        byte[] csv = orderService.exportCsv(request);

        assertThat(csv).isNotEmpty();
    }

    private OrderPostVm orderPostVm() {
        return OrderPostVm.builder()
            .checkoutId("checkout-1")
            .email("buyer@example.com")
            .shippingAddressPostVm(address())
            .billingAddressPostVm(address())
            .note("note")
            .tax(1F)
            .discount(2F)
            .numberItem(1)
            .totalPrice(BigDecimal.TEN)
            .deliveryFee(BigDecimal.ONE)
            .couponCode("PROMO")
            .deliveryMethod(DeliveryMethod.YAS_EXPRESS)
            .paymentMethod(PaymentMethod.COD)
            .paymentStatus(PaymentStatus.PENDING)
            .orderItemPostVms(List.of(OrderItemPostVm.builder()
                .productId(1L)
                .productName("Phone")
                .quantity(2)
                .productPrice(BigDecimal.TEN)
                .note("item")
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .taxPercent(BigDecimal.ZERO)
                .build()))
            .build();
    }

    private OrderAddressPostVm address() {
        return OrderAddressPostVm.builder()
            .contactName("Buyer")
            .phone("0909")
            .addressLine1("Line 1")
            .addressLine2("Line 2")
            .city("HCM")
            .zipCode("70000")
            .districtId(1L)
            .districtName("District")
            .stateOrProvinceId(2L)
            .stateOrProvinceName("State")
            .countryId(3L)
            .countryName("Vietnam")
            .build();
    }

    private Order order(Long id) {
        return Order.builder()
            .id(id)
            .email("buyer@example.com")
            .note("note")
            .tax(1F)
            .discount(2F)
            .numberItem(1)
            .totalPrice(BigDecimal.TEN)
            .deliveryFee(BigDecimal.ONE)
            .couponCode("PROMO")
            .orderStatus(OrderStatus.PENDING)
            .deliveryMethod(DeliveryMethod.YAS_EXPRESS)
            .deliveryStatus(DeliveryStatus.PREPARING)
            .paymentStatus(PaymentStatus.PENDING)
            .checkoutId("checkout-1")
            .shippingAddressId(orderAddress(1L))
            .billingAddressId(orderAddress(2L))
            .build();
    }

    private OrderAddress orderAddress(Long id) {
        return OrderAddress.builder()
            .id(id)
            .contactName("Buyer")
            .phone("0909")
            .addressLine1("Line 1")
            .addressLine2("Line 2")
            .city("HCM")
            .zipCode("70000")
            .districtId(1L)
            .districtName("District")
            .stateOrProvinceId(2L)
            .stateOrProvinceName("State")
            .countryId(3L)
            .countryName("Vietnam")
            .build();
    }

    private OrderItem orderItem(Long id, Long orderId) {
        return OrderItem.builder()
            .id(id)
            .orderId(orderId)
            .productId(1L)
            .productName("Phone")
            .quantity(2)
            .productPrice(BigDecimal.TEN)
            .note("item")
            .discountAmount(BigDecimal.ZERO)
            .taxAmount(BigDecimal.ZERO)
            .taxPercent(BigDecimal.ZERO)
            .build();
    }
}
