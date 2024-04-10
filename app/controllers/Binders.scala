// not being used

package controllers

import play.api.mvc.QueryStringBindable

object Binders {
  implicit def bigDecimalQueryStringBindable: QueryStringBindable[BigDecimal] = {
    new QueryStringBindable[BigDecimal] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, BigDecimal]] = {
        params.get(key).flatMap(_.headOption).map { value =>
          try {
            Right(BigDecimal(value))
          } catch {
            case _: NumberFormatException => Left("Cannot parse parameter as BigDecimal")
          }
        }
      }

      override def unbind(key: String, value: BigDecimal): String = {
        key + "=" + value.toString
      }
    }
  }
}
