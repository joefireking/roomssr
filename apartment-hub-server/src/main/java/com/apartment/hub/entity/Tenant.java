package com.apartment.hub.entity;

import com.apartment.hub.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tenant")
public class Tenant extends BaseEntity {
    private String name;
    private Integer gender;
    private String phone;
    private String idCard;
    private String idCardFront;
    private String idCardBack;
    private String emergencyContact;
    private String emergencyPhone;
    private String tag;
    private String remark;
}
