package com.resume.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.resume.platform.entity.UserPermission;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户级权限开关 Mapper
 *
 * @author system
 */
@Mapper
public interface UserPermissionMapper extends BaseMapper<UserPermission> {
}
