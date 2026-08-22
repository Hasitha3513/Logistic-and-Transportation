package com.transportlogistics.app.notification.infrastructure.adapters.out.email;

import com.transportlogistics.app.notification.application.ports.out.EmailNotificationSenderPort;
import com.transportlogistics.app.notification.domain.model.EmailDeliveryErrorCategory;
import com.transportlogistics.app.notification.infrastructure.config.NotificationEmailProperties;
import jakarta.mail.Address;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.eclipse.angus.mail.smtp.SMTPAddressFailedException;
import org.eclipse.angus.mail.smtp.SMTPSendFailedException;
import org.eclipse.angus.mail.smtp.SMTPSenderFailedException;

import javax.net.ssl.SSLException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

/** SMTP transport. An accepted result means only that the configured SMTP server accepted the message. */
public class SmtpEmailNotificationSenderAdapter implements EmailNotificationSenderPort {
    private final JavaMailSender mailSender;
    private final NotificationEmailProperties properties;

    public SmtpEmailNotificationSenderAdapter(JavaMailSender mailSender, NotificationEmailProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public SendResult send(SendRequest request) throws InterruptedException {
        checkInterrupted();
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(strictAddress(request.from()));
            helper.setTo(strictAddress(request.to()));
            if (properties.getReplyTo() != null && !properties.getReplyTo().isBlank()) {
                helper.setReplyTo(strictAddress(properties.getReplyTo()));
            }
            helper.setSubject(request.subject());
            helper.setText(request.plainTextBody(), false);
            message.setHeader("X-Notification-Id", request.notificationId().toString());
            message.setHeader("X-Idempotency-Key", request.idempotencyKey());
            message.saveChanges();
            checkInterrupted();
            mailSender.send(message);
            checkInterrupted();
            return SendResult.accepted(message.getMessageID());
        } catch (MailAuthenticationException exception) {
            return rejected(EmailDeliveryErrorCategory.AUTHENTICATION, "SMTP_AUTHENTICATION", "SMTP authentication failed");
        } catch (MailPreparationException exception) {
            return classify(exception);
        } catch (MailException exception) {
            return classify(exception);
        } catch (AddressException exception) {
            return rejected(EmailDeliveryErrorCategory.INVALID_RECIPIENT, "INVALID_EMAIL_ADDRESS", "EMAIL address is invalid");
        } catch (MessagingException exception) {
            return classify(exception);
        }
    }

    private static InternetAddress strictAddress(String value) throws AddressException {
        InternetAddress address = new InternetAddress(value, true);
        address.validate();
        return address;
    }

    private static SendResult classify(Throwable failure) throws InterruptedException {
        if (failure instanceof MailSendException sendException) {
            for (Exception messageFailure : sendException.getMessageExceptions()) {
                SendResult classified = classify(messageFailure);
                if (!"SMTP_DELIVERY".equals(classified.errorCode())) return classified;
            }
        }
        Throwable current = failure;
        int smtpCode = -1;
        while (current != null) {
            if (current instanceof InterruptedException) {
                Thread.currentThread().interrupt();
                throw (InterruptedException) current;
            }
            if (current instanceof AuthenticationFailedException) {
                return rejected(EmailDeliveryErrorCategory.AUTHENTICATION, "SMTP_AUTHENTICATION", "SMTP authentication failed");
            }
            if (current instanceof AddressException) {
                return rejected(EmailDeliveryErrorCategory.INVALID_RECIPIENT, "INVALID_EMAIL_ADDRESS", "EMAIL address is invalid");
            }
            if (current instanceof SMTPAddressFailedException smtpAddress) {
                return smtpCodeResult(smtpAddress.getReturnCode(), true);
            }
            if (current instanceof SMTPSenderFailedException smtpSender) {
                return smtpCodeResult(smtpSender.getReturnCode(), false);
            }
            if (current instanceof SMTPSendFailedException smtpSend) {
                return smtpCodeResult(smtpSend.getReturnCode(), false);
            }
            if (current instanceof SendFailedException sendFailed && hasInvalidAddresses(sendFailed)) {
                return rejected(EmailDeliveryErrorCategory.INVALID_RECIPIENT, "SMTP_RECIPIENT_REJECTED", "SMTP server rejected the recipient");
            }
            if (current instanceof SocketTimeoutException) {
                return rejected(EmailDeliveryErrorCategory.TIMEOUT, "SMTP_TIMEOUT", "SMTP operation timed out");
            }
            if (current instanceof ConnectException || current instanceof UnknownHostException) {
                return rejected(EmailDeliveryErrorCategory.CONNECTION, "SMTP_CONNECTION", "SMTP server is unavailable");
            }
            if (current instanceof SSLException) {
                return rejected(EmailDeliveryErrorCategory.CONFIGURATION, "SMTP_TLS", "SMTP TLS negotiation failed");
            }
            int candidateCode = smtpReturnCode(current);
            if (candidateCode > 0) smtpCode = candidateCode;
            current = nextCause(current);
        }
        if (smtpCode >= 400 && smtpCode < 500) {
            return rejected(EmailDeliveryErrorCategory.PROVIDER_5XX, "SMTP_" + smtpCode, "SMTP server temporarily rejected the message");
        }
        if (smtpCode >= 500) {
            return rejected(EmailDeliveryErrorCategory.PROVIDER_4XX, "SMTP_" + smtpCode, "SMTP server permanently rejected the message");
        }
        if (failure instanceof MailPreparationException) {
            return rejected(EmailDeliveryErrorCategory.CONFIGURATION, "SMTP_MESSAGE_PREPARATION", "EMAIL message could not be prepared");
        }
        return rejected(EmailDeliveryErrorCategory.CONNECTION, "SMTP_DELIVERY", "SMTP delivery failed before acceptance");
    }

    private static boolean hasInvalidAddresses(SendFailedException exception) {
        Address[] invalid = exception.getInvalidAddresses();
        return invalid != null && invalid.length > 0;
    }

    private static Throwable nextCause(Throwable current) {
        if (current instanceof MessagingException messaging && messaging.getNextException() != null) {
            return messaging.getNextException();
        }
        return current.getCause();
    }

    private static int smtpReturnCode(Throwable current) {
        try {
            Object value = current.getClass().getMethod("getReturnCode").invoke(current);
            return value instanceof Integer code ? code : -1;
        } catch (ReflectiveOperationException ignored) {
            return -1;
        }
    }

    private static SendResult smtpCodeResult(int code, boolean recipient) {
        if (code >= 400 && code < 500) {
            return rejected(EmailDeliveryErrorCategory.PROVIDER_5XX, "SMTP_" + code,
                "SMTP server temporarily rejected the message");
        }
        if (recipient) {
            return rejected(EmailDeliveryErrorCategory.INVALID_RECIPIENT, "SMTP_" + code,
                "SMTP server rejected the recipient");
        }
        return rejected(EmailDeliveryErrorCategory.PROVIDER_4XX, "SMTP_" + code,
            "SMTP server permanently rejected the message");
    }

    private static SendResult rejected(EmailDeliveryErrorCategory category, String code, String message) {
        return SendResult.rejected(category, code, message);
    }

    private static void checkInterrupted() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) throw new InterruptedException("SMTP delivery interrupted");
    }
}
