package service

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

object Models {
  case class CocktailInfo(name: String, ingredients: String, recipe: String, link: String)
  case class Drink(strdrink: String, ingredients: Option[Set[String]], strinstructions: String, strdrinkthumb: String)
  case class MyResult(drinks: Seq[CocktailInfo])

  implicit val cocktailDecoder: Decoder[CocktailInfo] = deriveDecoder
  implicit val drinkDecoder: Decoder[Drink] = deriveDecoder
  implicit val myResultDecoder: Decoder[MyResult] = deriveDecoder

  implicit val cocktailEncoder: Encoder[CocktailInfo] = deriveEncoder
  implicit val drinkEncoder: Encoder[Drink] = deriveEncoder
  implicit val myResultEncoder: Encoder[MyResult] = deriveEncoder
}
