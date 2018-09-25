package zoom.model

sealed trait Environment {

  import Environment._

  def shortname: String = this match {
    case Production        => "prod"
    case Integration       => "int"
    case RecetteTransverse => "rect"
    case Recette           => "rec"
    case Local             => "local"
  }

}

object Environment {

  //REVIEW : Il doit y avoir un moyen dans Circe / Shapeless de s'occuper des CoProduits
  def fromString(value: String): Environment =
    all.find(_.toString == value).get

  def fromShortname(env: String): Environment =
    env match {
      case "prod"  => Production
      case "rec"   => Recette
      case "rect"  => RecetteTransverse
      case "int"   => Integration
      case "local" => Local
    }

  val all = Vector(Production, Integration, RecetteTransverse, Recette, Local)

  case object Production extends Environment

  case object Integration extends Environment

  case object RecetteTransverse extends Environment

  case object Recette extends Environment

  case object Local extends Environment

}
