package com.faraday.flashsell.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.faraday.flashsell.model.entity.OrderInfo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper extends BaseMapper<OrderInfo> {
}
