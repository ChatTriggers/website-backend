package com.chattriggers.website.data

import com.chattriggers.website.config.MailConfig
import com.sendgrid.Method
import com.sendgrid.Request
import com.sendgrid.SendGrid
import com.sendgrid.helpers.mail.Mail
import com.sendgrid.helpers.mail.objects.Content
import com.sendgrid.helpers.mail.objects.Email
import org.koin.core.KoinComponent
import org.koin.core.get
import java.io.IOException



object Emails : KoinComponent {
    private val emailConfig = get<MailConfig>()
    private val fromEmail = Email(emailConfig.fromEmail)
    private val sendGrid = SendGrid(emailConfig.sendgridKey)

    fun sendPasswordReset(toEmailString: String, resetToken: String) {
        val toEmail = Email(toEmailString)
        val subject = "ChatTriggers Password Reset Requested"
        val content = Content()

        val mail = Mail(fromEmail, subject, toEmail, content)

        val request = Request()
        try {
            request.method = Method.POST
            request.endpoint = "mail/send"
            request.body = mail.build()
            sendGrid.api(request)
        } catch (ex: IOException) {
            throw ex
        }

    }
}