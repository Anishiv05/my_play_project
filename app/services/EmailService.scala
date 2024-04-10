// app/services/EmailService.scala
package services

import javax.inject.Inject
import play.api.libs.mailer._

class EmailService @Inject()(mailerClient: MailerClient) {

  def sendEmail(to: String, subject: String, body: String): Unit = {
    val email = Email(
      subject = subject,
      from = "Knight <knight90653@gmail.com>",
      to = Seq(to),
      bodyText = Some(body)
    )
    mailerClient.send(email)
  }

  // Add more email sending methods as needed
}


//package services
//
//import javax.inject.Inject
//import play.api.libs.mailer.{Email, MailerClient}
//import play.api.mvc.ControllerComponents
//import views.html.invoice
//
//class EmailService @Inject()(mailerClient: MailerClient, cc: ControllerComponents) {
//  def sendInvoice(customerEmail: String, products: Seq[(String, Int, Double)]): Unit = {
//    val emailContent = invoice(customerEmail, products).body
//    val email = Email(
//      subject = "Invoice for Your Purchase",
//      from = "Knight <knight90653@gmail.com>",
//      to = Seq(customerEmail),
//      bodyText = Some("Invoice attached."),
//      bodyHtml = Some(emailContent)
//    )
//    mailerClient.send(email)
//  }
//}
