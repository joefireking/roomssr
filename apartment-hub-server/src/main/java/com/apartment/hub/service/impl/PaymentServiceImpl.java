package com.apartment.hub.service.impl;

import com.apartment.hub.entity.Payment;
import com.apartment.hub.mapper.PaymentMapper;
import com.apartment.hub.service.PaymentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class PaymentServiceImpl extends ServiceImpl<PaymentMapper, Payment> implements PaymentService {
}
