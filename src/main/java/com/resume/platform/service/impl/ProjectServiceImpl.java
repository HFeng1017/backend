package com.resume.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.resume.platform.entity.Project;
import com.resume.platform.mapper.ProjectMapper;
import com.resume.platform.service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 项目文件服务实现类
 * 注意：由于 MyBatis-Plus ServiceImpl 基类已定义 protected Log log，
 * 此处显式声明 SLF4J Logger 并命名为 logger，避免字段冲突且支持占位符参数
 *
 * @author system
 */
@Service
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements ProjectService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectServiceImpl.class);

    /**
     * 数据正常状态
     */
    private static final int STATUS_NORMAL = 1;

    @Override
    public List<Project> getProjectsByUserId(Long userId) {
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Project::getUserId, userId);
        wrapper.orderByDesc(Project::getCreateTime);
        List<Project> projects = this.list(wrapper);
        logger.debug("查询用户项目列表: userId={}, count={}", userId, projects.size());
        return projects;
    }

    @Override
    public Project createProject(Project project) {
        project.setCreateTime(LocalDateTime.now());
        project.setUpdateTime(LocalDateTime.now());
        if (project.getStatus() == null) {
            project.setStatus(STATUS_NORMAL);
        }
        this.save(project);
        logger.info("项目创建成功: projectId={}, userId={}", project.getId(), project.getUserId());
        return project;
    }

    @Override
    public boolean deleteProject(Long id) {
        boolean removed = this.removeById(id);
        if (removed) {
            logger.info("项目删除成功: projectId={}", id);
        } else {
            logger.warn("项目删除失败, 记录不存在: projectId={}", id);
        }
        return removed;
    }

    @Override
    public Project updateProject(Project project) {
        project.setUpdateTime(LocalDateTime.now());
        this.updateById(project);
        logger.info("项目更新成功: projectId={}", project.getId());
        return project;
    }
}
