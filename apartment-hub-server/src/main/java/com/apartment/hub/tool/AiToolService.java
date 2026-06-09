package com.apartment.hub.tool;

import com.apartment.hub.entity.*;
import com.apartment.hub.enums.BillStatus;
import com.apartment.hub.enums.ContractStatus;
import com.apartment.hub.enums.RoomStatus;
import com.apartment.hub.service.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiToolService {

    private final RoomService roomService;
    private final ContractService contractService;
    private final BillService billService;
    private final TenantService tenantService;
    private final BuildingService buildingService;
    private final PaymentService paymentService;
    private final ApartmentService apartmentService;

    // ==================== 查询类工具 ====================

    /**
     * 按房间号查询房间状态
     */
    public Map<String, Object> getRoomInfo(String roomNumber) {
        log.info("AI Tool: getRoomInfo({})", roomNumber);

        Room room = roomService.getOne(
                new LambdaQueryWrapper<Room>().eq(Room::getRoomNumber, roomNumber));
        if (room == null) {
            return Map.of("found", false, "message", "未找到房间号 " + roomNumber);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("found", true);
        result.put("roomNumber", room.getRoomNumber());
        result.put("floor", room.getFloor());
        result.put("rentPrice", room.getRentPrice());
        result.put("status", room.getStatus().getDescription());

        // 查询楼栋
        if (room.getBuildingId() != null) {
            Building building = buildingService.getById(room.getBuildingId());
            if (building != null) result.put("building", building.getName());
        }

        // 查询当前有效合同和租客
        Contract activeContract = contractService.getOne(
                new LambdaQueryWrapper<Contract>()
                        .eq(Contract::getRoomId, room.getId())
                        .eq(Contract::getStatus, ContractStatus.ACTIVE));
        if (activeContract != null) {
            result.put("contractNo", activeContract.getContractNo());
            result.put("contractStart", activeContract.getStartDate().toString());
            result.put("contractEnd", activeContract.getEndDate().toString());
            result.put("depositAmount", activeContract.getDepositAmount());

            Tenant tenant = tenantService.getById(activeContract.getTenantId());
            if (tenant != null) {
                result.put("tenantName", tenant.getName());
                result.put("tenantPhone", tenant.getPhone());
            }
        }

        return result;
    }

    /**
     * 查询逾期账单
     */
    public Map<String, Object> getOverdueBills() {
        log.info("AI Tool: getOverdueBills()");

        List<Bill> overdueBills = billService.list(
                new LambdaQueryWrapper<Bill>().eq(Bill::getStatus, BillStatus.OVERDUE));
        if (overdueBills.isEmpty()) {
            return Map.of("count", 0, "message", "当前没有逾期账单");
        }

        BigDecimal totalOverdue = overdueBills.stream()
                .map(Bill::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Map<String, Object>> detailList = new ArrayList<>();
        for (Bill bill : overdueBills) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("billNo", bill.getBillNo());
            item.put("amount", bill.getAmount());
            item.put("billingMonth", bill.getBillingMonth());
            item.put("dueDate", bill.getDueDate().toString());

            Tenant tenant = tenantService.getById(bill.getTenantId());
            if (tenant != null) {
                item.put("tenantName", tenant.getName());
                item.put("tenantPhone", tenant.getPhone());
            }

            Room room = roomService.getById(bill.getRoomId());
            if (room != null) item.put("roomNumber", room.getRoomNumber());

            detailList.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", detailList.size());
        result.put("totalAmount", totalOverdue);
        result.put("bills", detailList);
        return result;
    }

    /**
     * 查询即将到期的合同
     */
    public Map<String, Object> getExpiringContracts(int withinDays) {
        log.info("AI Tool: getExpiringContracts({})", withinDays);

        LocalDate today = LocalDate.now();
        LocalDate deadline = today.plusDays(withinDays);

        List<Contract> expiring = contractService.list(
                new LambdaQueryWrapper<Contract>()
                        .eq(Contract::getStatus, ContractStatus.ACTIVE)
                        .between(Contract::getEndDate, today, deadline)
                        .orderByAsc(Contract::getEndDate));

        if (expiring.isEmpty()) {
            return Map.of("count", 0, "message",
                    "未来" + withinDays + "天内没有即将到期的合同（从" + today + "到" + deadline + "）");
        }

        List<Map<String, Object>> detailList = new ArrayList<>();
        for (Contract c : expiring) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("contractNo", c.getContractNo());
            item.put("endDate", c.getEndDate().toString());
            item.put("rentAmount", c.getRentAmount());
            item.put("daysRemaining", LocalDate.now().until(c.getEndDate()).getDays());

            Tenant tenant = tenantService.getById(c.getTenantId());
            if (tenant != null) {
                item.put("tenantName", tenant.getName());
                item.put("tenantPhone", tenant.getPhone());
            }

            Room room = roomService.getById(c.getRoomId());
            if (room != null) item.put("roomNumber", room.getRoomNumber());

            detailList.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", detailList.size());
        result.put("withinDays", withinDays);
        result.put("contracts", detailList);
        return result;
    }

    /**
     * 本月收入统计
     */
    public Map<String, Object> getMonthlyIncome() {
        log.info("AI Tool: getMonthlyIncome()");

        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        List<Map<String, Object>> revenueList = billService.revenueByMonth(currentMonth, currentMonth);

        BigDecimal totalPaid = billService.getTotalPaidAmount();
        BigDecimal totalPending = billService.getTotalPendingAmount();
        BigDecimal totalOverdue = billService.getTotalOverdueAmount();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("currentMonth", currentMonth);
        result.put("monthlyPaid", revenueList.isEmpty() ? BigDecimal.ZERO : revenueList.get(0).getOrDefault("amount", BigDecimal.ZERO));
        result.put("totalPaidAllTime", totalPaid);
        result.put("totalPending", totalPending);
        result.put("totalOverdue", totalOverdue);

        // 房间统计
        List<Map<String, Object>> statusCount = roomService.countByStatus();
        Map<String, Long> roomStats = new LinkedHashMap<>();
        for (Map<String, Object> sc : statusCount) {
            String statusName = String.valueOf(sc.get("status"));
            Number count = (Number) sc.get("count");
            if (statusName.equals("0")) roomStats.put("vacant", count.longValue());
            else if (statusName.equals("1")) roomStats.put("rented", count.longValue());
            else if (statusName.equals("2")) roomStats.put("maintenance", count.longValue());
            else if (statusName.equals("3")) roomStats.put("reserved", count.longValue());
        }
        result.put("roomStats", roomStats);

        return result;
    }

    /**
     * 按姓名查询租客信息
     */
    public Map<String, Object> getTenantInfo(String name) {
        log.info("AI Tool: getTenantInfo({})", name);

        List<Tenant> tenants = tenantService.list(
                new LambdaQueryWrapper<Tenant>().like(Tenant::getName, name));
        if (tenants.isEmpty()) {
            return Map.of("found", false, "message", "未找到姓名包含 \"" + name + "\" 的租客");
        }

        List<Map<String, Object>> detailList = new ArrayList<>();
        for (Tenant t : tenants) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", t.getName());
            item.put("phone", t.getPhone());
            item.put("gender", t.getGender() == 1 ? "男" : "女");
            item.put("tag", t.getTag());

            // 查当前合同
            Contract activeContract = contractService.getOne(
                    new LambdaQueryWrapper<Contract>()
                            .eq(Contract::getTenantId, t.getId())
                            .eq(Contract::getStatus, ContractStatus.ACTIVE));
            if (activeContract != null) {
                item.put("contractNo", activeContract.getContractNo());
                item.put("contractStart", activeContract.getStartDate().toString());
                item.put("contractEnd", activeContract.getEndDate().toString());
                Room room = roomService.getById(activeContract.getRoomId());
                if (room != null) item.put("roomNumber", room.getRoomNumber());
            }

            // 查欠费
            List<Bill> overdueBills = billService.list(
                    new LambdaQueryWrapper<Bill>()
                            .eq(Bill::getTenantId, t.getId())
                            .eq(Bill::getStatus, BillStatus.OVERDUE));
            if (!overdueBills.isEmpty()) {
                BigDecimal totalOwed = overdueBills.stream()
                        .map(Bill::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                item.put("overdueAmount", totalOwed);
            }

            detailList.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("found", true);
        result.put("count", detailList.size());
        result.put("tenants", detailList);
        return result;
    }
}
