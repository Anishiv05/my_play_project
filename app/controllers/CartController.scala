package controllers

import dao.{CartDao, EventDao}
import models.{CartItem, Event}
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{AbstractController, ControllerComponents}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CartController @Inject()
(cartDao: CartDao, eventDao: EventDao,cc: ControllerComponents, AuthAction: AuthAction)
(implicit executionContext: ExecutionContext) extends AbstractController(cc) {

  val addToCartForm = Form(
    "id" -> number()
  )

  def getAll = AuthAction.async{ implicit request =>
    val user_id = request.session.get("id").get.toInt
    cartDao.getAllNew(user_id).map(items =>
      Ok(views.html.cart(items)).withSession(request.session + ("last" -> "/cart"))

    )
  }

  def add(item_id: Int) = AuthAction.async { implicit request =>
    val user_id = request.session.get("id").get.toInt
    val timestamp = java.sql.Timestamp.from(java.time.Instant.now())
    val last_url = request.session.get("last").get
    cartDao.newAdd(CartItem(0, user_id, item_id, 1)).flatMap(_ =>
      eventDao.add(Event(0, user_id, item_id, "addtocart", timestamp)).map(_ =>
        Redirect(last_url).flashing("success" -> "The item has been added to cart.")
      )
    )
  }

  def remove(item_id: Int) = AuthAction.async { implicit request =>
    val user_id = request.session.get("id").get.toInt

    cartDao.delete(user_id, item_id).map(_ =>
      Redirect(routes.CartController.getAll)
    )
  }

  def removeAll = AuthAction.async { implicit request =>
    val user_id = request.session.get("id").get.toInt
    cartDao.deleteAll(user_id).map(_ => Redirect(routes.CartController.getAll))
  }

//  def buy = AuthAction.async { implicit request =>
//    val user_id = request.session.get("id").get.toInt
//    val timestamp = java.sql.Timestamp.from(java.time.Instant.now())
//    cartDao.getAllNew(user_id).flatMap(cartItems =>
//    {
//      val events = cartItems.flatMap(X => Seq.fill(X._2)(Event(0, user_id, X._1.id, "buy", timestamp)))
//      eventDao.addAll(events).flatMap { _ =>
////        val productInfo = cartItems.map(item => (item._1.title, item._2, item._1.price))
////        val updatedSession = request.session + ("productInfo" -> productInfo.map(pair => s"${pair._1}:${pair._2}:${pair._3}").mkString(","))
//
//
//        val productInfo = cartItems.map(item => (item._1.id, item._1.title, item._2, item._1.price))
//        val updatedSession = request.session + ("productInfo" -> productInfo.map(pair => s"${pair._1}:${pair._2}:${pair._3}:${pair._4}").mkString(","))
//
//        Future.successful(Redirect(routes.PaymentController.showPaymentForm()).withSession(updatedSession))
//      }
//    }
//
//
//    )
//  }

  def buy = AuthAction.async { implicit request =>
    val user_id = request.session.get("id").get.toInt
    val timestamp = java.sql.Timestamp.from(java.time.Instant.now())

    cartDao.getAllNew(user_id).flatMap { cartItems =>
      // Check if all items in the cart have quantity > CartItem.amount

      if (cartItems.forall { case (item, amount) => item.quantity - amount > 97 }) {
        val events = cartItems.flatMap { case (item, quantity) =>
          Seq.fill(quantity)(Event(0, user_id, item.id, "buy", timestamp))
        }

        eventDao.addAll(events).flatMap { _ =>
          val productInfo = cartItems.map { case (item, quantity) =>
            (item.id, item.title, quantity, item.price)
          }
          val updatedSession = request.session + ("productInfo" -> productInfo.map {
            case (itemId, title, quantity, price) => s"$itemId:$title:$quantity:$price"
          }.mkString(","))

          println("enough items in stock")

          Future.successful(Redirect(routes.PaymentController.showPaymentForm()).withSession(updatedSession))
        }
      } else {
        // Handle case where quantity is not sufficient for some items
        println("not enough items")
        Future.successful(Redirect(routes.CartController.getAll).flashing("error" -> "Some items in your cart are no longer available."))
      }
    }
  }

}
