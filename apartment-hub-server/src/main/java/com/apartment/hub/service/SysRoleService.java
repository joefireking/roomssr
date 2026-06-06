package com.apartment.hub.service;

import com.apartment.hub.entity.SysRole;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

public interface SysRoleService extends IService<SysRole> {
    void assignPermissions(Long roleId, List<Long> permissionIds);
    List<Long> getRolePermissionIds(Long roleId);
}
