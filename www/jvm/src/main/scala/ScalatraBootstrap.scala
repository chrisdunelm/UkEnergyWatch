import org.scalatra.LifeCycle
import javax.servlet.ServletContext
import org.ukenergywatch.www._

class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext): Unit = {
    context.mount(IndexServlet, "/*")
  }
}
