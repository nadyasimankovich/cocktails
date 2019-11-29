package core

import java.util.concurrent.Executor

import com.google.common.util.concurrent.ListenableFuture

import scala.util.{Failure, Success}
import com.twitter.util.{Future => TwitterFuture, Promise => TwitterPromise}

import scala.concurrent.{ExecutionContext,  Future => ScalaFuture, Promise => ScalaPromise}

object FutureUtils {

  /** Convert from a Guava Future to a Twitter Future */
  implicit class GuavaFuture[A](val f: ListenableFuture[A]) {
    def asScala(implicit ec: Executor): ScalaFuture[A] = {
      val promise: ScalaPromise[A] = ScalaPromise[A]
      val runnable = new Runnable {
        override def run(): Unit =

          if (f.isCancelled) promise.failure(new NoSuchElementException("cancelled"))
          else try {
            promise.success(f.get())
          } catch {
            case ex: Throwable => promise.failure(ex)
          }
      }
      f.addListener(runnable, ec)

      promise.future
    }
  }

//  /** Convert from a Twitter Future to a Scala Future */
//  implicit class RichTwitterFuture[A](val tf: TwitterFuture[A]) extends AnyVal {
//    def asScala: ScalaFuture[A] = {
//      val promise: ScalaPromise[A] = ScalaPromise()
//      tf.respond {
//        case Return(value) => promise.success(value)
//        case Throw(exception) => promise.failure(exception)
//      }
//      promise.future
//    }
//  }

  /** Convert from a Scala Future to a Twitter Future */
  implicit class RichScalaFuture[A](val sf: ScalaFuture[A]) extends AnyVal {
    def asTwitter(implicit e: ExecutionContext): TwitterFuture[A] = {
      val promise: TwitterPromise[A] = new TwitterPromise[A]()
      sf.onComplete {
        case Success(value) => promise.setValue(value)
        case Failure(exception) => promise.setException(exception)
      }
      promise
    }
  }
}
