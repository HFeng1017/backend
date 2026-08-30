package com.resume.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.platform.common.BusinessException;
import com.resume.platform.common.ErrorCode;
import com.resume.platform.dto.ProxyRequestDTO;
import com.resume.platform.dto.TestCaseSaveDTO;
import com.resume.platform.entity.TestCase;
import com.resume.platform.entity.TestRecord;
import com.resume.platform.mapper.TestCaseMapper;
import com.resume.platform.mapper.TestRecordMapper;
import com.resume.platform.service.TestToolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 测试工具服务实现
 * 使用 JDK 内置 java.net.http.HttpClient 转发请求以绕过浏览器 CORS，
 * 并持久化用例与执行记录、生成 Markdown 测试报告。
 *
 * @author system
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TestToolServiceImpl implements TestToolService {

    /**
     * 响应体预览最大长度（落库记录时截断，避免TEXT字段过大）
     */
    private static final int PREVIEW_MAX_LENGTH = 2000;

    /**
     * 单例 HttpClient（线程安全，复用连接池）
     */
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * 删除符号常量
     */
    private static final int STATUS_DELETED = 0;

    private final TestCaseMapper testCaseMapper;
    private final TestRecordMapper testRecordMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Object> proxyRequest(ProxyRequestDTO dto) {
        validateUrl(dto.getUrl());
        try {
            URI uri = buildUri(dto);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(dto.getTimeoutSeconds() != null ? dto.getTimeoutSeconds() : 30));

            // 附加请求头
            if (dto.getHeaders() != null) {
                dto.getHeaders().forEach(builder::header);
            }
            // 默认 UA
            builder.header("User-Agent", "Atelier-Test-Tool/1.0");

            String method = dto.getMethod().toUpperCase();
            String body = dto.getBody();
            String bodyPublisher = (body == null || body.isEmpty()) ? null : body;
            HttpRequest request = buildRequest(builder, method, bodyPublisher);
            long start = System.currentTimeMillis();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            long elapsed = System.currentTimeMillis() - start;

            Map<String, Object> result = new HashMap<>(8);
            result.put("status", response.statusCode());
            result.put("statusText", statusText(response.statusCode()));
            result.put("timeMs", elapsed);
            result.put("headers", responseHeaders(response));
            result.put("body", response.body());
            result.put("size", response.body() == null ? 0 : response.body().length());
            result.put("finalUrl", response.uri().toString());
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("代理请求失败: url={}, method={}, error={}", dto.getUrl(), dto.getMethod(), e.getMessage());
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "请求转发失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> proxyPage(String url) {
        validateUrl(url);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .header("User-Agent", "Atelier-Test-Tool/1.0 (page-proxy)")
                    .GET()
                    .build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String html = response.body();
            // 剥离阻止iframe内嵌与CSP指令，便于前端可视化加载并检查DOM
            String cleaned = stripFrameBusting(html);
            Map<String, Object> result = new HashMap<>(4);
            result.put("status", response.statusCode());
            result.put("finalUrl", response.uri().toString());
            result.put("headers", responseHeaders(response));
            result.put("body", cleaned);
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("网页代理失败: url={}, error={}", url, e.getMessage());
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "网页代理失败: " + e.getMessage());
        }
    }

    @Override
    public TestCase saveCase(Long userId, TestCaseSaveDTO dto) {
        TestCase entity;
        if (dto.getId() != null) {
            entity = testCaseMapper.selectById(dto.getId());
            if (entity == null || !userId.equals(entity.getUserId())) {
                throw new BusinessException(ErrorCode.PERMISSION_DENIED);
            }
        } else {
            entity = new TestCase();
            entity.setUserId(userId);
            entity.setCreateTime(LocalDateTime.now());
        }
        entity.setName(dto.getName());
        entity.setType(dto.getType() != null ? dto.getType() : "api");
        entity.setMethod(dto.getMethod() != null ? dto.getMethod().toUpperCase() : "GET");
        entity.setUrl(dto.getUrl());
        entity.setHeaders(dto.getHeaders());
        entity.setParams(dto.getParams());
        entity.setBody(dto.getBody());
        entity.setExpectedStatus(dto.getExpectedStatus());
        entity.setExpectedBody(dto.getExpectedBody());
        entity.setUpdateTime(LocalDateTime.now());
        if (entity.getId() == null) {
            testCaseMapper.insert(entity);
        } else {
            testCaseMapper.updateById(entity);
        }
        return entity;
    }

    @Override
    public List<TestCase> listCases(Long userId) {
        LambdaQueryWrapper<TestCase> wrapper = new LambdaQueryWrapper<TestCase>()
                .eq(TestCase::getUserId, userId)
                .orderByDesc(TestCase::getUpdateTime);
        return testCaseMapper.selectList(wrapper);
    }

    @Override
    public boolean deleteCase(Long userId, Long caseId) {
        TestCase entity = testCaseMapper.selectById(caseId);
        if (entity == null || !userId.equals(entity.getUserId())) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }
        return testCaseMapper.deleteById(caseId) > 0;
    }

    @Override
    public Map<String, Object> runCase(Long userId, Long caseId) {
        TestCase testCase = testCaseMapper.selectById(caseId);
        if (testCase == null || !userId.equals(testCase.getUserId())) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }
        return executeAndRecord(userId, testCase);
    }

    @Override
    public Map<String, Object> runAllAndReport(Long userId) {
        List<TestCase> cases = listCases(userId);
        List<Map<String, Object>> results = new ArrayList<>(cases.size());
        int passed = 0;
        for (TestCase tc : cases) {
            Map<String, Object> r = executeAndRecord(userId, tc);
            results.add(r);
            if (Boolean.TRUE.equals(r.get("passed"))) {
                passed++;
            }
        }
        String report = buildMarkdownReport(userId, cases, results, passed);
        Map<String, Object> summary = new HashMap<>(8);
        summary.put("report", report);
        summary.put("total", cases.size());
        summary.put("passed", passed);
        summary.put("failed", cases.size() - passed);
        summary.put("results", results);
        return summary;
    }

    // ================== 私有工具方法 ==================

    /**
     * 执行单条用例并落库记录
     */
    private Map<String, Object> executeAndRecord(Long userId, TestCase tc) {
        Map<String, Object> result = new HashMap<>(8);
        result.put("caseId", tc.getId());
        result.put("caseName", tc.getName());
        result.put("method", tc.getMethod());
        result.put("url", tc.getUrl());
        TestRecord record = new TestRecord();
        record.setUserId(userId);
        record.setCaseId(tc.getId());
        record.setCaseName(tc.getName());
        record.setMethod(tc.getMethod());
        record.setUrl(tc.getUrl());
        record.setCreateTime(LocalDateTime.now());
        try {
            ProxyRequestDTO dto = new ProxyRequestDTO();
            dto.setMethod(tc.getMethod());
            dto.setUrl(tc.getUrl());
            if (tc.getHeaders() != null && !tc.getHeaders().isEmpty()) {
                dto.setHeaders(objectMapper.readValue(tc.getHeaders(), Map.class));
            }
            if (tc.getParams() != null && !tc.getParams().isEmpty()) {
                dto.setParams(objectMapper.readValue(tc.getParams(), Map.class));
            }
            dto.setBody(tc.getBody());
            Map<String, Object> resp = proxyRequest(dto);
            int status = (Integer) resp.get("status");
            long elapsed = ((Number) resp.get("timeMs")).longValue();
            String body = (String) resp.get("body");
            boolean statusOk = tc.getExpectedStatus() == null || tc.getExpectedStatus() == status;
            boolean bodyOk = tc.getExpectedBody() == null || tc.getExpectedBody().isEmpty()
                    || (body != null && body.contains(tc.getExpectedBody()));
            boolean isPassed = statusOk && bodyOk;
            result.put("status", status);
            result.put("elapsedMs", elapsed);
            result.put("passed", isPassed);
            result.put("responsePreview", truncate(body));
            result.put("errorMsg", isPassed ? null : buildFailReason(tc, status, body, statusOk, bodyOk));
            record.setStatus(status);
            record.setElapsedMs(elapsed);
            record.setPassed(isPassed ? 1 : 0);
            record.setResponsePreview(truncate(body));
            record.setErrorMsg(isPassed ? null : buildFailReason(tc, status, body, statusOk, bodyOk));
        } catch (Exception e) {
            result.put("passed", false);
            result.put("errorMsg", e.getMessage());
            record.setPassed(0);
            record.setErrorMsg(e.getMessage());
        }
        testRecordMapper.insert(record);
        return result;
    }

    /**
     * 生成 Markdown 测试报告
     */
    private String buildMarkdownReport(Long userId, List<TestCase> cases, List<Map<String, Object>> results, int passed) {
        StringBuilder sb = new StringBuilder(512);
        sb.append("# 测试报告\n\n");
        sb.append("> 由 Atelier 测试工具自动生成\n\n");
        sb.append("- **执行人ID**: ").append(userId).append("\n");
        sb.append("- **执行时间**: ").append(LocalDateTime.now()).append("\n");
        sb.append("- **用例总数**: ").append(cases.size()).append("\n");
        sb.append("- **通过**: ").append(passed).append("\n");
        sb.append("- **失败**: ").append(cases.size() - passed).append("\n");
        sb.append("- **通过率**: ").append(cases.isEmpty() ? 0 : Math.round(passed * 100.0 / cases.size())).append("%\n\n");
        sb.append("## 用例明细\n\n");
        sb.append("| # | 用例 | 方法 | 状态码 | 耗时(ms) | 结果 | 备注 |\n");
        sb.append("|---|------|------|--------|----------|------|------|\n");
        for (int i = 0; i < results.size(); i++) {
            Map<String, Object> r = results.get(i);
            sb.append("| ").append(i + 1)
                    .append(" | ").append(r.get("caseName"))
                    .append(" | ").append(r.get("method"))
                    .append(" | ").append(r.getOrDefault("status", "-"))
                    .append(" | ").append(r.getOrDefault("elapsedMs", "-"))
                    .append(" | ").append(Boolean.TRUE.equals(r.get("passed")) ? "✅ 通过" : "❌ 失败")
                    .append(" | ").append(r.get("errorMsg") == null ? "" : r.get("errorMsg"))
                    .append(" |\n");
        }
        sb.append("\n## 失败详情\n\n");
        boolean anyFail = false;
        for (Map<String, Object> r : results) {
            if (!Boolean.TRUE.equals(r.get("passed"))) {
                anyFail = true;
                sb.append("### ").append(r.get("caseName")).append("\n\n");
                sb.append("- URL: `").append(r.get("url")).append("`\n");
                sb.append("- 失败原因: ").append(r.get("errorMsg")).append("\n\n");
            }
        }
        if (!anyFail) {
            sb.append("无失败用例。\n");
        }
        return sb.toString();
    }

    /**
     * 构建URI（含查询参数）
     */
    private URI buildUri(ProxyRequestDTO dto) {
        StringBuilder sb = new StringBuilder(dto.getUrl());
        if (dto.getParams() != null && !dto.getParams().isEmpty()) {
            String sep = dto.getUrl().contains("?") ? "&" : "?";
            sb.append(sep);
            boolean first = true;
            for (Map.Entry<String, String> e : dto.getParams().entrySet()) {
                if (!first) {
                    sb.append("&");
                }
                sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8));
                sb.append("=");
                sb.append(URLEncoder.encode(e.getValue() == null ? "" : e.getValue(), StandardCharsets.UTF_8));
                first = false;
            }
        }
        return URI.create(sb.toString());
    }

    /**
     * 按方法构建HttpRequest
     */
    private HttpRequest buildRequest(HttpRequest.Builder builder, String method, String body) {
        switch (method) {
            case "GET":
                return builder.GET().build();
            case "POST":
                return builder.POST(bodyPublisher(body)).build();
            case "PUT":
                return builder.PUT(bodyPublisher(body)).build();
            case "DELETE":
                return builder.DELETE().build();
            case "PATCH":
            case "HEAD":
            case "OPTIONS":
            default:
                return builder.method(method, bodyPublisher(body)).build();
        }
    }

    private HttpRequest.BodyPublisher bodyPublisher(String body) {
        return HttpRequest.BodyPublishers.ofString(body == null ? "" : body, StandardCharsets.UTF_8);
    }

    /**
     * 收集响应头（同名头合并）
     */
    private Map<String, String> responseHeaders(HttpResponse<?> response) {
        Map<String, String> headers = new LinkedHashMap<>();
        response.headers().map().forEach((k, v) -> headers.put(k, String.join("; ", v)));
        return headers;
    }

    /**
     * 剥离阻止iframe与CSP的脚本/指令
     */
    private String stripFrameBusting(String html) {
        if (html == null) {
            return "";
        }
        // 移除 frame-buster 脚本片段（粗粒度，主要应对常见X-Frame-Options JS）
        String cleaned = html.replaceAll("(?i)<script[^>]*>if\\s*\\(\\s*(top|self|window)\\s*[!=]=?", "");
        return cleaned;
    }

    private String truncate(String s) {
        if (s == null) {
            return null;
        }
        return s.length() > PREVIEW_MAX_LENGTH ? s.substring(0, PREVIEW_MAX_LENGTH) + "..." : s;
    }

    private String buildFailReason(TestCase tc, int status, String body, boolean statusOk, boolean bodyOk) {
        List<String> reasons = new ArrayList<>(2);
        if (!statusOk) {
            reasons.add("状态码不符(预期 " + tc.getExpectedStatus() + ", 实际 " + status + ")");
        }
        if (!bodyOk) {
            reasons.add("响应未包含预期文本「" + truncate(tc.getExpectedBody()) + "」");
        }
        return String.join("; ", reasons);
    }

    private String statusText(int code) {
        switch (code) {
            case 200: return "OK";
            case 201: return "Created";
            case 204: return "No Content";
            case 301: return "Moved Permanently";
            case 302: return "Found";
            case 304: return "Not Modified";
            case 400: return "Bad Request";
            case 401: return "Unauthorized";
            case 403: return "Forbidden";
            case 404: return "Not Found";
            case 500: return "Internal Server Error";
            case 502: return "Bad Gateway";
            case 503: return "Service Unavailable";
            default: return "";
        }
    }

    /**
     * URL 合法性校验（必须 http/https）
     */
    private void validateUrl(String url) {
        if (url == null || url.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "URL不能为空");
        }
        String lower = url.toLowerCase();
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅支持 http/https 协议");
        }
    }
}
