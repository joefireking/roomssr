package com.apartment.hub.service;

import com.apartment.hub.dto.LoginDTO;
import com.apartment.hub.entity.SysUser;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;
import java.util.Map;

public interface SysUserService extends IService<SysUser> {
    Map<String, Object> login(LoginDTO dto);
    Map<String, Object> getUserInfo(Long userId);
    List<Long> getUserRoleIds(Long userId);
}
