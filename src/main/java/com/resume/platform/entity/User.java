package com.resume.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体
 * 遵循阿里巴巴Java开发手册：
 * - POJO类必须实现Serializable接口
 * - 必须显式声明serialVersionUID
 * - 类字段添加Javadoc说明业务含义
 *
 * @author system
 */
@Data
@TableName("user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID（自增主键）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 登录用户名（唯一）
     */
    private String username;

    /**
     * 登录密码（BCrypt加密存储）
     */
    private String password;

    /**
     * 邮箱地址（用于重置密码等）
     */
    private String email;

    /**
     * 手机号码
     */
    private String phone;

    /**
     * 用户角色：admin-管理员，user-普通用户
     */
    private String role;

    /**
     * 账号状态：1-启用，0-禁用
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
