package com.resume.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.resume.platform.entity.TestCase;
import org.apache.ibatis.annotations.Mapper;

/**
 * 测试用例 Mapper
 *
 * @author system
 */
@Mapper
public interface TestCaseMapper extends BaseMapper<TestCase> {
}
