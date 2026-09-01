package com.resume.platform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.resume.platform.dto.ResumeSearchVO;
import com.resume.platform.entity.Resume;

import java.util.List;

/**
 * 简历服务接口
 *
 * @author system
 */
public interface ResumeService extends IService<Resume> {

    /**
     * 根据用户ID查询简历
     *
     * @param userId 用户ID
     * @return 简历实体
     */
    Resume getResumeByUserId(Long userId);

    /**
     * 创建或更新用户简历（按userId判重）
     *
     * @param resume 简历数据
     * @return 保存后的简历实体
     */
    Resume createOrUpdateResume(Resume resume);

    /**
     * 搜索简历（企业级方案B：FULLTEXT全文索引 + LIKE兜底）
     *
     * 策略：
     * 1. 姓名精准匹配优先级最高（得分100）
     * 2. 职位/技能/简介按权重打分（30/20/10），支持"Java工程师"等中文关键词
     * 3. 全文索引无结果时自动降级LIKE模糊查询
     *
     * @param keyword 关键词（姓名或职位/技能，已去除首尾空白）
     * @param page    页码（从1开始）
     * @param size    每页条数
     * @return 搜索结果列表（按相关性降序）
     */
    List<ResumeSearchVO> searchResumes(String keyword, int page, int size);

    /**
     * 统计搜索命中总数（与searchResumes同口径，含降级逻辑）
     *
     * @param keyword 关键词
     * @return 命中条数
     */
    long countSearchResults(String keyword);
}
