package com.apartment.hub.service.impl;

import com.apartment.hub.common.BusinessException;
import com.apartment.hub.entity.RepairOrder;
import com.apartment.hub.enums.RepairStatus;
import com.apartment.hub.mapper.RepairOrderMapper;
import com.apartment.hub.service.RepairOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class RepairOrderServiceImpl extends ServiceImpl<RepairOrderMapper, RepairOrder> implements RepairOrderService {

    @Override
    @Transactional
    public void assignOrder(Long orderId, Long assigneeId) {
        RepairOrder order = getById(orderId);
        if (order == null || order.getStatus() != RepairStatus.PENDING) {
            throw new BusinessException("Order not found or not in pending status");
        }
        order.setAssigneeId(assigneeId);
        order.setStatus(RepairStatus.ASSIGNED);
        updateById(order);
    }

    @Override
    @Transactional
    public void startWork(Long orderId) {
        RepairOrder order = getById(orderId);
        if (order == null || order.getStatus() != RepairStatus.ASSIGNED) {
            throw new BusinessException("Order not found or not assigned");
        }
        order.setStatus(RepairStatus.IN_PROGRESS);
        updateById(order);
    }

    @Override
    @Transactional
    public void completeWork(Long orderId, String remark) {
        RepairOrder order = getById(orderId);
        if (order == null || order.getStatus() != RepairStatus.IN_PROGRESS) {
            throw new BusinessException("Order not found or not in progress");
        }
        order.setStatus(RepairStatus.COMPLETED);
        order.setResolveRemark(remark);
        order.setResolveTime(LocalDateTime.now());
        updateById(order);
    }

    @Override
    @Transactional
    public void verifyOrder(Long orderId) {
        RepairOrder order = getById(orderId);
        if (order == null || order.getStatus() != RepairStatus.COMPLETED) {
            throw new BusinessException("Order not found or not completed");
        }
        order.setStatus(RepairStatus.VERIFIED);
        updateById(order);
    }
}
