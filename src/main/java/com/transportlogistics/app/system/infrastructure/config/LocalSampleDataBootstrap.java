package com.transportlogistics.app.system.infrastructure.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
@Profile("h2")
@ConditionalOnProperty(name = "app.dev.sample-data.enabled", havingValue = "true")
class LocalSampleDataBootstrap implements ApplicationRunner {
    private final DataSource dataSource;

    LocalSampleDataBootstrap(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        var script = new ClassPathResource("db/sample-data/h2-phase1.sql");
        var populator = new ResourceDatabasePopulator(script);
        populator.setSeparator(";");
        DatabasePopulatorUtils.execute(populator, dataSource);
    }
}
