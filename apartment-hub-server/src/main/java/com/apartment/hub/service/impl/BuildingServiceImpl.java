package com.apartment.hub.service.impl;

import com.apartment.hub.entity.Building;
import com.apartment.hub.mapper.BuildingMapper;
import com.apartment.hub.service.BuildingService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class BuildingServiceImpl extends ServiceImpl<BuildingMapper, Building> implements BuildingService {
}
