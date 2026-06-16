package com.example.demo.service.impl;

import com.example.demo.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${app.mail.from:${spring.mail.username:}}")
    private String fromEmail;

    @Value("${app.mail.from-name:TX任务网}")
    private String fromName;

    @Override
    public void sendVerificationCode(String toEmail, String code) {
        validateMailConfig();

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("邮箱验证码");
            helper.setText(buildMailContent(code), false);
            mailSender.send(mimeMessage);
        } catch (MailAuthenticationException e) {
            throw new IllegalStateException("SMTP 鉴权失败，请检查邮箱账号、密码或授权码是否正确", e);

        } catch (MessagingException e) {
            throw new IllegalStateException("验证码邮件构建失败", e);
        } catch (MailException e) {
            throw new IllegalStateException("验证码邮件发送失败：" + e.getMessage(), e);
        } catch (Exception e) {
            throw new IllegalStateException("验证码邮件发送失败：" + e.getMessage(), e);
        }
    }

    private void validateMailConfig() {
        if (isBlank(mailHost) || isBlank(mailUsername) || isBlank(mailPassword) || isBlank(fromEmail)) {
            throw new IllegalStateException("请先配置 SMTP：spring.mail.host、spring.mail.username、spring.mail.password 和发件人地址");
        }
    }

    private String buildMailContent(String code) {
        return "您好，您的邮箱验证码为：" + code + "，5 分钟内有效。\n\n"
                + "如果这不是您的操作，请忽略本邮件。";
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
