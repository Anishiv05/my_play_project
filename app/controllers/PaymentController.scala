package controllers

import dao.{CartDao, EventDao, PaymentDAO}
import models.{Event, Payment}
import play.api.Logger
import play.api.mvc._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class PaymentController @Inject()
(cc: ControllerComponents, cartDao: CartDao, eventDao: EventDao, AuthAction: AuthAction , paymentDAO: PaymentDAO)
                                 (implicit executionContext: ExecutionContext) extends AbstractController(cc) {


  private val logger = Logger(getClass)

  def processPayment(): Action[AnyContent] = AuthAction.async { implicit request =>
    // Extracting form data from the request
    val name = request.body.asFormUrlEncoded.get("name").headOption.getOrElse("")
    val email = request.body.asFormUrlEncoded.get("email").headOption.getOrElse("")
    val city = request.body.asFormUrlEncoded.get("city").headOption.getOrElse("")
    val productInfoSession = request.session.get("productInfo")

    val user_id = request.session.get("id").map(_.toInt).getOrElse(0) // Assuming you have user ID in the session
    val timestamp = java.sql.Timestamp.from(java.time.Instant.now())
    val last_url = request.session.get("last").getOrElse("/") // Default redirect URL if 'last' session attribute is not present


    // id, title, quantity, price
    val productInfo = productInfoSession.map(_.split(",").map { pair =>
      val keyValue = pair.split(":")
      val id = keyValue(0).toInt
      val quantity = keyValue(2).toInt
      val price = keyValue(3).toDouble
      val total = quantity * price // Calculate the total for each item
      (id, keyValue(1), quantity, total) // Include ID, title, quantity, and total in the tuple
    })

    // Inserting payment data into the database
    val paymentInsertion = productInfo.map { info =>
      val totalAmount = info.map(_._4).sum // Calculate the total amount for the entire order
      paymentDAO.add(Payment(0, user_id, name, email, city, totalAmount, timestamp)).map { _ =>
        Redirect(last_url).flashing("success" -> "Data added successfully")
      }
    }

    // delete cart items
//    cartDao.getAllNew(user_id).flatMap { cartItems =>
//      val events = cartItems.flatMap(X => Seq.fill(X._2)(Event(0, user_id, X._1.id, "purchase_complete", timestamp)))
//      eventDao.addAll(events).flatMap { _ =>
//        cartDao.deleteAll(user_id)
//      }
//    }

//    val cartReduce: Future[Unit] = productInfo.map { info =>
//      val reductionActions = info.map { case (itemId,_, quantity, _) =>
//        val itemIdInt = itemId.toInt
//        cartDao.reduceCartItemQuantity(itemIdInt, quantity).map(_ => ())
//      }
//
//      def reduceAll(actions: Seq[Future[Unit]]): Future[Unit] = {
//        actions match {
//          case Nil => Future.successful(Ok("ALL DONE HURRAY ....")) // Base case: no more actions to reduce
//          case headAction +: tailActions => headAction.flatMap(_ => reduceAll(tailActions)) // Reduce the head action and recursively process the rest
//        }
//      }
//      reduceAll(reductionActions)
//    }.getOrElse(Future.successful(Ok("ERROR ...."))) // Handle the case where productInfo is None
//
//
//    cartReduce.map { _ =>
//      println("Cart items reduced ")
//      Redirect(last_url).flashing("success" -> "Cart items reduced ")
//    }

    //    val result = productInfo.map(info => views.html.paymentcomplete("Payment Completed", info))
    //          .getOrElse(views.html.paymentcomplete("Payment Completed", Seq.empty))
    //        Future.successful(Ok(result))

    cartDao.getAllNew(user_id).flatMap { cartItems =>
      if (cartItems.forall { case (item, amount) => item.quantity - amount > 97 }) {
        val events = cartItems.flatMap(X => Seq.fill(X._2)(Event(0, user_id, X._1.id ,"purchase_complete", timestamp)))
        eventDao.addAll(events).flatMap { _ =>
          cartDao.deleteAll(user_id)
        }

        val cartReduce: Future[Unit] = productInfo.map { info =>
          val reductionActions = info.map { case (itemId, _, quantity, _) =>
            val itemIdInt = itemId.toInt
            cartDao.reduceCartItemQuantity(itemIdInt, quantity).map(_ => ())
          }

          def reduceAll(actions: Seq[Future[Unit]]): Future[Unit] = {
            actions match {
              case Nil => Future.successful(Ok("ALL DONE HURRAY ....")) // Base case: no more actions to reduce
              case headAction +: tailActions => headAction.flatMap(_ => reduceAll(tailActions)) // Reduce the head action and recursively process the rest
            }
          }

          reduceAll(reductionActions)
        }.getOrElse(Future.successful(Ok("ERROR ...."))) // Handle the case where productInfo is None


        cartReduce.map { _ =>
          println("Cart items reduced ")
          Redirect(last_url).flashing("success" -> "Cart items reduced ")
        }

        val result = productInfo.map(info => views.html.paymentcomplete("Payment Completed", info))
          .getOrElse(views.html.paymentcomplete("Payment Completed", Seq.empty))
        Future.successful(Ok(result))

      }
      else {
        println("not enough items - payment unsuccessfull")
        Future.successful(Redirect(routes.CartController.getAll).flashing("error" -> "Some items in your cart are no longer available."))
      }

    }



  }



  def showPaymentForm(): Action[AnyContent] = AuthAction { implicit request: Request[AnyContent] =>
    Ok(views.html.payment(""))
  }


  def getEmail: Action[AnyContent] = Action { implicit request =>

    val data = request.body.asFormUrlEncoded
    data.map { args =>
      val email = args("email").head
      Ok(s"email is - $email")
    }.getOrElse(Ok("YO"))
  }










}
