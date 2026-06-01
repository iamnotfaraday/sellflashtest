package com.faraday.flashsell.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.faraday.flashsell.model.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
