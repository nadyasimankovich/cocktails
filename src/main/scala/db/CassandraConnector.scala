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
      |insert into cocktails.catalog (name, recipe, image)
      |values (?, ? , ?)
      |""".stripMargin

  private val getQuery =
    """
      |select name, recipe, image
      |from cocktails.catalog
      |where name = ?
      |""".stripMargin

  private val insertStatement = session.prepare(insertQuery)
  private val getStatement = session.prepare(getQuery)

  def insert(cocktail: CocktailImage): Unit = {
    val statement = insertStatement
      .bind()
      .setString("name", cocktail.name)
      .setString("recipe", cocktail.recipe)
      .setBytes("image", ByteBuffer.wrap(cocktail.image))

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
    ))
  }
}