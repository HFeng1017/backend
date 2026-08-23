package com.resume.platform.chat;

import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * AI服务配置类
 *
 * 仅配置WebClient（非阻塞HTTP客户端），用于调用智谱AI流式接口。
 * 注意：不配置全局响应超时，由Controller层的Flux.timeout()按需控制。
 *
 * @author system
 */
@Slf4j
@Configuration
public class AiConfig {

    @Value("${ai.zhipu.api-key}")
    private String apiKey;

    @Value("${ai.zhipu.base-url}")
    private String baseUrl;

    @Value("${ai.zhipu.connect-timeout:10000}")
    private int connectTimeout;

    /**
     * 配置WebClient Bean
     */
    @Bean
    public WebClient aiWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
                // 不设置全局responseTimeout，流式响应超时由业务层控制
                .responseTimeout(Duration.ofMinutes(5));

        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "text/event-stream")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();

        log.info("智谱AI WebClient初始化完成: baseUrl={}, connectTimeout={}ms", baseUrl, connectTimeout);
        return webClient;
    }
}
