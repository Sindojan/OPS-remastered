package com.owlsburg.ops.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.owlsburg.ops")
public class JpaConfig {
}
