package com.apartment.hub.entity;

import com.apartment.hub.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("announcement")
public class Announcement extends BaseEntity {
    private String title;
    private String content;
    private String summary;
    private Long publisherId;
    private Integer status;
    private Integer topFlag;
}
