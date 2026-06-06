package com.apartment.hub.service;

import com.apartment.hub.entity.SysPermission;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;
import java.util.Map;

public interface SysPermissionService extends IService<SysPermission> {
    List<Map<String, Object>> getMenuTree();
}
