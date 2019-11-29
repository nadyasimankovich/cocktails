package core

import com.twitter.util.Future

trait FutureHelper {

  def batchTraverse[T, R](initialSet: Seq[T], future: T => Future[R]): Future[Map[T, R]] = {
    initialSet.grouped(10).foldLeft(Future.value(Seq.empty[(T, R)])) { case (f, group) =>
      f.flatMap { res =>
        Future.traverseSequentially(group.toSeq) { i =>
          future(i).map { r =>
            i -> r
          }
        }.map{_ ++ res}
      }
    }.map(_.toMap)
  }
}
