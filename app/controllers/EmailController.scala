// app/controllers/EmailController.scala
package controllers

import play.api.mvc._
import services.EmailService

import javax.inject.Inject

class EmailController @Inject()(emailService: EmailService, cc: ControllerComponents)
  extends AbstractController(cc) {


  def generateInvoice(productInfo: Seq[(String, Int, Double)]): String = {
    val invoiceContent = productInfo.map { case (productName, quantity, price) =>
      s"$productName x $quantity - $price\n"
    }.mkString("\n")

    s"Invoice:\n$invoiceContent"
  }

  def showInvoiceAndSendEmail: Action[AnyContent] = Action { implicit request =>
    val productInfoSession = request.session.get("productInfo")
    val productInfo = productInfoSession.map(_.split(",").map { pair =>
      val keyValue = pair.split(":")
      val price = keyValue(2).toDouble
      val quantity = keyValue(1).toInt
      val total = quantity * price // Calculate the total for each item
      (keyValue(0), quantity, total) // Include total in the tuple
    })

    productInfo match {
      case Some(info) =>
        val invoiceContent = info.map { case (productName, quantity, price) =>
          (productName, quantity, quantity * price)
        }

        val invoice = generateInvoice(invoiceContent)
//        val recipientEmailOption = request.body.asFormUrlEncoded.flatMap(_.get("email")).flatMap(_.headOption)
          val recipientEmailOption = request.session.get("email") // Retrieve the email from the session

        recipientEmailOption match {
          case Some(email) =>
//            emailService.sendEmail(email, "Invoice", invoice)
//            Ok(views.html.invoice(invoiceContent))
          Ok(s"email is - $recipientEmailOption")
          case None =>
//          BadRequest("Recipient email not provided")
            Ok(views.html.invoice(invoiceContent))

        }

      case None =>
        BadRequest("Product information not available in session") // Return a BadRequest result if product information is missing
    }
  }

  def sendEmail(): Action[AnyContent] = Action { implicit request =>
    emailService.sendEmail("knight906534@gmail.com", "Test Email - hi ", "This is a test email.")
    Ok("Email sent successfully!")
  }

}
