package com.resume.platform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.resume.platform.entity.Project;

import java.util.List;

/**
 * 项目文件服务接口
 *
 * @author system
 */
public interface ProjectService extends IService<Project> {

    /**
     * 根据用户ID查询项目文件列表（按创建时间倒序）
     *
     * @param userId 用户ID
     * @return 项目文件列表
     */
    List<Project> getProjectsByUserId(Long userId);

    /**
     * 创建新项目
     *
     * @param project 项目数据
     * @return 创建后的项目实体
     */
    Project createProject(Project project);

    /**
     * 根据ID删除项目
     *
     * @param id 项目ID
     * @return 是否删除成功
     */
    boolean deleteProject(Long id);

    /**
     * 更新项目信息
     *
     * @param project 项目数据（必须含ID）
     * @return 更新后的项目实体
     */
    Project updateProject(Project project);
}
