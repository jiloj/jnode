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
    *
    */
  private val SchemaCreation = DBIO.seq(
      RawPages.schema.create,
      Shows.schema.create,
      Categories.schema.create,
      CategoryShows.schema.create,
      Clues.schema.create
  )

  /**
    *
    */
  private val SchemaDeletion = DBIO.seq(
    Clues.schema.drop,
    CategoryShows.schema.drop,
    Categories.schema.drop,
    Shows.schema.drop,
    RawPages.schema.drop
  )

  /**
    *
    */
  private val IndexCreation = DBIO.seq(
    Shows.schema.create,
    Categories.schema.create,
    CategoryShows.schema.create,
    Clues.schema.create
  )

  /**
    *
    */
  private val IndexDeletion = DBIO.seq(
    Clues.schema.drop,
    CategoryShows.schema.drop,
    Categories.schema.drop,
    Shows.schema.drop
  )

  /**
    * This initializes the entire persistence layer. This method does not check if the persistence layer already exists.
    * Therefore it should only be called once when the entire app is created.
    *
    * @return A future that resolves when the creation operation is done.
    */
  def initialize(): Future[Unit] = {
    db.run(SchemaCreation)
  }

  /**
    *
    * @return
    */
  def recreate(): Future[Unit] = {
    val actions = DBIO.seq(SchemaDeletion, SchemaCreation)
    db.run(actions)
  }

  /**
    *
    * @return
    */
  def reindex(): Future[Unit] = {
    val actions = DBIO.seq(IndexDeletion, IndexCreation)
    db.run(actions)
  }
}
