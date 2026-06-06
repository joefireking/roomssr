package com.apartment.hub.service.impl;

import com.apartment.hub.entity.Announcement;
import com.apartment.hub.mapper.AnnouncementMapper;
import com.apartment.hub.service.AnnouncementService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class AnnouncementServiceImpl extends ServiceImpl<AnnouncementMapper, Announcement> implements AnnouncementService {
}
