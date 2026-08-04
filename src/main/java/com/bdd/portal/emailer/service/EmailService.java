package com.bdd.portal.emailer.service;

import com.bdd.portal.emailer.entity.EmailConfiguration;
import com.bdd.portal.emailer.repository.EmailConfigurationRepository;
import com.bdd.portal.emailer.util.EncryptionUtil;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Properties;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final EmailConfigurationRepository emailConfigurationRepository;

    private JavaMailSenderImpl getMailSender() {
        EmailConfiguration config = emailConfigurationRepository.findTopByOrderByIdDesc()
                .orElseThrow(() -> new RuntimeException("SMTP Configuration not found"));

        if (!config.isEnabled()) {
            throw new RuntimeException("Email notifications are disabled");
        }

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(config.getSmtpHost());
        mailSender.setPort(config.getSmtpPort());
        mailSender.setUsername(config.getUsername());
        mailSender.setPassword(EncryptionUtil.decrypt(config.getPassword()));

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", String.valueOf(config.isAuthentication()));
        props.put("mail.smtp.starttls.enable", String.valueOf(config.isEnableTls()));
        
        if (config.isEnableSsl()) {
            props.put("mail.smtp.ssl.enable", "true");
        }
        
        return mailSender;
    }

    public void sendEmail(String to, String subject, String htmlBody, List<File> attachments) throws MessagingException {
        EmailConfiguration config = emailConfigurationRepository.findTopByOrderByIdDesc()
                .orElseThrow(() -> new RuntimeException("SMTP Configuration not found"));

        if (!config.isEnabled()) {
            log.warn("Email sending skipped because SMTP is disabled");
            return;
        }

        JavaMailSenderImpl mailSender = getMailSender();
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(config.getFromEmail());
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);

        if (config.getReplyTo() != null && !config.getReplyTo().isEmpty()) {
            helper.setReplyTo(config.getReplyTo());
        }

        if (attachments != null) {
            for (File file : attachments) {
                if (file.exists()) {
                    helper.addAttachment(file.getName(), new FileSystemResource(file));
                }
            }
        }

        mailSender.send(message);
        log.info("Email sent successfully to {}", to);
    }
    
    public void testConnection(EmailConfiguration config) throws MessagingException {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(config.getSmtpHost());
        mailSender.setPort(config.getSmtpPort());
        mailSender.setUsername(config.getUsername());
        // Use password directly if it's a test before saving, or decrypt if it's already saved
        String pass = config.getPassword();
        try {
            // try to decrypt, if it fails, assume it's raw
            pass = EncryptionUtil.decrypt(pass);
        } catch (Exception e) {
            // ignore
        }
        mailSender.setPassword(pass);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", String.valueOf(config.isAuthentication()));
        props.put("mail.smtp.starttls.enable", String.valueOf(config.isEnableTls()));
        
        if (config.isEnableSsl()) {
            props.put("mail.smtp.ssl.enable", "true");
        }
        
        mailSender.testConnection();
    }
}
