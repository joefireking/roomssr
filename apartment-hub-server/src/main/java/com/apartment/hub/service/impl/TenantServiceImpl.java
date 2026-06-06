package com.apartment.hub.service.impl;

import com.apartment.hub.common.BusinessException;
import com.apartment.hub.entity.Contract;
import com.apartment.hub.entity.Tenant;
import com.apartment.hub.enums.ContractStatus;
import com.apartment.hub.mapper.TenantMapper;
import com.apartment.hub.service.ContractService;
import com.apartment.hub.service.TenantService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TenantServiceImpl extends ServiceImpl<TenantMapper, Tenant> implements TenantService {

    private final ContractService contractService;

    @Override
    public boolean removeById(java.io.Serializable id) {
        long activeContracts = contractService.lambdaQuery()
                .eq(Contract::getTenantId, id)
                .eq(Contract::getStatus, ContractStatus.ACTIVE)
                .count();
        if (activeContracts > 0) {
            throw new BusinessException("Cannot delete tenant with active contracts");
        }
        return super.removeById(id);
    }
}
