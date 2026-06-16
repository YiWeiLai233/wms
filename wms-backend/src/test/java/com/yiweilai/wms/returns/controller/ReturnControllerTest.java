package com.yiweilai.wms.returns.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yiweilai.wms.common.PageResult;
import com.yiweilai.wms.express.mapper.ExpressCompanyMapper;
import com.yiweilai.wms.express.mapper.ExpressFeeStepMapper;
import com.yiweilai.wms.express.mapper.ExpressFeeTemplateMapper;
import com.yiweilai.wms.order.mapper.SalesOrderItemMapper;
import com.yiweilai.wms.order.mapper.SalesOrderMapper;
import com.yiweilai.wms.platform.mapper.PlatformMapper;
import com.yiweilai.wms.returns.dto.ReturnBatchCreateDTO;
import com.yiweilai.wms.returns.dto.ReturnCheckDTO;
import com.yiweilai.wms.returns.dto.ReturnConfirmDTO;
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
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReturnControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void confirmAcceptsReturnIdJsonObject() throws Exception {
        CapturingReturnService returnService = new CapturingReturnService();
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(controller(returnService))
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
                .standaloneSetup(controller(returnService))
                .build();

        mockMvc.perform(post("/api/returns/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "orderId", 7L,
                                "reason", "damaged",
                                "items", java.util.List.of()))))
                .andExpect(status().isBadRequest());
    }

    private ReturnController controller(ReturnService returnService) {
        return new ReturnController(
                returnService,
                mock(PlatformMapper.class),
                mock(ExpressFeeTemplateMapper.class),
                mock(ExpressFeeStepMapper.class),
                mock(SalesOrderMapper.class),
                mock(SalesOrderItemMapper.class),
                mock(ExpressCompanyMapper.class));
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
        public java.util.List<Long> createBatch(ReturnBatchCreateDTO dto) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void check(ReturnCheckDTO dto) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void confirm(Long returnId, java.util.List<ReturnConfirmDTO.ReturnConfirmItemDTO> items) {
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
