package dao

import javax.inject.Inject
import models.Login
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

trait LoginTrait { self: HasDatabaseConfigProvider[JdbcProfile] =>
  import profile.api._

  class LoginTable(tag: Tag) extends Table[Login](tag, "login") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def email = column[String]("email", O.Unique)
    def password = column[String]("password")

    def * = (id, email, password) <> (Login.tupled, Login.unapply)
  }
}

class LoginDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] with LoginTrait {

  import profile.api._

  private val Logins = TableQuery[LoginTable]

  def all(): Future[Seq[Login]] = db.run(Logins.result)

  def get(email: String): Future[Option[Login]] = db.run(Logins.filter(_.email === email).result.headOption)

  def exists(email: String): Future[Boolean] = db.run(Logins.filter(_.email === email).exists.result)

  def insert(login: Login): Future[Int] = db.run(Logins returning Logins.map(_.id) += login)

  def insert1(email: String, password: String): Future[Int] = {
    println(s"Inserting login with email: $email and password: $password")
    val query = (Logins.map(l => (l.email, l.password)) returning Logins.map(_.id)) += (email, password)
    db.run(query)
  }

  def authenticate(email: String, password: String): Future[Option[Login]] =
    db.run(Logins.filter(u => u.email === email && u.password === password).result.headOption)
}
