package com.apartment.hub.service;

import com.apartment.hub.dto.PaymentDTO;
import com.apartment.hub.entity.Bill;
import com.baomidou.mybatisplus.extension.service.IService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface BillService extends IService<Bill> {
    void generateBills(com.apartment.hub.entity.Contract contract);
    void payBill(PaymentDTO dto, Long operatorId);
    void checkOverdue();
    List<Map<String, Object>> revenueByMonth(String startMonth, String endMonth);
    BigDecimal getTotalPaidAmount();
    BigDecimal getTotalPendingAmount();
    BigDecimal getTotalOverdueAmount();
}
