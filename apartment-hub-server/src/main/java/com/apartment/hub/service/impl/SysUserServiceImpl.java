package com.apartment.hub.service.impl;

import com.apartment.hub.common.BusinessException;
import com.apartment.hub.common.ResultCode;
import com.apartment.hub.dto.LoginDTO;
import com.apartment.hub.entity.SysUser;
import com.apartment.hub.entity.SysUserRole;
import com.apartment.hub.mapper.SysUserMapper;
import com.apartment.hub.mapper.SysUserRoleMapper;
import com.apartment.hub.security.JwtUtil;
import com.apartment.hub.security.LoginUser;
import com.apartment.hub.service.SysUserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final SysUserRoleMapper userRoleMapper;

    @Override
    public Map<String, Object> login(LoginDTO dto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword()));

        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        List<String> roles = baseMapper.selectRoleCodesByUserId(loginUser.getUserId());

        String token = jwtUtil.generateToken(loginUser.getUserId(), loginUser.getUsername(), roles);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", loginUser.getUserId());
        result.put("username", loginUser.getUsername());
        return result;
    }

    @Override
    public Map<String, Object> getUserInfo(Long userId) {
        SysUser user = getById(userId);
        if (user == null) {
            throw new BusinessException("User not found");
        }
        user.setPassword(null);

        List<String> roles = baseMapper.selectRoleCodesByUserId(userId);
        List<String> permissions = baseMapper.selectPermissionCodesByUserId(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("user", user);
        result.put("roles", roles);
        result.put("permissions", permissions);
        return result;
    }

    @Override
    public List<Long> getUserRoleIds(Long userId) {
        return userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId))
                .stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList());
    }
}
