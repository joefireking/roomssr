package com.apartment.hub.service.impl;

import com.apartment.hub.entity.SysPermission;
import com.apartment.hub.mapper.SysPermissionMapper;
import com.apartment.hub.service.SysPermissionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SysPermissionServiceImpl extends ServiceImpl<SysPermissionMapper, SysPermission> implements SysPermissionService {

    @Override
    public List<Map<String, Object>> getMenuTree() {
        List<SysPermission> all = list(new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getType, 0)
                .orderByAsc(SysPermission::getSortOrder));
        return buildTree(all, 0L);
    }

    private List<Map<String, Object>> buildTree(List<SysPermission> all, Long parentId) {
        return all.stream()
                .filter(p -> Objects.equals(p.getParentId(), parentId))
                .map(p -> {
                    Map<String, Object> node = new LinkedHashMap<>();
                    node.put("id", p.getId());
                    node.put("name", p.getPermissionName());
                    node.put("code", p.getPermissionCode());
                    node.put("path", p.getPath());
                    node.put("icon", p.getIcon());
                    node.put("children", buildTree(all, p.getId()));
                    return node;
                })
                .collect(Collectors.toList());
    }
}
