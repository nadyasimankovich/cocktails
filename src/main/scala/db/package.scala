import java.time.{LocalDateTime, ZoneOffset}

package object db {
  case class CocktailImage(
    name: String,
    ingredients: Set[String],
    recipe: String,
    image: Array[Byte],
    ts: Long = LocalDateTime.now().toInstant(ZoneOffset.UTC).getEpochSecond
  )

  case class IngredientLink(name: String, cocktail: String)
  case class Ingredient(name: String, cocktails: Set[String])

  def cocktailToIngredientLink(c: CocktailImage): Set[IngredientLink] = {
    c.ingredients.map(i => IngredientLink(name = i, cocktail = c.name))
  }

}
