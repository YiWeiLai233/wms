package com.yiweilai.wms.returns.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.returns.dto.ReturnCheckDTO;
import com.yiweilai.wms.returns.dto.ReturnCreateDTO;
import com.yiweilai.wms.returns.dto.ReturnQueryDTO;
import com.yiweilai.wms.returns.service.ReturnService;
import com.yiweilai.wms.returns.vo.ReturnOrderVO;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReturnControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void confirmAcceptsReturnIdJsonObject() throws Exception {
        CapturingReturnService returnService = new CapturingReturnService();
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new ReturnController(returnService))
                .build();

        mockMvc.perform(post("/api/returns/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("returnId", 42L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        assertThat(returnService.confirmedReturnId).isEqualTo(42L);
    }

    @Test
    void createRejectsEmptyReturnItems() throws Exception {
        CapturingReturnService returnService = new CapturingReturnService();
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new ReturnController(returnService))
                .build();

        mockMvc.perform(post("/api/returns/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "orderId", 7L,
                                "reason", "damaged",
                                "items", java.util.List.of()))))
                .andExpect(status().isBadRequest());
    }

    private static class CapturingReturnService implements ReturnService {
        private Long confirmedReturnId;

        @Override
        public PageResult<ReturnOrderVO> findByPage(ReturnQueryDTO query) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ReturnOrderVO getById(Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Long create(ReturnCreateDTO dto) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void check(ReturnCheckDTO dto) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void confirm(Long returnId) {
            confirmedReturnId = returnId;
        }

        @Override
        public void cancel(Long returnId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void cancelByOrderId(Long orderId) {
            throw new UnsupportedOperationException();
        }
    }
}
