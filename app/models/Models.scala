package models

import java.sql.Timestamp

case class User(id: Int, email: String)

case class Item(id: Int, price: Double, title: String, description: String, category: Int, quantity: Int)

case class CartItem(id: Int, user_id: Int, item_id: Int, amount: Int)

case class Favourite(user_id: Int, item_id: Int)

case class Category(id: Int, parent_id: Int, depth: Int, name: String)

case class Event(id: Int, user_id: Int, item_id: Int, event: String, timestamp: Timestamp)

case class Payment(id: Long,user_id: Int,  name: String, email: String, city: String, total: BigDecimal,timestamp: Timestamp)

case class Login(id: Int, email: String, password: String)

case class FavouriteItem(user_id: Int, item_id: Int)



