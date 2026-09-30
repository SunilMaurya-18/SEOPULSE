package com.seopulse.support;

import com.seopulse.notification.EmailDispatcher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
public class TestEmailConfig {

    @Bean
    @Primary
    public CapturingEmailSender capturingEmailSender(ObjectProvider<EmailDispatcher> dispatcher) {
        return new CapturingEmailSender(() -> dispatcher.ifAvailable(EmailDispatcher::dispatchPending));
    }
}
