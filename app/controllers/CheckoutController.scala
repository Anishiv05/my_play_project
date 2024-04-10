package controllers

import com.stripe.Stripe
import com.stripe.model.checkout.Session
import com.stripe.param.checkout.SessionCreateParams
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future} // Add this import

class CheckoutController @Inject()(cc: ControllerComponents,  AuthAction: AuthAction)(implicit executionContext: ExecutionContext) extends AbstractController(cc) {


  private val logger: Logger = Logger(this.getClass)


  val loginForm = Form(
    (
      "email" -> text)
  )

  def fetchEmail: Action[AnyContent] = Action { implicit request =>
    val boundForm = loginForm.bindFromRequest()
    val emailOption = boundForm("email").value // Retrieve the email as an Option[String]

    emailOption match {
      case Some(email) =>
        logger.info(s"Email received: $email")
        Ok(views.html.email(email))
      case None =>
        logger.warn("Email not received")
        BadRequest("Email not received")
    }
  }



  //  def fetchEmail: Action[AnyContent] = Action { implicit request =>
//    val emailOption = request.body.asFormUrlEncoded.flatMap(_.get("email").flatMap(_.headOption))
//    emailOption match {
//      case Some(email) =>
//        logger.info(s"Email received: $email") // Log the received email
//        // Process the email
//        Ok(views.html.email(email))
//      case None =>
//        logger.warn("Email not provided") // Log a warning if email is not provided
//        BadRequest("Email not provided")
//    }
//  }

  Stripe.apiKey = "sk_test_your_secret_key"

  def createCheckoutSession(amount: BigDecimal): Action[AnyContent] = Action.async { implicit request =>
    val amountInCents = (amount * 100).toLong

    val params = SessionCreateParams.builder()
      .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD) // Use addPaymentMethodType instead of setPaymentMethodTypes
      .addLineItem(
        SessionCreateParams.LineItem.builder()
          .setPriceData(
            SessionCreateParams.LineItem.PriceData.builder()
              .setCurrency("usd")
              .setProductData(
                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                  .setName("Your Product Name")
                  .build()
              )
              .setUnitAmount(amountInCents) // Amount in cents as Long
              .build()
          )
          .setQuantity(1) // Quantity of the product
          .build()
      )
      .setMode(SessionCreateParams.Mode.PAYMENT)
      .setSuccessUrl("http://example.com/success") // URL to redirect on successful payment
      .setCancelUrl("http://example.com/cancel") // URL to redirect on payment cancellation
      .build()

    val session: Session = Session.create(params)
    Future.successful(Ok(session.getId))
  }

  val paymentForm: Form[PaymentFormData] = Form {
    mapping(
      "amount" -> number(min = 1),
      "currency" -> nonEmptyText
    )(PaymentFormData.apply)(PaymentFormData.unapply)
  }


}

case class PaymentFormData(amount: Int, currency: String)

//
//def getEmail: Action[AnyContent] = Action { implicit request =>
//  val emailOption = request.body.asFormUrlEncoded.flatMap(_.get("email").map(_.head))
//  emailOption match {
//    case Some(email) =>
//      Ok(s"Received email: $email")
//    case None =>
//      BadRequest("Email not provided")
//  }
//}