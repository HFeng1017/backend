package com.resume.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.resume.platform.entity.TestRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 测试执行记录 Mapper
 *
 * @author system
 */
@Mapper
public interface TestRecordMapper extends BaseMapper<TestRecord> {
}
