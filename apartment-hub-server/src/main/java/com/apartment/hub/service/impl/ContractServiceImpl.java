package com.apartment.hub.service.impl;

import com.apartment.hub.common.BusinessException;
import com.apartment.hub.common.ResultCode;
import com.apartment.hub.dto.CheckoutDTO;
import com.apartment.hub.dto.ContractCreateDTO;
import com.apartment.hub.entity.*;
import com.apartment.hub.enums.*;
import com.apartment.hub.mapper.ContractMapper;
import com.apartment.hub.service.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import com.apartment.hub.util.NoGenerator;

@Service
@RequiredArgsConstructor
public class ContractServiceImpl extends ServiceImpl<ContractMapper, Contract> implements ContractService {

    private final RoomService roomService;
    private final TenantService tenantService;
    private final BillService billService;

    @Override
    @Transactional
    public Contract createContract(ContractCreateDTO dto) {
        // Validate tenant exists
        Tenant tenant = tenantService.getById(dto.getTenantId());
        if (tenant == null) {
            throw new BusinessException(ResultCode.TENANT_NOT_FOUND);
        }

        // Validate dates
        if (!dto.getStartDate().isBefore(dto.getEndDate())) {
            throw new BusinessException("End date must be after start date");
        }

        // Check for overlapping contracts
        long overlap = count(new LambdaQueryWrapper<Contract>()
                .eq(Contract::getRoomId, dto.getRoomId())
                .eq(Contract::getStatus, ContractStatus.ACTIVE)
                .and(w -> w.le(Contract::getStartDate, dto.getEndDate())
                        .ge(Contract::getEndDate, dto.getStartDate())));
        if (overlap > 0) {
            throw new BusinessException("Room already has an active contract for this period");
        }

        // Concurrent-safe: atomically update room status from VACANT to RENTED
        boolean locked = roomService.lambdaUpdate()
                .eq(Room::getId, dto.getRoomId())
                .eq(Room::getStatus, RoomStatus.VACANT)
                .set(Room::getStatus, RoomStatus.RENTED)
                .update();
        if (!locked) {
            throw new BusinessException(ResultCode.ROOM_NOT_AVAILABLE);
        }

        // Create contract
        Contract contract = new Contract();
        contract.setContractNo(NoGenerator.contractNo());
        contract.setTenantId(dto.getTenantId());
        contract.setRoomId(dto.getRoomId());
        contract.setStartDate(dto.getStartDate());
        contract.setEndDate(dto.getEndDate());
        contract.setRentAmount(dto.getRentAmount());
        contract.setDepositAmount(dto.getDepositAmount());
        contract.setPaymentCycle(dto.getPaymentCycle() != null ? dto.getPaymentCycle() : 1);
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setRemark(dto.getRemark());
        save(contract);

        // Generate bills
        billService.generateBills(contract);

        return contract;
    }

    @Override
    @Transactional
    public void terminateContract(Long contractId, String reason) {
        Contract contract = getById(contractId);
        if (contract == null || contract.getStatus() != ContractStatus.ACTIVE) {
            throw new BusinessException(ResultCode.CONTRACT_NOT_FOUND);
        }
        contract.setStatus(ContractStatus.TERMINATED);
        contract.setTerminateReason(reason);
        updateById(contract);

        // Cancel pending bills
        billService.lambdaUpdate()
                .eq(Bill::getContractId, contract.getId())
                .in(Bill::getStatus, BillStatus.PENDING, BillStatus.OVERDUE)
                .set(Bill::getStatus, BillStatus.CANCELLED)
                .update();

        // Only reset room to VACANT if it's currently RENTED
        Room room = roomService.getById(contract.getRoomId());
        if (room != null && room.getStatus() == RoomStatus.RENTED) {
            room.setStatus(RoomStatus.VACANT);
            roomService.updateById(room);
        }
    }

    @Override
    @Transactional
    public BigDecimal checkout(CheckoutDTO dto) {
        Contract contract = getById(dto.getContractId());
        if (contract == null || contract.getStatus() != ContractStatus.ACTIVE) {
            throw new BusinessException(ResultCode.CONTRACT_NOT_FOUND);
        }

        // Calculate refund
        BigDecimal deposit = contract.getDepositAmount();
        BigDecimal damageCost = dto.getDamageCost() != null ? dto.getDamageCost() : BigDecimal.ZERO;
        BigDecimal penalty = dto.getPenaltyAmount() != null ? dto.getPenaltyAmount() : BigDecimal.ZERO;

        // Find unpaid bills for this contract
        BigDecimal unpaidAmount = billService.lambdaQuery()
                .eq(Bill::getContractId, contract.getId())
                .in(Bill::getStatus, BillStatus.PENDING, BillStatus.OVERDUE)
                .list()
                .stream()
                .map(Bill::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDeduction = damageCost.add(penalty).add(unpaidAmount);
        BigDecimal refund = deposit.subtract(totalDeduction);
        if (refund.compareTo(BigDecimal.ZERO) < 0) {
            refund = BigDecimal.ZERO;
        }

        // Terminate contract
        contract.setStatus(ContractStatus.TERMINATED);
        contract.setTerminateReason(dto.getTerminateReason());
        updateById(contract);

        // Cancel pending bills
        billService.lambdaUpdate()
                .eq(Bill::getContractId, contract.getId())
                .in(Bill::getStatus, BillStatus.PENDING, BillStatus.OVERDUE)
                .set(Bill::getStatus, BillStatus.CANCELLED)
                .update();

        // Only reset room to VACANT if it's currently RENTED
        Room room = roomService.getById(contract.getRoomId());
        if (room != null && room.getStatus() == RoomStatus.RENTED) {
            room.setStatus(RoomStatus.VACANT);
            roomService.updateById(room);
        }

        return refund;
    }
}
