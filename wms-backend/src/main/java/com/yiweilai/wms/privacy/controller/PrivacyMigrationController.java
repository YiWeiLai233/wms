package com.yiweilai.wms.privacy.controller;

import com.yiweilai.wms.common.Result;
import com.yiweilai.wms.exception.BusinessException;
import com.yiweilai.wms.exception.ErrorCode;
import com.yiweilai.wms.privacy.migration.PrivacyDataMigrationService;
import com.yiweilai.wms.privacy.migration.PrivacyMigrationResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
@RequestMapping("/api/privacy")
@RequiredArgsConstructor
public class PrivacyMigrationController {

    private final PrivacyDataMigrationService migrationService;

    @PostMapping("/migrate-sales-order")
    public Result<PrivacyMigrationResult> migrateSalesOrder(
            @RequestParam(defaultValue = "500") Integer batchSize,
            HttpServletRequest request) {
        if (!hasSuperAdminRole(request)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return Result.success(migrationService.migrateSalesOrders(batchSize == null ? 500 : batchSize));
    }

    private boolean hasSuperAdminRole(HttpServletRequest request) {
        Object roles = request.getAttribute("roles");
        if (roles instanceof Collection<?> collection) {
            return collection.stream().map(String::valueOf).anyMatch("SUPER_ADMIN"::equals);
        }
        return false;
    }
}
