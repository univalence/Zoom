package zoom

import java.time.Instant
import java.util.UUID

import scala.util.{ Failure, Success, Try }
import io.circe.generic.extras.decoding._
import io.circe.{ Decoder, _ }
import io.circe.generic.auto._
import io.circe.generic.extras.encoding._
import io.circe.parser._
import io.circe.syntax._
import shapeless.the
import zoom._

/*
import io.circe.generic.extras.decoding._
import io.circe._
import io.circe.generic.auto._
import io.circe.generic.extras.encoding._
import io.circe.parser._
import io.circe.syntax._
 */

trait ProjectEncoderDecoderZoom {

  implicit val encodeUUID: Encoder[UUID] = Encoder.encodeString.contramap[UUID](_.toString)
  implicit val encodeInstant: Encoder[Instant] = Encoder.encodeString.contramap[Instant](_.toString)
  implicit val encodeEnvironment: Encoder[Environment] = Encoder.encodeString.contramap[Environment](_.toString)
  //implicit val encodeTGApplication: Encoder[TGApplication] = Encoder.encodeString.contramap[TGApplication](_.toString)
  //implicit val encodeRanking: Encoder[models.Ranking] = Encoder.encodeString.contramap[models.Ranking](_.toString)
  //implicit val encodeEnumEtatProduit: Encoder[models.EnumEtatProduit] = Encoder.encodeString.contramap[models.EnumEtatProduit](_.toString)
  //implicit val encodeAccessAction: Encoder[AccessAction] = Encoder.encodeString.contramap[AccessAction](_.toString)
  //implicit val encodeToggleAction: Encoder[ToggleAction] = Encoder.encodeString.contramap[ToggleAction](_.toString)
  implicit val decodeUUID: Decoder[UUID] = Decoder.decodeString.map[UUID](UUID.fromString)
  implicit val decodeInstant: Decoder[Instant] = Decoder.decodeString.map[Instant](Instant.parse)
  implicit val decodeEnvironment: Decoder[Environment] = Decoder.decodeString.map[Environment](Environment.fromString)
  //implicit val decodeTGApplication: Decoder[TGApplication] = Decoder.decodeString.map[TGApplication](TGApplication.fromString)
  //implicit val decodeEnumEtatProduit: Decoder[models.EnumEtatProduit] = Decoder.decodeString.map[models.EnumEtatProduit](models.EnumEtatProduit.fromString)
  //implicit val decodeAccessRanking: Decoder[Ranking] = Decoder.decodeString.map[Ranking](Ranking.fromString)
  //implicit val decodeAccessAction: Decoder[AccessAction] = Decoder.decodeString.map[AccessAction](AccessAction.fromString)
  //implicit val decodeToggleAction: Decoder[ToggleAction] = Decoder.decodeString.map[ToggleAction](ToggleAction.fromString)
}

object ZoomEventSerde extends ProjectEncoderDecoderZoom {

  /*
  //in case of import optimization
  import io.circe.jawn._
  import io.circe.syntax._
  import io.circe.parser.decode
  import io.circe.syntax._
  import io.circe.generic.extras.Configuration
  import io.circe.generic.extras.auto._

   */

  import io.circe.jawn._
  import io.circe.syntax._
  import io.circe.parser.decode
  import io.circe.syntax._
  import io.circe.generic.extras.Configuration
  import io.circe.generic.extras.auto._

  implicit val customConfig: Configuration = Configuration.default.withDefaults.withDiscriminator("_typehint")

  lazy val decoder: ConfiguredDecoder[ZoomEvent] = the[ConfiguredDecoder[ZoomEvent]]

  def toJson(obj: ZoomEvent): ToJson = {
    ToJson(obj.getClass.getName, obj.asJson(the[ConfiguredObjectEncoder[ZoomEvent]]).noSpaces)
  }
  def fromJson[T <: ZoomEvent](s: String): Try[T] = {
    val error1OrEvent: Either[Error, ZoomEvent] = decode[ZoomEvent](s)(decoder)
    Try {
      error1OrEvent.fold[Try[T]](e ⇒ Failure(e), x ⇒ Try(x.asInstanceOf[T]))
    }.flatten
  }

  case class ToJson private (event_type: String, payload: String)
}