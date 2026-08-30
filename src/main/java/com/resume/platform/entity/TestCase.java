package com.resume.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 测试用例实体
 * 用户保存的接口/网页测试用例，支持断言预期状态码与响应文本。
 *
 * @author system
 */
@Data
@TableName("test_case")
public class TestCase implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 用例名称
     */
    private String name;

    /**
     * 用例类型：api-接口测试 / page-网页测试
     */
    private String type;

    /**
     * HTTP方法：GET/POST/PUT/DELETE/PATCH
     */
    private String method;

    /**
     * 请求URL
     */
    private String url;

    /**
     * 请求头（JSON字符串）
     */
    private String headers;

    /**
     * 查询参数（JSON字符串）
     */
    private String params;

    /**
     * 请求体
     */
    private String body;

    /**
     * 预期状态码
     */
    private Integer expectedStatus;

    /**
     * 预期响应包含文本（断言）
     */
    private String expectedBody;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
