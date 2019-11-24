import io.circe.Decoder
import io.circe._, io.circe.generic.semiauto._

object Models {
  case class CocktailImage(name: String, recipe: String, image: Array[Byte])
  case class CocktailInfo(name: String, recipe: String, link: String)
  case class Drink(strDrink: String, strInstructions: String, strDrinkThumb: String)
  case class Result(drinks: Seq[Drink])
  case class MyResult(drinks: Seq[CocktailInfo])

  implicit val cocktailDecoder: Decoder[CocktailInfo] = deriveDecoder
  implicit val drinkDecoder: Decoder[Drink] = deriveDecoder
  implicit val resultDecoder: Decoder[Result] = deriveDecoder
  implicit val myResultDecoder: Decoder[MyResult] = deriveDecoder

  implicit val cocktailEncoder: Encoder[CocktailInfo] = deriveEncoder
  implicit val drinkEncoder: Encoder[Drink] = deriveEncoder
  implicit val resultEncoder: Encoder[Result] = deriveEncoder
  implicit val myResultEncoder: Encoder[MyResult] = deriveEncoder
}