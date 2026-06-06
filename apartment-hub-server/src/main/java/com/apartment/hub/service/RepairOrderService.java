package com.apartment.hub.service;

import com.apartment.hub.entity.RepairOrder;
import com.baomidou.mybatisplus.extension.service.IService;

public interface RepairOrderService extends IService<RepairOrder> {
    void assignOrder(Long orderId, Long assigneeId);
    void startWork(Long orderId);
    void completeWork(Long orderId, String remark);
    void verifyOrder(Long orderId);
}
