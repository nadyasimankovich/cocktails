import java.time.{LocalDateTime, ZoneOffset}

package object db {

  def currentTs = LocalDateTime.now().toInstant(ZoneOffset.UTC).getEpochSecond

  case class CocktailImage(
    name: String,
    ingredients: Set[String],
    recipe: String,
    image: Array[Byte],
    ts: Long = currentTs
  ) {
    def toIngredientLink: Set[IngredientLink] = {
      ingredients.map(i => IngredientLink(name = i, cocktail = name))
    }
  }

  case class IngredientLink(name: String, cocktail: String)
  case class Ingredient(name: String, cocktails: Set[String])
}
