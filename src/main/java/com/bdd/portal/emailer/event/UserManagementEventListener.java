package com.bdd.portal.emailer.event;

import com.bdd.portal.controller.AuthRestController;
import com.bdd.portal.controller.UserRestController;
import com.bdd.portal.emailer.entity.EmailQueue;
import com.bdd.portal.emailer.repository.EmailQueueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
@RequiredArgsConstructor
public class UserManagementEventListener {

    private final EmailQueueRepository emailQueueRepository;
    private final TemplateEngine templateEngine;

    @EventListener
    public void onOtpRequested(AuthRestController.OtpRequestedEvent event) {
        Context context = new Context();
        context.setVariable("name", event.user().getFirstName());
        context.setVariable("otp", event.otp());
        
        String body = templateEngine.process("email/forgot-password-otp", context);
                
        queueEmail(event.user().getEmail(), "Your Password Reset OTP", body);
    }

    @EventListener
    public void onPasswordChanged(AuthRestController.PasswordChangedEvent event) {
        Context context = new Context();
        context.setVariable("name", event.user().getFirstName());
        
        String body = templateEngine.process("email/password-reset-success", context);

        queueEmail(event.user().getEmail(), "Password Changed Successfully", body);
    }

    private void queueEmail(String to, String subject, String body) {
        EmailQueue queue = new EmailQueue();
        queue.setRecipient(to);
        queue.setSubject(subject);
        queue.setBody(body);
        emailQueueRepository.save(queue);
    }
    
    @EventListener
    public void onUserCreated(UserRestController.UserCreatedEvent event) {
        Context context = new Context();
        context.setVariable("name", event.user().getFirstName());
        context.setVariable("email", event.user().getEmail());
        context.setVariable("password", event.rawPassword());
        context.setVariable("portalUrl", "http://localhost:8080/login");
        
        String body = templateEngine.process("email/welcome-email", context);

        queueEmail(event.user().getEmail(), "Welcome to PSL Automation Portal", body);
    }
}
