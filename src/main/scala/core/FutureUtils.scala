package core

import java.util.concurrent.Executor

import com.google.common.util.concurrent.ListenableFuture

import com.twitter.util.{Future, Promise}

object FutureUtils {
  implicit class GuavaFuture[A](val f: ListenableFuture[A]) {
    def asScala(implicit ec: Executor): Future[A] = {
      val promise: Promise[A] = Promise[A]
      val runnable = new Runnable {
        override def run(): Unit =

          if (f.isCancelled) promise.setException(new NoSuchElementException("cancelled"))
          else try {
            promise.setValue(f.get())
          } catch {
            case ex: Throwable => promise.setException(ex)
          }
      }
      f.addListener(runnable, ec)

      promise
    }
  }
}
