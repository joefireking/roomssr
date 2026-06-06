package com.apartment.hub.service.impl;

import com.apartment.hub.entity.Tenant;
import com.apartment.hub.mapper.TenantMapper;
import com.apartment.hub.service.TenantService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class TenantServiceImpl extends ServiceImpl<TenantMapper, Tenant> implements TenantService {
}
