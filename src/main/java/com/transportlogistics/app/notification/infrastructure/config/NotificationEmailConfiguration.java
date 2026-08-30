package com.transportlogistics.app.notification.infrastructure.config;

import com.transportlogistics.app.notification.application.ports.out.EmailNotificationSenderPort;
import com.transportlogistics.app.notification.infrastructure.adapters.out.email.DeterministicTestEmailNotificationSenderAdapter;
import com.transportlogistics.app.notification.infrastructure.adapters.out.email.EmailNotificationDeliveryAdapter;
import com.transportlogistics.app.notification.infrastructure.adapters.out.email.SmtpEmailNotificationSenderAdapter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Arrays;
import java.util.Properties;

@Configuration
@EnableConfigurationProperties(NotificationEmailProperties.class)
public class NotificationEmailConfiguration {
    @Bean
    EmailNotificationSenderPort emailNotificationSender(NotificationEmailProperties email, Environment environment) {
        boolean productionProfile = Arrays.stream(environment.getActiveProfiles())
            .anyMatch(profile -> profile.equalsIgnoreCase("prod") || profile.equalsIgnoreCase("production"))
            || (Arrays.stream(environment.getActiveProfiles()).anyMatch(profile -> profile.equalsIgnoreCase("postgres"))
                && Arrays.stream(environment.getActiveProfiles()).noneMatch(profile -> profile.equalsIgnoreCase("e2e") || profile.equalsIgnoreCase("test") || profile.equalsIgnoreCase("dev")));
        email.validate(productionProfile);
        if (!email.isEnabled()) return new EmailNotificationDeliveryAdapter();
        if (email.testMode()) return new DeterministicTestEmailNotificationSenderAdapter();

        NotificationEmailProperties.Smtp smtp = email.getSmtp();
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(smtp.getHost());
        mailSender.setPort(smtp.getPort());
        mailSender.setDefaultEncoding("UTF-8");
        if (smtp.isAuthenticationRequired()) {
            mailSender.setUsername(smtp.getUsername());
            mailSender.setPassword(smtp.getPassword());
        }
        Properties javaMail = mailSender.getJavaMailProperties();
        javaMail.setProperty("mail.transport.protocol", "smtp");
        javaMail.setProperty("mail.smtp.auth", Boolean.toString(smtp.isAuthenticationRequired()));
        javaMail.setProperty("mail.smtp.connectiontimeout", Long.toString(email.getConnectTimeout().toMillis()));
        javaMail.setProperty("mail.smtp.timeout", Long.toString(email.getReadTimeout().toMillis()));
        javaMail.setProperty("mail.smtp.writetimeout", Long.toString(email.getReadTimeout().toMillis()));
        javaMail.setProperty("mail.smtp.starttls.enable", Boolean.toString("starttls".equalsIgnoreCase(smtp.getTlsMode())));
        javaMail.setProperty("mail.smtp.starttls.required", Boolean.toString("starttls".equalsIgnoreCase(smtp.getTlsMode())));
        javaMail.setProperty("mail.smtp.ssl.enable", Boolean.toString("ssl".equalsIgnoreCase(smtp.getTlsMode())));
        return new SmtpEmailNotificationSenderAdapter(mailSender, email);
    }
}
