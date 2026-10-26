package com.ecom.platform.controller;

import com.ecom.platform.config.JwtTokenProvider;
import com.ecom.platform.config.SecurityConfig;
import com.ecom.platform.dto.ProductDto;
import com.ecom.platform.exception.BadRequestException;
import com.ecom.platform.exception.ResourceNotFoundException;
import com.ecom.platform.service.CustomUserDetailsService;
import com.ecom.platform.service.ImageStorageService;
import com.ecom.platform.service.ProductService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
class ProductControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private ProductService productService;
    @MockBean private ImageStorageService imageStorageService;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private CustomUserDetailsService customUserDetailsService;

    private ProductDto phone() {
        return ProductDto.builder().id(1L).name("Phone").slug("phone").price(new BigDecimal("100.00")).build();
    }

    @Test
    void catalogIsPublicAndUsesDefaultPaging() throws Exception {
        when(productService.searchProducts(any(), any(), any(), any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(phone())));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Phone"));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(productService).searchProducts(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(12);
        assertThat(pageable.getValue().getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void catalogPassesFiltersAndAscendingSort() throws Exception {
        when(productService.searchProducts(any(), any(), any(), any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/products")
                        .param("keyword", "phone").param("categoryId", "3").param("minPrice", "50")
                        .param("isTopDeal", "true").param("page", "2").param("size", "5").param("sort", "price,asc"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(productService).searchProducts(eq("phone"), eq(3L), eq(new BigDecimal("50")), isNull(), isNull(), eq(true), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageable.getValue().getSort().getOrderFor("price").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void getProductById() throws Exception {
        when(productService.getProductById(1L)).thenReturn(phone());

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("phone"))
                .andExpect(jsonPath("$.price").value(100.00));
    }

    @Test
    void missingProductReturns404WithMessage() throws Exception {
        when(productService.getProductById(99L)).thenThrow(new ResourceNotFoundException("Product not found or soft-deleted"));

        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Product not found or soft-deleted"));
    }

    @Test
    void topDealsEndpoint() throws Exception {
        when(productService.getTopDeals(any())).thenReturn(new PageImpl<>(List.of(phone())));

        mockMvc.perform(get("/api/products/top-deals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void reviewWithOutOfRangeRatingFailsValidation() throws Exception {
        mockMvc.perform(post("/api/products/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1,\"rating\":6,\"comment\":\"too good\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.rating").value("Rating cannot exceed 5"));
        verifyNoInteractions(productService);
    }

    @Test
    void imageEndpointPicksContentTypeFromExtension() throws Exception {
        when(imageStorageService.loadFile("pic.png")).thenReturn(new byte[]{1, 2, 3});
        when(imageStorageService.loadFile("missing.jpg")).thenThrow(new BadRequestException("File not found: missing.jpg"));

        mockMvc.perform(get("/api/products/images/pic.png"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
        mockMvc.perform(get("/api/products/images/missing.jpg"))
                .andExpect(status().isBadRequest());
    }
}
