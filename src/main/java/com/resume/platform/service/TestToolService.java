package com.resume.platform.service;

import com.resume.platform.dto.ProxyRequestDTO;
import com.resume.platform.dto.TestCaseSaveDTO;
import com.resume.platform.entity.TestCase;

import java.util.List;
import java.util.Map;

/**
 * 测试工具服务
 * 提供接口转发代理、网页代理、测试用例CRUD、测试记录与MD报告生成。
 *
 * @author system
 */
public interface TestToolService {

    /**
     * 转发HTTP请求（绕过浏览器CORS）
     *
     * @param dto 代理请求
     * @return 响应信息：status/statusText/timeMs/headers/body/size
     */
    Map<String, Object> proxyRequest(ProxyRequestDTO dto);

    /**
     * 代理外部网页，剥离X-Frame-Options/CSP以便iframe加载并支持DOM检查
     *
     * @param url 目标网址
     * @return 响应信息：status/headers/body(html文本)/finalUrl
     */
    Map<String, Object> proxyPage(String url);

    /**
     * 保存测试用例（新增或更新）
     *
     * @param userId 用户ID
     * @param dto    用例数据
     * @return 保存后的用例
     */
    TestCase saveCase(Long userId, TestCaseSaveDTO dto);

    /**
     * 查询用户测试用例列表
     *
     * @param userId 用户ID
     * @return 用例列表
     */
    List<TestCase> listCases(Long userId);

    /**
     * 删除测试用例
     *
     * @param userId 用户ID
     * @param caseId 用例ID
     * @return 是否删除
     */
    boolean deleteCase(Long userId, Long caseId);

    /**
     * 执行单条用例并落库记录
     *
     * @param userId 用户ID
     * @param caseId 用例ID
     * @return 执行结果：passed/status/elapsedMs/errorMsg/responsePreview
     */
    Map<String, Object> runCase(Long userId, Long caseId);

    /**
     * 批量执行用户全部用例并生成MD报告
     *
     * @param userId 用户ID
     * @return 报告信息：report(markdown文本)/total/passed/failed
     */
    Map<String, Object> runAllAndReport(Long userId);
}
