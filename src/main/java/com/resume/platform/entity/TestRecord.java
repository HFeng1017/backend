package com.resume.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 测试执行记录实体
 * 每次执行测试用例后落库一条记录，用于生成MD测试报告。
 *
 * @author system
 */
@Data
@TableName("test_record")
public class TestRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 执行用户ID
     */
    private Long userId;

    /**
     * 关联用例ID
     */
    private Long caseId;

    /**
     * 用例名称
     */
    private String caseName;

    /**
     * HTTP方法
     */
    private String method;

    /**
     * 请求URL
     */
    private String url;

    /**
     * 实际HTTP状态码
     */
    private Integer status;

    /**
     * 耗时（毫秒）
     */
    private Long elapsedMs;

    /**
     * 是否通过：1-通过 0-失败
     */
    private Integer passed;

    /**
     * 失败原因
     */
    private String errorMsg;

    /**
     * 响应预览（截断）
     */
    private String responsePreview;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
