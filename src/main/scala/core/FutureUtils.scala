package core

import java.util.concurrent.Executor

import com.google.common.util.concurrent.ListenableFuture
import com.twitter.util.{Return, Throw, Future => TwitterFuture, Promise => TwitterPromise}

import scala.concurrent.{Future => ScalaFuture, Promise => ScalaPromise}

object FutureUtils {
  implicit class GuavaFuture[A](val f: ListenableFuture[A]) {
    def asScala(implicit ec: Executor): TwitterFuture[A] = {
      val promise: TwitterPromise[A] = TwitterPromise[A]
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

  implicit class RichTwitterFuture[A](val tf: TwitterFuture[A]) extends AnyVal {
    def asScala: ScalaFuture[A] = {
      val promise: ScalaPromise[A] = ScalaPromise()
      tf.respond {
        case Return(value) => promise.success(value)
        case Throw(exception) => promise.failure(exception)
      }
      promise.future
    }
  }
}