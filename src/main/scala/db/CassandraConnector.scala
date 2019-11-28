package db

import java.nio.ByteBuffer

import com.datastax.driver.core.{Cluster, Session}

class CassandraConnector {
  private val cluster: Cluster = Cluster.builder
    .addContactPoint("127.0.0.1")
    .build

  private[db] val session: Session = cluster.connect("cocktails")

  private val insertQuery =
    """
      |insert into cocktails.catalog (name, recipe, image, ts)
      |values (?, ? , ?, ?)
      |""".stripMargin

  private val upsertQuery =
    """
      |update cocktails.catalog
      |set recipe = ?, image = ?, ts = ?
      |where name = ?
      |""".stripMargin

  private val getQuery =
    """
      |select name, recipe, image, ts
      |from cocktails.catalog
      |where name = ?
      |""".stripMargin

  private val insertStatement = session.prepare(insertQuery)
  private val upsertStatement = session.prepare(upsertQuery)
  private val getStatement = session.prepare(getQuery)

  def upsert(cocktail: CocktailImage): Unit = {
    val statement = if (get(cocktail.name).isDefined) {
      upsertStatement
        .bind()
        .setString("recipe", cocktail.recipe)
        .setBytes("image", ByteBuffer.wrap(cocktail.image))
        .setLong("ts", cocktail.ts)
        .setString("name", cocktail.name)
    } else {
      insertStatement
        .bind()
        .setString("name", cocktail.name)
        .setString("recipe", cocktail.recipe)
        .setBytes("image", ByteBuffer.wrap(cocktail.image))
        .setLong("ts", cocktail.ts)
    }

    session.execute(statement)
  }

  def get(name: String): Option[CocktailImage] = {
    println(s"CassandraConnector: $name")
    val row = session.execute(getStatement.bind(name)).one()

    if (row == null) None
    else Some(CocktailImage(
      name = row.getString("name"),
      recipe = row.getString("recipe"),
      image = row.getBytes("image").array(),
      ts = row.getLong("ts")
    ))
  }
}