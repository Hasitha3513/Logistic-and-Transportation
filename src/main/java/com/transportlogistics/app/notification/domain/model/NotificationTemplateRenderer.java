package com.transportlogistics.app.notification.domain.model;

import com.transportlogistics.app.shared.domain.BusinessRuleException;

import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NotificationTemplateRenderer {
    private static final Pattern TOKEN = Pattern.compile("\\{\\{([A-Za-z][A-Za-z0-9]*)}}" );
    private static final Pattern TOKEN_LIKE = Pattern.compile("\\{\\{.*?}}", Pattern.DOTALL);

    public RenderedNotification render(NotificationTemplate template, Map<String, String> variables) {
        var definition = NotificationEventCatalogue.require(template.eventType());
        validateTokens(definition, template.subject());
        validateTokens(definition, template.body());

        Map<String, String> safeVariables = variables == null ? Map.of() : Map.copyOf(variables);
        for (String required : definition.requiredVariables()) {
            if (safeVariables.get(required) == null || safeVariables.get(required).isBlank()) {
                throw new BusinessRuleException("TEMPLATE_DATA_MISSING", "Required template variable is missing: " + required);
            }
        }

        String subject = renderText(template.subject(), definition.optionalVariables(), safeVariables);
        String body = renderText(template.body(), definition.optionalVariables(), safeVariables);
        validateRendered(subject, 255, "subject");
        validateRendered(body, 4000, "body");
        return new RenderedNotification(subject, body);
    }

    static void validateTokens(NotificationEventDefinition definition, String text) {
        Matcher tokenLike = TOKEN_LIKE.matcher(text);
        StringBuffer textWithoutTokens = new StringBuffer();
        while (tokenLike.find()) {
            Matcher token = TOKEN.matcher(tokenLike.group());
            if (!token.matches() || !definition.allowsVariable(token.group(1))) {
                throw new IllegalArgumentException("Unknown or invalid template variable: " + tokenLike.group());
            }
            tokenLike.appendReplacement(textWithoutTokens, "");
        }
        tokenLike.appendTail(textWithoutTokens);
        if (textWithoutTokens.indexOf("{{") >= 0 || textWithoutTokens.indexOf("}}") >= 0) {
            throw new IllegalArgumentException("Malformed template variable syntax");
        }
    }

    private static String renderText(String text, Set<String> optionalVariables, Map<String, String> variables) {
        Matcher matcher = TOKEN.matcher(text);
        StringBuffer rendered = new StringBuffer();
        while (matcher.find()) {
            String name = matcher.group(1);
            String value = variables.get(name);
            if (value == null && optionalVariables.contains(name)) {
                value = "";
            }
            if (value == null) {
                throw new BusinessRuleException("TEMPLATE_DATA_MISSING", "Required template variable is missing: " + name);
            }
            NotificationTemplate.rejectUnsafeControls(value, "Template variable " + name);
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(rendered);
        return NotificationTemplate.normalize(rendered.toString());
    }

    private static void validateRendered(String value, int maximumLength, String field) {
        NotificationTemplate.rejectUnsafeControls(value, "Rendered " + field);
        if (value.isBlank() || value.length() > maximumLength) {
            throw new BusinessRuleException("NOTIFICATION_TEMPLATE_INVALID",
                "Rendered " + field + " must contain 1 to " + maximumLength + " characters");
        }
    }

    public record RenderedNotification(String subject, String body) {
    }
}
