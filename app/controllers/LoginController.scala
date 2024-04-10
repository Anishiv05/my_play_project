package controllers

import dao._
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{AbstractController, ControllerComponents}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class LoginController @Inject()
(loginDao: LoginDAO, itemDao: ItemDao, cartDao: CartDao, eventDao: EventDao, categoryDao: CategoryDao, cc: ControllerComponents, AuthAction: AuthAction)
(implicit executionContext: ExecutionContext) extends AbstractController(cc) {

  // Define a case class for login form data
  case class LoginForm(email: String, password: String)

  // Create a form mapping for LoginForm
  val loginForm = Form(
    mapping(
      "email" -> email,
      "password" -> nonEmptyText
    )(LoginForm.apply)(LoginForm.unapply)
  )

  // Action to show the login form
  def showLogin = Action { implicit request =>
    if (request.session.get("connected").isEmpty)
      Ok(views.html.login())
    else
      Redirect(routes.LoginController.index())
  }

  // Action to handle the login form submission
  def login = Action.async { implicit request =>
    loginForm.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest("Invalid form data")),
      formData => {
        val email = formData.email
        val password = formData.password

        loginDao.get(email).flatMap {
          case Some(user) if user.password == password =>
            // Authentication successful, create a session for the user
            Future.successful(
              Redirect(routes.LoginController.index()).withSession(
                request.session + ("connected" -> email) + ("id" -> user.id.toString)
              )
            )
          case _ =>
            loginDao.insert1(email, password).flatMap { id =>
                        Future.successful(
                          Redirect(routes.LoginController.index()).withSession(
                            request.session + ("connected" -> email) + ("id" -> id.toString)
                          )
                        )
                      }
        }
      }
    )
  }

  // Action to log out the user
  def logout = Action.async { implicit request =>
    Future.successful(Redirect(routes.LoginController.showLogin).withNewSession)
  }

    def index(category_id: Int) = AuthAction.async { implicit request =>
      categoryDao.getMenu(category_id).flatMap(categories =>
        categoryDao.getAllChilds(category_id).flatMap(relatedCategories =>
          itemDao.all(relatedCategories).map(items =>
            Ok(views.html.index2(items, categories, category_id)).withSession(request.session - "category" + ("category" -> category_id.toString) - "last" + ("last" -> "/items?category=".concat(category_id.toString)))
          )
        )
      )
    }

    def admin = AuthAction.async { implicit request =>
      loginDao.all().flatMap { logins =>
        val users = logins.map(login => models.User(login.id, login.email)) // Convert Login objects to User objects
        eventDao.getEverything.map { events =>
          Ok(views.html.admin(users, events)).withSession(request.session - "last" + ("last" -> "/admin"))
        }
      }
    }

}


//package controllers
//
//import dao.{CartDao, CategoryDao, EventDao, ItemDao, LoginDAO}
//
//import javax.inject.Inject
//import play.api.data.Form
//import play.api.data.Forms._
//import play.api.mvc.{AbstractController, ControllerComponents}
//
//import scala.concurrent.{ExecutionContext, Future}
//
//class LoginController @Inject()
//(loginDao: LoginDAO, itemDao: ItemDao, cartDao: CartDao, eventDao: EventDao, categoryDao: CategoryDao, cc: ControllerComponents, AuthAction: AuthAction)
//(implicit executionContext: ExecutionContext) extends AbstractController(cc) {
//
//  def index(category_id: Int) = AuthAction.async { implicit request =>
//    categoryDao.getMenu(category_id).flatMap(categories =>
//      categoryDao.getAllChilds(category_id).flatMap(relatedCategories =>
//        itemDao.all(relatedCategories).map(items =>
//          Ok(views.html.index2(items, categories, category_id)).withSession(request.session - "category" + ("category" -> category_id.toString) - "last" + ("last" -> "/items?category=".concat(category_id.toString)))
//        )
//      )
//    )
//  }
//
//  def admin = AuthAction.async { implicit request =>
//    loginDao.all().flatMap { logins =>
//      val users = logins.map(login => models.User(login.id, login.email)) // Convert Login objects to User objects
//      eventDao.getEverything.map { events =>
//        Ok(views.html.admin(users, events)).withSession(request.session - "last" + ("last" -> "/admin"))
//      }
//    }
//  }
//
//
////  val loginForm = Form("email" -> text)
//val loginForm = Form(
//  tuple(
//    "email" -> text,
//    "password" -> text
//  )
//)
//
//  def showLogin = Action { implicit request =>
//    if (request.session.get("connected").isEmpty)
//      Ok(views.html.login())
//    else
//      Redirect(routes.LoginController.index())
//  }
//
//  def login = Action.async { implicit request =>
//    val boundForm = loginForm.bindFromRequest()
//    val email = boundForm("email").value.getOrElse("")
//    val password = boundForm("password").value.getOrElse("")
//
//    loginDao.get(email).flatMap { userOption =>
//      if (userOption.isDefined) {
//        // Email exists, authenticate the user
//        loginDao.authenticate(email, password).flatMap { userOption =>
//          if (userOption.isDefined) {
//            // Authentication successful, redirect
//            val user = userOption.get
//            Future.successful(
//              Redirect(routes.LoginController.index()).withSession(
//                request.session + ("connected" -> email) + ("id" -> user.id.toString)
//              )
//            )
//          } else {
//            // Authentication failed, show error message
//            Future.successful(Ok("Wrong email or password"))
//          }
//        }
//      } else {
//        // Email not found, add to database and authenticate
//        loginDao.insert1(email, password).flatMap { id =>
//          Future.successful(
//            Redirect(routes.LoginController.index()).withSession(
//              request.session + ("connected" -> email) + ("id" -> id.toString)
//            )
//          )
//        }
//      }
//    }
//  }
//
//
//  def logout = Action.async { implicit request =>
//    Future.successful(Redirect(routes.LoginController.showLogin).withNewSession)
//  }
//}
