package com.example.MyProject.REVIEW.config;

import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient restClient() {

        HttpClient httpClient = HttpClient.create()
                .option(
                        ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        5000
                )
                .responseTimeout(
                        Duration.ofSeconds(15)
                );

        ReactorClientHttpRequestFactory factory =
                new ReactorClientHttpRequestFactory(
                        httpClient
                );

        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
}

