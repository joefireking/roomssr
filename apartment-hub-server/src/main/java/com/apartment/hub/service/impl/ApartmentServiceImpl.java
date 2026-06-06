package com.apartment.hub.service.impl;

import com.apartment.hub.entity.Apartment;
import com.apartment.hub.mapper.ApartmentMapper;
import com.apartment.hub.service.ApartmentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class ApartmentServiceImpl extends ServiceImpl<ApartmentMapper, Apartment> implements ApartmentService {
}
