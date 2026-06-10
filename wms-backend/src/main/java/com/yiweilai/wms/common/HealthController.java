package com.yiweilai.wms.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 心跳检测
 */
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public String health() {
        return "helloWms";
    }
}
