package com.apartment.hub.service.impl;

import com.apartment.hub.entity.RoomType;
import com.apartment.hub.mapper.RoomTypeMapper;
import com.apartment.hub.service.RoomTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class RoomTypeServiceImpl extends ServiceImpl<RoomTypeMapper, RoomType> implements RoomTypeService {
}
