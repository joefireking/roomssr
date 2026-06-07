package com.apartment.hub.service.impl;

import com.apartment.hub.common.BusinessException;
import com.apartment.hub.common.ResultCode;
import com.apartment.hub.dto.PaymentDTO;
import com.apartment.hub.entity.Bill;
import com.apartment.hub.entity.Contract;
import com.apartment.hub.entity.Payment;
import com.apartment.hub.enums.BillStatus;
import com.apartment.hub.enums.BillType;
import com.apartment.hub.mapper.BillMapper;
import com.apartment.hub.service.BillService;
import com.apartment.hub.service.PaymentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.apartment.hub.util.NoGenerator;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BillServiceImpl extends ServiceImpl<BillMapper, Bill> implements BillService {

    private final PaymentService paymentService;

    @Override
    @Transactional
    public void generateBills(Contract contract) {
        List<Bill> bills = new ArrayList<>();

        // Generate deposit bill
        Bill depositBill = new Bill();
        depositBill.setBillNo(NoGenerator.billNo());
        depositBill.setContractId(contract.getId());
        depositBill.setTenantId(contract.getTenantId());
        depositBill.setRoomId(contract.getRoomId());
        depositBill.setBillType(BillType.DEPOSIT);
        depositBill.setAmount(contract.getDepositAmount());
        depositBill.setBillingMonth(contract.getStartDate().format(DateTimeFormatter.ofPattern("yyyy-MM")));
        depositBill.setDueDate(contract.getStartDate());
        depositBill.setStatus(BillStatus.PENDING);
        bills.add(depositBill);

        // Generate rent bills for each payment cycle
        LocalDate current = contract.getStartDate();
        int cycle = contract.getPaymentCycle();
        while (!current.isAfter(contract.getEndDate())) {
            Bill rentBill = new Bill();
            rentBill.setBillNo(NoGenerator.billNo());
            rentBill.setContractId(contract.getId());
            rentBill.setTenantId(contract.getTenantId());
            rentBill.setRoomId(contract.getRoomId());
            rentBill.setBillType(BillType.RENT);
            rentBill.setAmount(contract.getRentAmount());
            rentBill.setBillingMonth(current.format(DateTimeFormatter.ofPattern("yyyy-MM")));
            rentBill.setDueDate(current);
            rentBill.setStatus(BillStatus.PENDING);
            bills.add(rentBill);

            current = current.plusMonths(cycle);
        }

        saveBatch(bills);
    }

    @Override
    @Transactional
    public void payBill(PaymentDTO dto, Long operatorId) {
        Bill bill = getById(dto.getBillId());
        if (bill == null) {
            throw new BusinessException(ResultCode.BILL_NOT_FOUND);
        }
        if (bill.getStatus() == BillStatus.PAID) {
            throw new BusinessException(ResultCode.BILL_ALREADY_PAID);
        }
        if (bill.getStatus() == BillStatus.CANCELLED) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }

        // Validate payment amount matches bill amount
        if (dto.getAmount().compareTo(bill.getAmount()) != 0) {
            throw new BusinessException(ResultCode.BILL_AMOUNT_MISMATCH);
        }

        // Concurrent-safe update: only mark PAID if still PENDING/OVERDUE
        boolean updated = lambdaUpdate()
                .eq(Bill::getId, dto.getBillId())
                .in(Bill::getStatus, BillStatus.PENDING, BillStatus.OVERDUE)
                .set(Bill::getStatus, BillStatus.PAID)
                .set(Bill::getPaidTime, LocalDateTime.now())
                .update();
        if (!updated) {
            throw new BusinessException(ResultCode.BILL_STATUS_CHANGED);
        }

        // Create payment record
        Payment payment = new Payment();
        payment.setPaymentNo(NoGenerator.paymentNo());
        payment.setBillId(dto.getBillId());
        payment.setTenantId(bill.getTenantId());
        payment.setAmount(dto.getAmount());
        com.apartment.hub.enums.PaymentMethod[] methods = com.apartment.hub.enums.PaymentMethod.values();
        if (dto.getPaymentMethod() < 0 || dto.getPaymentMethod() >= methods.length) {
            throw new BusinessException(ResultCode.INVALID_PAYMENT_METHOD);
        }
        payment.setPaymentMethod(methods[dto.getPaymentMethod()]);
        payment.setPaymentTime(LocalDateTime.now());
        payment.setRemark(dto.getRemark());
        payment.setOperatorId(operatorId);
        paymentService.save(payment);
    }

    @Override
    @Transactional
    public void checkOverdue() {
        lambdaUpdate()
                .eq(Bill::getStatus, BillStatus.PENDING)
                .lt(Bill::getDueDate, LocalDate.now())
                .set(Bill::getStatus, BillStatus.OVERDUE)
                .update();
    }

    @Override
    public List<Map<String, Object>> revenueByMonth(String startMonth, String endMonth) {
        return baseMapper.revenueByMonth(startMonth, endMonth);
    }

    @Override
    public BigDecimal getTotalPaidAmount() {
        return lambdaQuery()
                .eq(Bill::getStatus, BillStatus.PAID)
                .list()
                .stream()
                .map(Bill::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal getTotalPendingAmount() {
        return lambdaQuery()
                .eq(Bill::getStatus, BillStatus.PENDING)
                .list()
                .stream()
                .map(Bill::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal getTotalOverdueAmount() {
        return lambdaQuery()
                .eq(Bill::getStatus, BillStatus.OVERDUE)
                .list()
                .stream()
                .map(Bill::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
