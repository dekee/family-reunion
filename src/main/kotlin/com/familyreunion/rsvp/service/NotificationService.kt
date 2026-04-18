package com.familyreunion.rsvp.service

import com.familyreunion.rsvp.config.ReunionConfig
import com.familyreunion.rsvp.model.PaymentLineItem
import com.familyreunion.rsvp.repository.AdminUserRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class NotificationService(
    private val mailSender: JavaMailSender?,
    private val adminUserRepository: AdminUserRepository,
    private val reunionConfig: ReunionConfig,
    private val siteConfigService: SiteConfigService,
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
                <h2 style="color: #4a5568;">${siteConfigService.getResolvedValue("family.fullTitle", reunionConfig.fullTitle)} Ticket</h2>
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
        helper.setSubject("Your ${siteConfigService.getResolvedValue("family.fullTitle", reunionConfig.fullTitle)} Ticket - ${familyName} Family")
        helper.setText(htmlBody, true)
        mailSender.send(message)

        log.info("Ticket email sent to $to for family $familyName")
    }

    fun sendTicketSms(to: String, familyName: String, checkinToken: String) {
        if (twilioAccountSid.isBlank() || twilioAuthToken.isBlank() || twilioFromNumber.isBlank()) {
            throw IllegalStateException("Twilio not configured. Set TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, and TWILIO_FROM_NUMBER.")
        }

        val normalizedNumber = normalizePhoneNumber(to)
        val ticketUrl = "$baseUrl/ticket/$checkinToken"
        val messageBody = "${siteConfigService.getResolvedValue("family.fullTitle", reunionConfig.fullTitle)} - ${familyName} Family Ticket\n\nShow this at the door for check-in:\n$ticketUrl"

        com.twilio.Twilio.init(twilioAccountSid, twilioAuthToken)
        val message = com.twilio.rest.api.v2010.account.Message.creator(
            com.twilio.type.PhoneNumber(normalizedNumber),
            com.twilio.type.PhoneNumber(twilioFromNumber),
            messageBody
        ).create()

        val status = message.status?.toString() ?: "unknown"
        if (status == "failed" || status == "undelivered") {
            val errorMsg = message.errorMessage ?: "Message $status"
            log.error("Ticket SMS failed to $normalizedNumber for family $familyName: $errorMsg")
            throw RuntimeException("SMS failed: $errorMsg")
        }

        log.info("Ticket SMS sent to $normalizedNumber for family $familyName (status: $status, sid: ${message.sid})")
    }

    private fun normalizePhoneNumber(phone: String): String {
        val digits = phone.replace(Regex("[^0-9]"), "")
        return when {
            digits.length == 10 -> "+1$digits"
            digits.length == 11 && digits.startsWith("1") -> "+$digits"
            phone.startsWith("+") -> phone
            else -> "+$digits"
        }
    }

    fun sendPaymentNotificationToAdmins(
        familyName: String,
        payerName: String?,
        payerEmail: String?,
        amount: BigDecimal,
        lineItems: List<PaymentLineItem>,
        timestamp: LocalDateTime
    ) {
        if (!isEmailConfigured()) {
            log.info("Email not configured, skipping admin payment notification")
            return
        }

        val admins = adminUserRepository.findAll()
        if (admins.isEmpty()) {
            log.info("No admin users found, skipping admin payment notification")
            return
        }

        val formattedAmount = "$${amount}"
        val formattedTime = timestamp.format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a"))

        val lineItemRows = lineItems.joinToString("\n") { li ->
            val name = li.familyMemberName ?: li.guestName ?: "Unknown"
            """<tr>
                <td style="padding: 8px; border-bottom: 1px solid #e2e8f0;">${name}</td>
                <td style="padding: 8px; border-bottom: 1px solid #e2e8f0;">${li.ageGroup.name}</td>
                <td style="padding: 8px; border-bottom: 1px solid #e2e8f0;">$${li.amount}</td>
            </tr>"""
        }

        val htmlBody = """
            <div style="font-family: sans-serif; max-width: 600px; margin: 0 auto;">
                <h2 style="color: #2d3748;">Payment Received</h2>
                <div style="background: #f7fafc; border-radius: 8px; padding: 20px; margin: 20px 0;">
                    <p style="margin: 4px 0;"><strong>Family:</strong> ${familyName}</p>
                    <p style="margin: 4px 0;"><strong>Payer:</strong> ${payerName ?: "N/A"}</p>
                    <p style="margin: 4px 0;"><strong>Email:</strong> ${payerEmail ?: "N/A"}</p>
                    <p style="margin: 4px 0;"><strong>Total:</strong> ${formattedAmount}</p>
                    <p style="margin: 4px 0;"><strong>Date:</strong> ${formattedTime}</p>
                </div>
                <table style="width: 100%; border-collapse: collapse; margin: 20px 0;">
                    <thead>
                        <tr style="background: #edf2f7;">
                            <th style="padding: 8px; text-align: left;">Name</th>
                            <th style="padding: 8px; text-align: left;">Age Group</th>
                            <th style="padding: 8px; text-align: left;">Amount</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${lineItemRows}
                    </tbody>
                </table>
            </div>
        """.trimIndent()

        val subject = "Payment Received - $familyName Family - $formattedAmount"

        for (admin in admins) {
            try {
                val message = mailSender!!.createMimeMessage()
                val helper = MimeMessageHelper(message, true, "UTF-8")
                helper.setFrom(fromEmail)
                helper.setTo(admin.email)
                helper.setSubject(subject)
                helper.setText(htmlBody, true)
                mailSender.send(message)
                log.info("Payment notification sent to admin ${admin.email}")
            } catch (e: Exception) {
                log.error("Failed to send payment notification to ${admin.email}: ${e.message}", e)
            }
        }
    }

    fun isEmailConfigured(): Boolean = mailSender != null && fromEmail.isNotBlank()
    // Disabled until A2P 10DLC campaign is approved (pending TCR review)
    fun isSmsConfigured(): Boolean = false
}
