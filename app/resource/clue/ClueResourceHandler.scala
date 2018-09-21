package resource.clue

import javax.inject.{Inject, Provider}
import play.api.MarkerContext
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}

/**
  * DTO for displaying post information.
  */
case class PostResource(id: String, title: String, body: String)

object PostResource {

  /**
    * Mapping to write a PostResource out as a JSON value.
    */
  implicit val implicitWrites = new Writes[PostResource] {
    def writes(post: PostResource): JsValue = {
      Json.obj(
        "id" -> post.id,
        "title" -> post.title,
        "body" -> post.body
      )
    }
  }
}

/**
  * Controls access to the backend data, returning [[PostResource]]
  */
class ClueResourceHandler @Inject()(
    routerProvider: Provider[ClueRouter],
    postRepository: PostRepository)(implicit ec: ExecutionContext) {

  def lookup(id: String)(implicit mc: MarkerContext): Future[Option[PostResource]] = {
    val postFuture = postRepository.get(PostId(id))
    postFuture.map { maybePostData =>
      maybePostData.map { postData =>
        createPostResource(postData)
      }
    }
  }

  def find(implicit mc: MarkerContext): Future[Iterable[PostResource]] = {
    postRepository.list().map { postDataList =>
      postDataList.map(postData => createPostResource(postData))
    }
  }

  private def createPostResource(p: PostData): PostResource = {
    PostResource(p.id.toString, p.title, p.body)
  }

}
