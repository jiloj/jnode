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
  */
@Singleton
class AppDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ResourceExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] {
  /**
    * This creates the entire persistence layer. This method does not check if the persistence layer already exists.
    * Therefore it should only be called if it does not currenlty exist.
    *
    * @return A future that resolves when the operation is done.
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
