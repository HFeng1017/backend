package com.resume.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.resume.platform.entity.Resume;
import com.resume.platform.mapper.ResumeMapper;
import com.resume.platform.service.ResumeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 简历服务实现类
 * 注意：由于 MyBatis-Plus ServiceImpl 基类已定义 protected Log log，
 * 此处显式声明 SLF4J Logger 并命名为 logger，避免字段冲突且支持占位符参数
 *
 * @author system
 */
@Service
public class ResumeServiceImpl extends ServiceImpl<ResumeMapper, Resume> implements ResumeService {

    private static final Logger logger = LoggerFactory.getLogger(ResumeServiceImpl.class);

    /**
     * 数据正常状态
     */
    private static final int STATUS_NORMAL = 1;

    @Override
    public Resume getResumeByUserId(Long userId) {
        LambdaQueryWrapper<Resume> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Resume::getUserId, userId);
        return this.getOne(wrapper);
    }

    @Override
    public Resume createOrUpdateResume(Resume resume) {
        Resume existing = getResumeByUserId(resume.getUserId());
        if (existing != null) {
            resume.setId(existing.getId());
            resume.setCreateTime(existing.getCreateTime());
            resume.setUpdateTime(LocalDateTime.now());
            this.updateById(resume);
            logger.info("简历更新成功: userId={}, resumeId={}", resume.getUserId(), resume.getId());
            return resume;
        }
        resume.setCreateTime(LocalDateTime.now());
        resume.setUpdateTime(LocalDateTime.now());
        resume.setStatus(STATUS_NORMAL);
        this.save(resume);
        logger.info("简历创建成功: userId={}, resumeId={}", resume.getUserId(), resume.getId());
        return resume;
    }
}
