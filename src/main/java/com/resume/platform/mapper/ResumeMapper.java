package com.resume.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.resume.platform.entity.Resume;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ResumeMapper extends BaseMapper<Resume> {
}
