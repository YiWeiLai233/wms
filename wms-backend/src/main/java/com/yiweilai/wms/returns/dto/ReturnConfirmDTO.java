package com.yiweilai.wms.returns.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReturnConfirmDTO {

    @NotNull(message = "Return order ID cannot be null")
    private Long returnId;
}
