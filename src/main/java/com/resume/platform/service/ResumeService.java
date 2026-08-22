package com.resume.platform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.resume.platform.entity.Resume;

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
}
