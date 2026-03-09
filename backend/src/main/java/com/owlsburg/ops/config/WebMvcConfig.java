package com.owlsburg.ops.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final ModuleAccessInterceptor moduleAccessInterceptor;

    public WebMvcConfig(ModuleAccessInterceptor moduleAccessInterceptor) {
        this.moduleAccessInterceptor = moduleAccessInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(moduleAccessInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/**",
                        "/api/users/**",
                        "/api/modules/**",
                        "/api/settings/**",
                        "/api/chat/**",
                        "/api/agent-templates/**",
                        "/api/agent-instances/**",
                        "/api/agent-runs/**",
                        "/api/events/**",
                        "/api/scheduled-triggers/**",
                        "/api/documents/**",
                        "/api/tenant/**",
                        "/api/budget/**",
                        "/api/system/**",
                        "/api/admin/**",
                        "/api/agent-activity/**"
                );
    }
}
