package core

import com.twitter.util.Future

trait FutureHelper {

  def batchTraverse[T, R](initialSeq: Seq[T], future: T => Future[R]): Future[Map[T, R]] = {
    initialSeq.grouped(10).foldLeft(Future.value(Seq.empty[(T, R)])) { case (result, group) =>
      result.flatMap { res =>
        Future.collect {
          group.map { i =>
            future(i).map { r =>
              i -> r
            }
          }
        }
          .map(_ ++ res)
      }
    }.map(_.toMap)
  }
}