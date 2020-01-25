package service

import db.CocktailImage
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

object Models {
  case class UserCocktailInfo(name: String, ingredients: String, recipe: String){
    def toDbCocktails: CocktailImage = CocktailImage(
      name = name,
      ingredients = ingredients.split(",").toSet,
      recipe = recipe,
      image = Array.empty
    )

  }
  case class CocktailInfo(name: String, ingredients: String, recipe: String, link: String)
  case class Drink(strDrink: String, ingredients: Option[Set[String]], strInstructions: String, strDrinkThumb: String)
  case class MyResult(drinks: Seq[CocktailInfo])

  implicit val cocktailDecoder: Decoder[CocktailInfo] = deriveDecoder
  implicit val drinkDecoder: Decoder[Drink] = deriveDecoder
  implicit val myResultDecoder: Decoder[MyResult] = deriveDecoder

  implicit val cocktailEncoder: Encoder[CocktailInfo] = deriveEncoder
  implicit val drinkEncoder: Encoder[Drink] = deriveEncoder
  implicit val myResultEncoder: Encoder[MyResult] = deriveEncoder
}
