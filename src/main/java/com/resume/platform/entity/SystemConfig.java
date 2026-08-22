package com.resume.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统配置实体
 * 遵循阿里巴巴Java开发手册：
 * - POJO类必须实现Serializable接口
 * - 必须显式声明serialVersionUID
 *
 * @author system
 */
@Data
@TableName("system_config")
public class SystemConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 配置ID（自增主键）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 配置键（唯一索引），如 guest_can_view_home
     */
    private String configKey;

    /**
     * 配置值，如 true/false
     */
    private String configValue;

    /**
     * 配置描述说明
     */
    private String description;

    /**
     * 最后更新时间
     */
    private LocalDateTime updateTime;
}
