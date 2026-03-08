package com.familyreunion.rsvp.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service

@Service
class NotificationService(
    private val mailSender: JavaMailSender?,
    @Value("\${app.base-url:http://localhost:5173}") private val baseUrl: String,
    @Value("\${spring.mail.username:}") private val fromEmail: String,
    @Value("\${twilio.account-sid:}") private val twilioAccountSid: String,
    @Value("\${twilio.auth-token:}") private val twilioAuthToken: String,
    @Value("\${twilio.from-number:}") private val twilioFromNumber: String
) {
    private val log = LoggerFactory.getLogger(NotificationService::class.java)

    fun sendTicketEmail(to: String, familyName: String, attendeeNames: List<String>, checkinToken: String) {
        if (mailSender == null || fromEmail.isBlank()) {
            throw IllegalStateException("Email not configured. Set GMAIL_USERNAME and GMAIL_APP_PASSWORD.")
        }

        val ticketUrl = "$baseUrl/ticket/$checkinToken"

        val htmlBody = """
            <div style="font-family: sans-serif; max-width: 600px; margin: 0 auto;">
                <h2 style="color: #4a5568;">Tumblin Family Reunion Ticket</h2>
                <p>Hello ${familyName} Family!</p>
                <p>Here is your reunion check-in ticket. Show the QR code at the door for entry.</p>
                <div style="background: #f7fafc; border-radius: 8px; padding: 20px; margin: 20px 0;">
                    <h3 style="margin-top: 0;">Your Party:</h3>
                    <ul style="list-style: none; padding: 0;">
                        ${attendeeNames.joinToString("\n") { "<li style=\"padding: 4px 0;\">$it</li>" }}
                    </ul>
                </div>
                <p>
                    <a href="$ticketUrl"
                       style="display: inline-block; background: #3182ce; color: white; padding: 12px 24px;
                              border-radius: 6px; text-decoration: none; font-weight: bold;">
                        View Your Ticket & QR Code
                    </a>
                </p>
                <p style="color: #718096; font-size: 14px;">
                    You can also open this link directly: <a href="$ticketUrl">$ticketUrl</a>
                </p>
            </div>
        """.trimIndent()

        val message = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, true, "UTF-8")
        helper.setFrom(fromEmail)
        helper.setTo(to)
        helper.setSubject("Your Tumblin Family Reunion Ticket - ${familyName} Family")
        helper.setText(htmlBody, true)
        mailSender.send(message)

        log.info("Ticket email sent to $to for family $familyName")
    }

    fun sendTicketSms(to: String, familyName: String, checkinToken: String) {
        if (twilioAccountSid.isBlank() || twilioAuthToken.isBlank() || twilioFromNumber.isBlank()) {
            throw IllegalStateException("Twilio not configured. Set TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, and TWILIO_FROM_NUMBER.")
        }

        val ticketUrl = "$baseUrl/ticket/$checkinToken"
        val messageBody = "Tumblin Family Reunion - ${familyName} Family Ticket\n\nShow this at the door for check-in:\n$ticketUrl"

        com.twilio.Twilio.init(twilioAccountSid, twilioAuthToken)
        com.twilio.rest.api.v2010.account.Message.creator(
            com.twilio.type.PhoneNumber(to),
            com.twilio.type.PhoneNumber(twilioFromNumber),
            messageBody
        ).create()

        log.info("Ticket SMS sent to $to for family $familyName")
    }

    fun isEmailConfigured(): Boolean = mailSender != null && fromEmail.isNotBlank()
    fun isSmsConfigured(): Boolean = twilioAccountSid.isNotBlank() && twilioAuthToken.isNotBlank()
}
