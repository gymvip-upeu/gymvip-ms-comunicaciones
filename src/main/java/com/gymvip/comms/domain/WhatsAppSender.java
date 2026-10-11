package com.gymvip.comms.domain;

import java.util.List;

public interface WhatsAppSender {
    String sendTemplateMessage(String phoneNumber, String templateName, String language, List<String> parameters);
}
