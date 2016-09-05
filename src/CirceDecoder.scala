package testy

import io.circe._

trait CirceCoder {
  implicit def circeJsonDecoder[A](implicit dec: Decoder[A]) =
    org.http4s.circe.jsonOf[A]

  implicit def circeJsonEncoder[A](implicit enc: Encoder[A]) =
    org.http4s.circe.jsonEncoderOf[A]
}
