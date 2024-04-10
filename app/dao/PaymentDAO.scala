package dao

import models.Payment
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class PaymentDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
                          (implicit executionContext: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] with ItemTrait with CartTrait {

  import profile.api._

  private class PaymentsTable(tag: Tag) extends Table[Payment](tag, "PAYMENT") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def user_id = column[Int]("USER_ID")
    def name = column[String]("NAME")
    def email = column[String]("EMAIL")
    def city = column[String]("CITY")
    def total = column[BigDecimal]("TOTAL")
    def timestamp = column[java.sql.Timestamp]("TIMESTAMP")

    def * = (id, user_id, name, email, city, total, timestamp) <> ((Payment.apply _).tupled, Payment.unapply)
  }

  private val Payments = TableQuery[PaymentsTable]


  def getById(paymentId: Long): Future[Option[Payment]] = db.run(Payments.filter(_.id === paymentId).result.headOption)

  def delete(paymentId: Long): Future[Int] = db.run(Payments.filter(_.id === paymentId).delete)

  def update(paymentId: Long, updatedPayment: Payment): Future[Int] = {
    val query = Payments.filter(_.id === paymentId)
      .map(payment => (payment.name, payment.email, payment.city, payment.total, payment.timestamp))
      .update((updatedPayment.name, updatedPayment.email, updatedPayment.city, updatedPayment.total, updatedPayment.timestamp))
    db.run(query.transactionally)
  }

  def add(payment: Payment): Future[Int] = db.run(Payments += payment)

}
