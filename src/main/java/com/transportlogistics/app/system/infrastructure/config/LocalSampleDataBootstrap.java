package com.transportlogistics.app.system.infrastructure.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
@Order(2)
@Profile({"h2", "docker", "postgres"})
@ConditionalOnProperty(name = "app.dev.sample-data.enabled", havingValue = "true")
@RequiredArgsConstructor
class LocalSampleDataBootstrap implements ApplicationRunner {
    private final DataSource dataSource;
    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) {
        var script = sampleDataScript();
        var populator = new ResourceDatabasePopulator(script);
        populator.setSeparator(";");
        populator.setContinueOnError(false);
        DatabasePopulatorUtils.execute(populator, dataSource);
    }

    ClassPathResource sampleDataScript() {
        var database = environment.acceptsProfiles(Profiles.of("postgres", "docker"))
                ? "postgresql-sample-data.sql"
                : "h2-phase1.sql";
        return new ClassPathResource("db/sample-data/" + database);
    }
}
