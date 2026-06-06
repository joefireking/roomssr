package com.apartment.hub.entity;

import com.apartment.hub.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_permission")
public class SysPermission extends BaseEntity {
    private Long parentId;
    private String permissionName;
    private String permissionCode;
    private Integer type;
    private String path;
    private String icon;
    private Integer sortOrder;
}
