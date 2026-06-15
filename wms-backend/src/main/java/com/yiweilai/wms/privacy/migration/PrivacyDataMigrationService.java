package com.yiweilai.wms.privacy.migration;

import com.yiweilai.wms.order.entity.SalesOrder;
import com.yiweilai.wms.order.mapper.SalesOrderMapper;
import com.yiweilai.wms.privacy.crypto.PrivacyCryptoService;
import com.yiweilai.wms.privacy.crypto.PrivacyHashService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrivacyDataMigrationService {

    private final SalesOrderMapper salesOrderMapper;
    private final PrivacyCryptoService privacyCryptoService;
    private final PrivacyHashService privacyHashService;

    @Transactional(rollbackFor = Exception.class)
    public PrivacyMigrationResult migrateSalesOrders(int batchSize) {
        int safeBatchSize = Math.max(1, Math.min(batchSize, 1000));
        PrivacyMigrationResult result = new PrivacyMigrationResult();
        Long lastId = 0L;

        while (true) {
            List<SalesOrder> batch = salesOrderMapper.findPrivacyMigrationBatch(lastId, safeBatchSize);
            if (batch.isEmpty()) {
                return result;
            }
            for (SalesOrder order : batch) {
                lastId = order.getId();
                result.setScanned(result.getScanned() + 1);
                try {
                    protectReceiverFields(order);
                    salesOrderMapper.updatePrivacyFields(order);
                    result.setMigrated(result.getMigrated() + 1);
                } catch (RuntimeException e) {
                    result.setFailed(result.getFailed() + 1);
                    result.getFailedOrderIds().add(order.getId());
                    log.warn("Privacy migration failed for sales order id {}, reason {}",
                            order.getId(), e.getClass().getSimpleName());
                }
            }
        }
    }

    private void protectReceiverFields(SalesOrder order) {
        String receiverName = privacyCryptoService.decrypt(order.getReceiverName());
        String receiverPhone = privacyCryptoService.decrypt(order.getReceiverPhone());
        String receiverAddress = privacyCryptoService.decrypt(order.getReceiverAddress());

        order.setReceiverNameHash(privacyHashService.hmacSha256(privacyHashService.normalizeName(receiverName)));
        order.setReceiverPhoneHash(privacyHashService.hmacSha256(privacyHashService.normalizePhone(receiverPhone)));
        order.setReceiverName(privacyCryptoService.encrypt(receiverName));
        order.setReceiverPhone(privacyCryptoService.encrypt(receiverPhone));
        order.setReceiverAddress(privacyCryptoService.encrypt(receiverAddress));
    }
}
