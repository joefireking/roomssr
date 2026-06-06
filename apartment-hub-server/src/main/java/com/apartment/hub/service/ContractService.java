package com.apartment.hub.service;

import com.apartment.hub.dto.CheckoutDTO;
import com.apartment.hub.dto.ContractCreateDTO;
import com.apartment.hub.entity.Contract;
import com.baomidou.mybatisplus.extension.service.IService;
import java.math.BigDecimal;

public interface ContractService extends IService<Contract> {
    Contract createContract(ContractCreateDTO dto);
    void terminateContract(Long contractId, String reason);
    BigDecimal checkout(CheckoutDTO dto);
}
