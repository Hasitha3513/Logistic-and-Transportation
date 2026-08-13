package com.transportlogistics.app.organization.infrastructure.config;

import com.transportlogistics.app.organization.application.ports.in.DepartmentUseCase;
import com.transportlogistics.app.organization.application.ports.out.DepartmentRepository;
import com.transportlogistics.app.organization.application.service.DepartmentService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class DepartmentConfig {
    @Bean
    DepartmentUseCase departmentUseCase(DepartmentRepository repo) {
        return new DepartmentService(repo);
    }
}
