package com.yiweilai.wms.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yiweilai.wms.common.Constants;
import com.yiweilai.wms.config.CacheService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class JwtAuthFilterTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validJwtAuthenticatesRequestInSecurityContext() throws Exception {
        JwtUtils jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "secret", "YiCangWms2026SecretKeyForJwtTokenGenerationMustBeLongEnough");
        ReflectionTestUtils.setField(jwtUtils, "expiration", 86400000L);
        String token = jwtUtils.generateToken(1L, "admin", List.of("SUPER_ADMIN"));

        JwtAuthFilter filter = new JwtAuthFilter(jwtUtils, new ObjectMapper(), mock(CacheService.class));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/reports/dashboard");
        request.addHeader(Constants.TOKEN_HEADER, Constants.TOKEN_PREFIX + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isNotEqualTo(401);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().isAuthenticated()).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("admin");
    }

    @Test
    void optionsRequestPassesWithoutJwt() throws Exception {
        JwtUtils jwtUtils = new JwtUtils();
        JwtAuthFilter filter = new JwtAuthFilter(jwtUtils, new ObjectMapper(), mock(CacheService.class));
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/reports/dashboard");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isNotEqualTo(401);
        assertThat(filterChain.getRequest()).isSameAs(request);
    }

    @Test
    void imageResourceRequestPassesWithoutJwt() throws Exception {
        JwtUtils jwtUtils = new JwtUtils();
        JwtAuthFilter filter = new JwtAuthFilter(jwtUtils, new ObjectMapper(), mock(CacheService.class));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/images/uploaded.jpg");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isNotEqualTo(401);
        assertThat(filterChain.getRequest()).isSameAs(request);
    }

    @Test
    void aiServiceTokenAuthenticatesOnlyPendingActionCreation() throws Exception {
        JwtUtils jwtUtils = new JwtUtils();
        JwtAuthFilter filter = new JwtAuthFilter(jwtUtils, new ObjectMapper(), mock(CacheService.class));
        ReflectionTestUtils.setField(filter, "aiServiceToken", "secret-ai-token");

        MockHttpServletRequest createRequest = new MockHttpServletRequest("POST", "/api/ai/actions/pending");
        createRequest.addHeader("X-AI-Service-Token", "secret-ai-token");
        MockHttpServletResponse createResponse = new MockHttpServletResponse();
        MockFilterChain createChain = new MockFilterChain();

        filter.doFilterInternal(createRequest, createResponse, createChain);

        assertThat(createResponse.getStatus()).isNotEqualTo(401);
        assertThat(createChain.getRequest()).isSameAs(createRequest);
        assertThat(createRequest.getAttribute("username")).isEqualTo("wms-ai-service");

        SecurityContextHolder.clearContext();

        MockHttpServletRequest confirmRequest = new MockHttpServletRequest("POST", "/api/ai/actions/1001/confirm");
        confirmRequest.addHeader("X-AI-Service-Token", "secret-ai-token");
        MockHttpServletResponse confirmResponse = new MockHttpServletResponse();

        filter.doFilterInternal(confirmRequest, confirmResponse, new MockFilterChain());

        assertThat(confirmResponse.getStatus()).isEqualTo(401);
    }
}
