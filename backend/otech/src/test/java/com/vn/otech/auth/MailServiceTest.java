package com.vn.otech.auth;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class MailServiceTest {
    @Mock
    private JavaMailSender mailSender;

    @Test
    void sendSignupOtpSendsExpectedMessage() {
        MailService service = new MailService(mailSender, "noreply@otech.test");

        service.sendSignupOtp("user@example.com", "123456");

        SimpleMailMessage message = captureMessage();
        assertEquals("noreply@otech.test", message.getFrom());
        assertArrayEquals(new String[] {"user@example.com"}, message.getTo());
        assertEquals("Your Otech verification code", message.getSubject());
        assertEquals("Your Otech signup verification code is 123456. It expires in 10 minutes.",
                message.getText());
    }

    @Test
    void sendPasswordResetOtpOmitsBlankSender() {
        MailService service = new MailService(mailSender, " ");

        service.sendPasswordResetOtp("user@example.com", "654321");

        SimpleMailMessage message = captureMessage();
        assertEquals(null, message.getFrom());
        assertEquals("Your Otech password reset code", message.getSubject());
        assertEquals("Your Otech password reset code is 654321. It expires in 30 minutes.",
                message.getText());
    }

    private SimpleMailMessage captureMessage() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }
}