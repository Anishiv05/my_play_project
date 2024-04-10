package controllers

import dao.{EventDao, ItemDao}
import models.Event
import play.api.mvc.{AbstractController, ControllerComponents}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ItemController @Inject()
(itemDao: ItemDao, eventDao: EventDao,  cc: ControllerComponents, AuthAction: AuthAction)
(implicit executionContext: ExecutionContext) extends AbstractController(cc) {


  def show(item_id: Int) = AuthAction.async { implicit request =>
    val user_id = request.session.get("id").get.toInt
    val timestamp = java.sql.Timestamp.from(java.time.Instant.now())
    itemDao.getById(item_id).flatMap(item =>
      eventDao.add(Event(0, user_id, item_id, "view", timestamp)).map( _ =>
        Ok(views.html.item(item)).withSession(request.session - "last" + ("last" -> "/item/".concat(item_id.toString)))
      )
    )
  }


}
