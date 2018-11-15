package model.dao

import javax.inject.{Inject, Singleton}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.dbio.DBIO
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Future

/**
  * A DAO that holds static general operations to the persistence layer. These are not specific to a specific resource
  * but to the entire application.
  *
  * @param dbConfigProvider An injected database provider to use slick database.
  */
@Singleton
class AppDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
  extends HasDatabaseConfigProvider[JdbcProfile] {
  /**
    * This creates the entire persistence layer. This method does not check if the persistence layer already exists.
    * Therefore it should only be called if it does not currently exist.
    *
    * @return A future that resolves when the creation operation is done.
    */
  def create(): Future[Unit] = {
    val schemaCreation = DBIO.seq(
      Shows.schema.create,
      Categories.schema.create,
      CategoryShows.schema.create,
      Clues.schema.create
    )

    db.run(schemaCreation)
  }
}
