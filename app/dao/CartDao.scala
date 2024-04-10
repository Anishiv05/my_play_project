package dao

import models.{CartItem, Item}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}



trait ItemTrait extends CategoryTrait { self: HasDatabaseConfigProvider[JdbcProfile] =>
  import profile.api._

  private val Categories = TableQuery[CategoryTable]

  class ItemTable(tag: Tag) extends Table[Item](tag, "ITEM") {
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def price = column[Double]("PRICE")
    def title = column[String]("TITLE")
    def description = column[String]("DESCRIPTION")
    def category_id = column[Int]("CATEGORY_ID")
    def quantity = column[Int]("QUANTITY")

    def item = foreignKey("CATEGORY_FK", category_id, Categories)(_.id)

    def * = (id, price, title, description, category_id, quantity) <> (Item.tupled, Item.unapply)
  }
}

trait CartTrait extends CategoryTrait with ItemTrait { self: HasDatabaseConfigProvider[JdbcProfile] =>
  import profile.api._

  private val Categories = TableQuery[CategoryTable]
  private val Items = TableQuery[ItemTable]

  class CartTable(tag: Tag) extends Table[CartItem](tag, "CARTITEM") {
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def user_id = column[Int]("USER_ID")
    def item_id = column[Int]("ITEM_ID")
    def amount = column[Int]("AMOUNT")

    def item = foreignKey("ITEM_FK", item_id, Items)(_.id)

    def * = (id, user_id, item_id, amount) <> (CartItem.tupled, CartItem.unapply)
  }
}

class CartDao @Inject()
(protected val dbConfigProvider: DatabaseConfigProvider)
(implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] with CartTrait {


  import profile.api._

  private val CartItems = TableQuery[CartTable]
  private val Items = TableQuery[ItemTable]

  def getAll(user_id: Int): Future[Seq[CartItem]] =
    db.run(CartItems.filter(_.user_id === user_id).result)

  def getAllNew(user_id: Int): Future[Seq[(Item, Int)]] = {
    val query = for (
      cartItem <- CartItems;
      item <- Items if cartItem.user_id === user_id && cartItem.item_id === item.id
    ) yield (item, cartItem.amount)
    db.run(query.result)
  }

  def deleteAll(user_id: Int): Future[Int] =
    db.run(CartItems.filter(_.user_id === user_id).delete)

  def delete(user_id: Int, cartItem_id: Int) =
    db.run {
      CartItems.filter(X => (X.user_id === user_id) && (X.item_id === cartItem_id)).result.flatMap { X =>
        if (X.head.amount == 1)
          CartItems.filter(X => (X.user_id === user_id) && (X.item_id === cartItem_id)).delete.map(_ => ())
        else
          CartItems.filter(X => (X.user_id === user_id) && (X.item_id === cartItem_id)).map(_.amount).update(X.head.amount - 1).map(_ => ())
      }
    }

  def add(cartItem: CartItem): Future[Int] =
    db.run(CartItems += cartItem)

  def newAdd(cartItem: CartItem): Future[Unit] = {
    // Check if the cart item already exists in the database
    val existingCartItemQuery = CartItems.filter(x => x.user_id === cartItem.user_id && x.item_id === cartItem.item_id)

    db.run(existingCartItemQuery.result.headOption).flatMap { existingCartItemOpt =>
      if (existingCartItemOpt.isEmpty) {
        // Cart item doesn't exist, add it only if the quantity is positive
        if (cartItem.amount > 0) {
          db.run(CartItems += cartItem).map(_ => ()) // Success
        } else {
          // Quantity is not positive, do nothing
          Future.successful(())
        }
      } else {
        // Cart item exists, update its quantity based on the amount purchased
        val existingCartItem = existingCartItemOpt.get
        val updatedQuantity = existingCartItem.amount + cartItem.amount
        if (updatedQuantity > 0) {
          db.run(existingCartItemQuery.map(_.amount).update(updatedQuantity)).map(_ => ()) // Success
        } else {
          // Quantity would become non-positive after update, do nothing
          Future.successful(())
        }
      }
    }
  }


//  def reduceCartItemQuantity(cartItemId: Int, purchaseAmount: Int): Future[Boolean] = {
//    val retrieveAction = CartItems.filter(_.id === cartItemId).result.headOption
//
//    db.run(retrieveAction).flatMap {
//      case Some(cartItem) =>
//        if (cartItem.amount >= purchaseAmount) {
//          // Update the quantity in the database
//          val updatedQuantity = cartItem.amount - purchaseAmount
//          val updateAction = CartItems.filter(_.id === cartItemId).map(_.amount).update(updatedQuantity)
//          db.run(updateAction).map(_ => true) // Quantity reduced successfully
//        } else {
//          Future.successful(false)
//        }
//      case None =>
//        Future.successful(false)
//    }
//  }

  
def reduceCartItemQuantity(ItemId: Int, purchaseAmount: Int): Future[Boolean] = {
  val retrieveAction = Items.filter(_.id === ItemId).result.headOption

  db.run(retrieveAction).flatMap {
    case Some(cartItem) =>
      if (cartItem.quantity >= purchaseAmount) {
        // Update the quantity in the Items table
        val updatedQuantity = cartItem.quantity - purchaseAmount
        val updateAction = Items.filter(_.id === cartItem.id).map(_.quantity).update(updatedQuantity)
        db.run(updateAction).map(_ => true) // Quantity reduced successfully
      } else {
        Future.successful(false)
      }
    case None =>
      Future.successful(false)
  }
}


}
