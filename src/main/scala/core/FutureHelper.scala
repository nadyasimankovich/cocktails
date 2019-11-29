package core

import com.twitter.util.Future

trait FutureHelper {

  def batchTraverse[T, R](initialSeq: Seq[T], future: T => Future[R]): Future[Seq[(T, R)]] = {
    initialSeq.grouped(10).foldLeft(Future.value(Seq.empty[(T, R)])) { case (f, group) =>
      f.flatMap { res =>
        Future.traverseSequentially(group) { i =>
          future(i).map { r =>
            i -> r
          }
        }.map{_ ++ res}
      }
    }
  }
}
