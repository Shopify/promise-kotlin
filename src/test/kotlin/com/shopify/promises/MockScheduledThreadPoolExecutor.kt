package com.shopify.promises

import java.util.concurrent.Delayed
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

class MockScheduledThreadPoolExecutor(val taskDelayAssert: (Long, TimeUnit) -> Unit = { _, _ -> }) : ScheduledThreadPoolExecutor(1) {
  var lastScheduledFuture: MockScheduledFuture? = null

  override fun schedule(command: Runnable?, delay: Long, timeUnit: TimeUnit?): ScheduledFuture<*> {
    taskDelayAssert(delay, timeUnit!!)
    command?.run()
    return MockScheduledFuture()
  }

  class MockScheduledFuture : ScheduledFuture<Any> {
    override fun get(timeout: Long, unit: TimeUnit?): Any {
      TODO("not implemented")
    }

    override fun get(): Any {
      TODO("not implemented")
    }

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean = true

    override fun isDone(): Boolean = true

    override fun getDelay(unit: TimeUnit?): Long = 0

    override fun compareTo(other: Delayed?): Int = 0

    override fun isCancelled(): Boolean = false
  }
}