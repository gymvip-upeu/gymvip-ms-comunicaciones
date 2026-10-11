package com.gymvip.comms.domain;

import java.util.Map;

public interface EmailSender {
    String sendEmail(String to, String subject, String template, Map<String, Object> variables);
}
