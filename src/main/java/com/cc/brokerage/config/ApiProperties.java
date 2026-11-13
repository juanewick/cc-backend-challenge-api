package com.cc.brokerage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "brokerage.api")
public record ApiProperties(String basePath) {
}
