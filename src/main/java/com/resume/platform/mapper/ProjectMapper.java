package com.resume.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.resume.platform.entity.Project;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProjectMapper extends BaseMapper<Project> {
}
