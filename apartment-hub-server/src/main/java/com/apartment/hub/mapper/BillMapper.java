package com.apartment.hub.mapper;

import com.apartment.hub.entity.Bill;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface BillMapper extends BaseMapper<Bill> {

    @Select("SELECT DATE_FORMAT(billing_month, '%Y-%m') as month, SUM(amount) as total " +
            "FROM bill WHERE deleted = 0 AND status = 1 " +
            "AND billing_month >= #{startMonth} AND billing_month <= #{endMonth} " +
            "GROUP BY DATE_FORMAT(billing_month, '%Y-%m') ORDER BY month")
    List<Map<String, Object>> revenueByMonth(@Param("startMonth") String startMonth,
                                              @Param("endMonth") String endMonth);
}
