package com.transportlogistics.app.organization.infrastructure.config;

import com.transportlogistics.app.organization.application.ports.in.ProjectUseCase;
import com.transportlogistics.app.organization.application.ports.out.ProjectRepository;
import com.transportlogistics.app.organization.application.service.ProjectService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ProjectConfig {
    @Bean
    ProjectUseCase projectUseCase(ProjectRepository repo) {
        return new ProjectService(repo);
    }
}
