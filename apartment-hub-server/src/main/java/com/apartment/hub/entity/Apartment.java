package com.apartment.hub.entity;

import com.apartment.hub.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("apartment")
public class Apartment extends BaseEntity {
    private String name;
    private String address;
    private String city;
    private String district;
    private String contactName;
    private String contactPhone;
    private Integer totalBuildings;
    private String description;
    private Integer status;
}
