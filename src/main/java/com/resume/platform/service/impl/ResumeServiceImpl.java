package com.resume.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.resume.platform.dto.ResumeSearchVO;
import com.resume.platform.entity.Resume;
import com.resume.platform.mapper.ResumeMapper;
import com.resume.platform.service.ResumeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

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

    /**
     * 关键词最大长度（防止超长输入拖垮查询）
     */
    private static final int KEYWORD_MAX_LENGTH = 50;

    /**
     * 每页最大条数（防止恶意大分页）
     */
    private static final int PAGE_SIZE_MAX = 20;

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

    @Override
    public List<ResumeSearchVO> searchResumes(String keyword, int page, int size) {
        String kw = sanitizeKeyword(keyword);
        if (kw == null) {
            return Collections.emptyList();
        }
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), PAGE_SIZE_MAX);
        int offset = (safePage - 1) * safeSize;

        // 主查询：FULLTEXT 全文索引（ngram 中文分词 + 加权打分）
        List<ResumeSearchVO> results = this.baseMapper.searchByFulltext(kw, offset, safeSize);

        // 降级策略：全文索引无结果时（如 ngram 未收录的低频词）→ LIKE 模糊兜底
        if (results.isEmpty()) {
            results = this.baseMapper.searchByLike(kw, offset, safeSize);
            logger.info("搜索降级为LIKE模糊匹配: keyword={}, hits={}", kw, results.size());
        }
        logger.info("简历搜索: keyword={}, page={}, size={}, hits={}", kw, safePage, safeSize, results.size());
        return results;
    }

    @Override
    public long countSearchResults(String keyword) {
        String kw = sanitizeKeyword(keyword);
        if (kw == null) {
            return 0;
        }
        // 与 searchResumes 同口径：全文优先，无结果降级 LIKE
        long count = this.baseMapper.countByFulltext(kw);
        if (count == 0) {
            count = this.baseMapper.countByLike(kw);
        }
        return count;
    }

    /**
     * 关键词清洗：去首尾空白，长度校验
     *
     * @param keyword 原始关键词
     * @return 合法关键词；为空或超长返回 null
     */
    private String sanitizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String kw = keyword.trim();
        if (kw.isEmpty() || kw.length() > KEYWORD_MAX_LENGTH) {
            return null;
        }
        return kw;
    }
}
