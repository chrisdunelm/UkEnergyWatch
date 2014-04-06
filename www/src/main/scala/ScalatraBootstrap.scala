
import org.scalatra._
import javax.servlet.ServletContext
import org.ukenergywatch.www.UkewServlet

class ScalatraBootstrap extends LifeCycle {

  override def init(context: ServletContext) {
    context.mount(UkewServlet, "/*")
  }

}
