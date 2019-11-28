import java.time.{LocalDateTime, ZoneOffset}

package object db {
  case class CocktailImage(
    name: String,
    recipe: String,
    image: Array[Byte],
    ts: Long = LocalDateTime.now().toInstant(ZoneOffset.UTC).getEpochSecond
  )
}
