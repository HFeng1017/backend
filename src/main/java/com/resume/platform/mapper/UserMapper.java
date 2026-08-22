package com.resume.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.resume.platform.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
