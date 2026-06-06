package com.apartment.hub.service.impl;

import com.apartment.hub.entity.OperationLog;
import com.apartment.hub.mapper.OperationLogMapper;
import com.apartment.hub.service.OperationLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements OperationLogService {
}
