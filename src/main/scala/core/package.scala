import scala.concurrent.ExecutionContext
import scala.concurrent.forkjoin.ForkJoinPool

package object core {

  implicit val context: ExecutionContext = ExecutionContext.fromExecutor(new ForkJoinPool(20))

}
