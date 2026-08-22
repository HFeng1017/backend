package com.resume.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 简历实体
 * 遵循阿里巴巴Java开发手册：
 * - POJO类必须实现Serializable接口
 * - 必须显式声明serialVersionUID
 *
 * @author system
 */
@Data
@TableName("resume")
public class Resume implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 简历ID（自增主键）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 姓名
     */
    private String name;

    /**
     * 职位/求职意向
     */
    private String title;

    /**
     * 头像图片URL
     */
    private String avatar;

    /**
     * 电子邮箱
     */
    private String email;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 个人简介
     */
    private String introduction;

    /**
     * 技能标签（逗号分隔）
     */
    private String skills;

    /**
     * 工作经历（换行分隔）
     */
    private String experience;

    /**
     * 教育背景（换行分隔）
     */
    private String education;

    /**
     * 项目经验（换行分隔）
     */
    private String projects;

    /**
     * 状态：1-正常，0-删除
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
