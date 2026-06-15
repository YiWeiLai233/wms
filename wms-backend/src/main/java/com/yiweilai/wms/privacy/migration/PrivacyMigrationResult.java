package com.yiweilai.wms.privacy.migration;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PrivacyMigrationResult {

    private int scanned;

    private int migrated;

    private int failed;

    private List<Long> failedOrderIds = new ArrayList<>();
}
