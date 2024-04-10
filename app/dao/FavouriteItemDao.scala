package dao



import javax.inject.Inject
import models.{ FavouriteItem, Item}
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

class FavouriteItemDao  @Inject()
(protected val dbConfigProvider: DatabaseConfigProvider)
(implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] with ItemTrait {

  import profile.api._

  private val FavouriteItems = TableQuery[FavouriteItemTable]
  private val Items = TableQuery[ItemTable]

  def getAll(user_id: Int): Future[Seq[FavouriteItem]] =
    db.run(FavouriteItems.filter(_.user_id === user_id).result)

  def getAllNew(user_id: Int): Future[Seq[Item]] = {
    val query = for (
      favouriteItem <- FavouriteItems;
      item <- Items if favouriteItem.user_id === user_id && favouriteItem.item_id === item.id
    ) yield item
    db.run(query.result)
  }

  def deleteAll(user_id: Int): Future[Int] =
    db.run(FavouriteItems.filter(_.user_id === user_id).delete)


  def delete(user_id: Int, item_id: Int): Future[Int] =
    db.run(FavouriteItems.filter(X => (X.user_id === user_id) && (X.item_id === item_id)).delete)

  def newAdd(item: FavouriteItem) =
    db.run {
      FavouriteItems.filter(X => X.user_id === item.user_id && X.item_id === item.item_id).result.flatMap { X =>
        if (X.isEmpty)
          FavouriteItems += item
        else
          FavouriteItems.filter(X => X.user_id === item.user_id && X.item_id === item.item_id).update(item)
      }
    }

  def add(favourite: FavouriteItem): Future[Int] =
    db.run(FavouriteItems.insertOrUpdate(favourite))

  private class FavouriteItemTable(tag: Tag) extends Table[FavouriteItem](tag, "FAVOURITEITEM") {

    def user_id = column[Int]("USER_ID", O.PrimaryKey)
    def item_id = column[Int]("ITEM_ID", O.PrimaryKey)

    def item = foreignKey("ITEM_FK", item_id, Items)(_.id)

    def * = (user_id, item_id) <> (FavouriteItem.tupled, FavouriteItem.unapply)
  }
}