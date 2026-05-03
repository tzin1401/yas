package com.yas.rating.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.commonlibrary.exception.ResourceExistedException;
import com.yas.rating.model.Rating;
import com.yas.rating.repository.RatingRepository;
import com.yas.rating.viewmodel.CustomerVm;
import com.yas.rating.viewmodel.OrderExistsByProductAndUserGetVm;
import com.yas.rating.viewmodel.RatingListVm;
import com.yas.rating.viewmodel.RatingPostVm;
import com.yas.rating.viewmodel.RatingVm;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    private final String userId = "user1";
    
    @Mock
    private RatingRepository ratingRepository;
    @Mock
    private CustomerService customerService;
    @Mock
    private OrderService orderService;
    
    @InjectMocks
    private RatingService ratingService;

    private List<Rating> ratingList;

    @BeforeEach
    void setUp() {
        ratingList = List.of(
            Rating.builder()
                .id(1L)
                .content("comment 1")
                .ratingStar(4)
                .productId(1L)
                .productName("product1")
                .firstName("Duy")
                .lastName("Nguyen")
                .build()
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setUpSecurityContext(String subject) {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(subject);
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);
        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
    }

    @Test
    void getRatingList_ValidProductId_ShouldSuccess() {
        Page<Rating> page = new PageImpl<>(ratingList);
        when(ratingRepository.findByProductId(anyLong(), any(Pageable.class))).thenReturn(page);

        RatingListVm actualResponse = ratingService.getRatingListByProductId(1L, 0, 10);
        assertEquals(1, actualResponse.ratingList().size());
    }

    @Test
    void createRating_ValidRatingData_ShouldSuccess() {
        setUpSecurityContext(userId);

        when(orderService.checkOrderExistsByProductAndUserWithStatus(anyLong())).
                thenReturn(new OrderExistsByProductAndUserGetVm(true));
        when(customerService.getCustomer()).thenReturn(new CustomerVm(userId, "user", "Cuong", "Tran"));
        
        Rating savedRating = ratingList.get(0);
        when(ratingRepository.saveAndFlush(any(Rating.class))).thenReturn(savedRating);

        RatingPostVm postVm = RatingPostVm.builder().content("cmt").star(4).productId(1L).build();
        RatingVm result = ratingService.createRating(postVm);
        
        assertEquals(4, result.star());
    }

    @Test
    void createRating_ExistedRating_ShouldThrowResourceExistedException() {
        setUpSecurityContext(userId);

        when(orderService.checkOrderExistsByProductAndUserWithStatus(anyLong())).thenReturn(new OrderExistsByProductAndUserGetVm(true));
        when(ratingRepository.existsByCreatedByAndProductId(anyString(), anyLong())).thenReturn(true);

        RatingPostVm postVm = RatingPostVm.builder().productId(1L).build();
        assertThrows(ResourceExistedException.class, () -> ratingService.createRating(postVm));
    }

    @Test
    void calculateAverageStar_ValidProductId_ShouldSuccess() {
        Object[] row = new Object[]{10, 2};
        List<Object[]> mockResult = java.util.Collections.singletonList(row);
        when(ratingRepository.getTotalStarsAndTotalRatings(1L)).thenReturn(mockResult);
        
        Double averageStar = ratingService.calculateAverageStar(1L);
        assertEquals(5.0, averageStar);
    }
}
