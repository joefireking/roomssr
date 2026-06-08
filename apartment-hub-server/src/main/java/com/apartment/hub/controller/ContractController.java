package com.apartment.hub.controller;

import com.apartment.hub.aspect.OperationLog;
import com.apartment.hub.common.PageResult;
import com.apartment.hub.common.Result;
import com.apartment.hub.dto.CheckoutDTO;
import com.apartment.hub.dto.ContractCreateDTO;
import com.apartment.hub.entity.Contract;
import com.apartment.hub.entity.Room;
import com.apartment.hub.entity.Tenant;
import com.apartment.hub.enums.ContractStatus;
import com.apartment.hub.service.ContractService;
import com.apartment.hub.service.RoomService;
import com.apartment.hub.service.TenantService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;
    private final TenantService tenantService;
    private final RoomService roomService;

    @GetMapping("/list")
    public Result<PageResult<Contract>> list(
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        LambdaQueryWrapper<Contract> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(tenantId != null, Contract::getTenantId, tenantId)
                .eq(roomId != null, Contract::getRoomId, roomId)
                .eq(status != null, Contract::getStatus, status)
                .orderByDesc(Contract::getCreateTime);
        return Result.success(PageResult.from(contractService.page(new Page<>(current, size), wrapper)));
    }

    @GetMapping("/{id}")
    public Result<Contract> getById(@PathVariable Long id) {
        return Result.success(contractService.getById(id));
    }

    @PreAuthorize("hasAuthority('contract:create')")
    @OperationLog(module = "Contract Management", operation = "Create Contract")
    @PostMapping
    public Result<Contract> create(@Valid @RequestBody ContractCreateDTO dto) {
        return Result.success(contractService.createContract(dto));
    }

    @OperationLog(module = "Contract Management", operation = "Terminate Contract")
    @PutMapping("/{id}/terminate")
    public Result<Boolean> terminate(@PathVariable Long id, @RequestParam(required = false) String reason) {
        contractService.terminateContract(id, reason);
        return Result.success();
    }

    @OperationLog(module = "Contract Management", operation = "Checkout")
    @PostMapping("/checkout")
    public Result<BigDecimal> checkout(@Valid @RequestBody CheckoutDTO dto) {
        return Result.success(contractService.checkout(dto));
    }

    @GetMapping("/{id}/agreement")
    public Result<String> agreement(@PathVariable Long id) {
        return Result.success(buildContractHtml(id));
    }

    @GetMapping("/{id}/agreement/download")
    public ResponseEntity<byte[]> agreementDownload(@PathVariable Long id) {
        Contract contract = contractService.getById(id);
        if (contract == null) return ResponseEntity.notFound().build();
        Tenant tenant = tenantService.getById(contract.getTenantId());
        Room room = roomService.getById(contract.getRoomId());
        String fileName = "Lease_Contract_" + contract.getContractNo() + ".html";
        String html = buildContractHtml(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .contentType(MediaType.TEXT_HTML)
                .body(html.getBytes());
    }

    private String buildContractHtml(Long contractId) {
        Contract contract = contractService.getById(contractId);
        if (contract == null) return "<p>Contract not found</p>";
        Tenant tenant = tenantService.getById(contract.getTenantId());
        Room room = roomService.getById(contract.getRoomId());
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String tenantName = tenant != null ? tenant.getName() : "N/A";
        String tenantPhone = tenant != null ? tenant.getPhone() : "N/A";
        String tenantIdCard = tenant != null ? tenant.getIdCard() : "N/A";
        String roomNumber = room != null ? room.getRoomNumber() : "N/A";
        String rentPrice = contract.getRentAmount() != null ? contract.getRentAmount().toPlainString() : "0";
        String deposit = contract.getDepositAmount() != null ? contract.getDepositAmount().toPlainString() : "0";
        String startDate = contract.getStartDate() != null ? contract.getStartDate().format(fmt) : "N/A";
        String endDate = contract.getEndDate() != null ? contract.getEndDate().format(fmt) : "N/A";
        String cycleLabel = switch (contract.getPaymentCycle() != null ? contract.getPaymentCycle() : 1) {
            case 3 -> "Quarterly";
            case 12 -> "Yearly";
            default -> "Monthly";
        };
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head><meta charset="UTF-8"><title>Lease Agreement</title>
                <style>
                body{font-family:SimSun,serif;padding:40px 60px;color:#333;line-height:1.8}
                h1{text-align:center;font-size:24px;margin-bottom:30px}
                .clause{margin-bottom:12px;text-indent:2em}
                .sig-block{display:flex;justify-content:space-between;margin-top:60px}
                .sig-box{width:45%%;text-align:center}
                .sig-line{border-bottom:1px solid #333;height:40px;margin:10px 0}
                table{width:100%%;border-collapse:collapse;margin:16px 0}
                td,th{border:1px solid #ccc;padding:8px 12px;text-align:left}
                </style></head>
                <body>
                <h1>Residential Lease Agreement</h1>
                <p class="clause"><b>Contract No:</b> %s</p>
                <p class="clause"><b>Landlord (Party A):</b> ApartmentHub Property Management</p>
                <p class="clause"><b>Tenant (Party B):</b> %s &nbsp;&nbsp; Phone: %s &nbsp;&nbsp; ID: %s</p>
                <table>
                <tr><th>Item</th><th>Details</th></tr>
                <tr><td>Room</td><td>%s</td></tr>
                <tr><td>Lease Period</td><td>%s to %s</td></tr>
                <tr><td>Monthly Rent</td><td>&yen;%s</td></tr>
                <tr><td>Deposit</td><td>&yen;%s</td></tr>
                <tr><td>Payment Cycle</td><td>%s</td></tr>
                </table>
                <p class="clause"><b>Article 1:</b> Party A agrees to lease the above property to Party B for residential use.</p>
                <p class="clause"><b>Article 2:</b> Party B shall pay rent on time according to the agreed payment cycle. Late payment may incur a penalty.</p>
                <p class="clause"><b>Article 3:</b> The deposit shall be returned upon lease termination, minus any deductions for damages or unpaid bills.</p>
                <p class="clause"><b>Article 4:</b> Party B shall not sublease the property without written consent from Party A.</p>
                <p class="clause"><b>Article 5:</b> Party B shall maintain the property in good condition and report any necessary repairs promptly.</p>
                <p class="clause"><b>Article 6:</b> Either party may terminate this agreement with 30 days written notice, subject to applicable penalties.</p>
                <p class="clause"><b>Article 7:</b> This agreement is governed by the laws of the People's Republic of China.</p>
                <div class="sig-block">
                <div class="sig-box"><p>Party A (Seal)</p><div class="sig-line"></div><p>Date: _______________</p></div>
                <div class="sig-box"><p>Party B (Signature)</p><div class="sig-line"></div><p>Date: _______________</p></div>
                </div>
                </body></html>
                """.formatted(contract.getContractNo(), tenantName, tenantPhone, tenantIdCard,
                roomNumber, startDate, endDate, rentPrice, deposit, cycleLabel);
    }
}
