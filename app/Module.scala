//    bind(classOf[ApplicationTimer]).asEagerSingleton()
//    bind(classOf[Counter]).to(classOf[AtomicCounter])


import com.google.inject.AbstractModule

import java.time.Clock
import play.api.libs.mailer.{MailerClient, SMTPConfiguration}
import play.api.{Environment, Configuration}


class Module(environment: Environment, configuration: Configuration) extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[Clock]).toInstance(Clock.systemDefaultZone)

    val smtpConfig = SMTPConfiguration(
      host = configuration.get[String]("play.mailer.host"),
      port = configuration.get[Int]("play.mailer.port"),
      ssl = configuration.get[Boolean]("play.mailer.ssl"),
      tls = configuration.get[Boolean]("play.mailer.tls"),
      tlsRequired = configuration.get[Boolean]("play.mailer.tlsRequired"),
      user = configuration.getOptional[String]("play.mailer.user"),
      password = configuration.getOptional[String]("play.mailer.password"),
      debugMode = configuration.get[Boolean]("play.mailer.debug"),
      timeout = configuration.getOptional[Int]("play.mailer.timeout"),
      connectionTimeout = configuration.getOptional[Int]("play.mailer.connectionTimeout"),
      props = configuration.underlying,
      mock = false // Set to true if using in test environment
    )

    bind(classOf[MailerClient]).toInstance(new play.api.libs.mailer.SMTPMailer(smtpConfig))


  }
}


