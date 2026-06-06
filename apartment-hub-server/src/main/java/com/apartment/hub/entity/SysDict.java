package com.apartment.hub.entity;

import com.apartment.hub.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict")
public class SysDict extends BaseEntity {
    private String dictType;
    private String dictCode;
    private String dictLabel;
    private Integer sortOrder;
    private Integer status;
}
