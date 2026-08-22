package com.resume.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 项目文件实体
 * 遵循阿里巴巴Java开发手册：
 * - POJO类必须实现Serializable接口
 * - 必须显式声明serialVersionUID
 *
 * @author system
 */
@Data
@TableName("project")
public class Project implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 项目ID（自增主键）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 项目/文件展示名称
     */
    private String name;

    /**
     * 文件描述（最大500字符）
     */
    private String description;

    /**
     * 文件访问URL，格式：/uploads/uuid.ext
     */
    private String fileUrl;

    /**
     * 文件类型：img/pdf/doc/excel/ppt/archive/text/other
     */
    private String fileType;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

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
