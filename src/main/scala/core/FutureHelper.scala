package core

import scala.concurrent.Future

trait FutureHelper {

  def batchTraverse[A, B, C](
    initialSeq: Seq[A],
    transformedSeq: Seq[C],
    future: C => Future[B]
  ): Future[Map[A, B]] = {
    transformedSeq.zip(initialSeq)
      .grouped(10)
      .foldLeft(Future.successful(Seq.empty[(A, B)])) { case (result, group) =>
      result.flatMap { res =>
        Future.sequence {
          group.map { case (c, a) =>
            future(c).map { r =>
              a -> r
            }
          }
        }
          .map(_ ++ res)
      }
    }.map(_.toMap)
  }

  def batchTraverse[A, B](initialSeq: Seq[A], future: A => Future[B]): Future[Map[A, B]] = {
    initialSeq
      .grouped(10)
      .foldLeft(Future.successful(Seq.empty[(A, B)])) { case (result, group) =>
        result.flatMap { res =>
          Future.sequence {
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