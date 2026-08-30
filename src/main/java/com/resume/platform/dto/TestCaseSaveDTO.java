package com.resume.platform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 测试用例保存 DTO
 *
 * @author system
 */
@Data
public class TestCaseSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用例ID（更新时传，新增时不传）
     */
    private Long id;

    /**
     * 用例名称
     */
    @NotBlank(message = "用例名称不能为空")
    private String name;

    /**
     * 用例类型：api/page
     */
    private String type;

    /**
     * HTTP方法
     */
    private String method;

    /**
     * 请求URL
     */
    @NotBlank(message = "请求URL不能为空")
    private String url;

    /**
     * 请求头JSON
     */
    private String headers;

    /**
     * 查询参数JSON
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
     * 预期响应包含文本
     */
    private String expectedBody;
}
