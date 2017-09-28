/*
 *   The MIT License (MIT)
 *
 *   Copyright (c) 2017 Shopify Inc.
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 */
@file:JvmName("PromiseScheduling")

package com.shopify.promises

import java.util.concurrent.Executor
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Schedule execution of this [Promise] on provided executor
 *
 * @param executor to be scheduled on
 * @return [Promise]`<T, E>`
 */
fun <T, E> Promise<T, E>.startOn(executor: Executor): Promise<T, E> {
  return Promise<Unit, E> {
    executor.execute { resolve(Unit) }
  }.then { this }
}

/**
 * Schedule delivering results of this promise on provide executor
 *
 * @param executor to be scheduled on
 * @return [Promise]`<T, E>`
 */
fun <T, E> Promise<T, E>.completeOn(executor: Executor): Promise<T, E> {
  return bind { result ->
    Promise<T, E> {
      executor.execute {
        when (result) {
          is Promise.Result.Success -> resolve(result.value)
          is Promise.Result.Error -> reject(result.error)
        }
      }
    }
  }
}

/**
 * Delay execution of this [Promise] for specified time
 *
 * @param delay the time from now to delay execution
 * @param timeUnit  the time unit of the delay parameter
 * @return [Promise]`<T, E>`
 */
fun <T, E> Promise<T, E>.delay(delay: Long, timeUnit: TimeUnit, executor: ScheduledExecutorService): Promise<T, E> {
  return if (delay <= 0) {
    this.startOn(executor)
  } else {
    Promise<Unit, E> {
      val future = executor.schedule({ resolve(Unit) }, delay, timeUnit)
      onCancel { future.cancel(true) }
    }.then { this }
  }
}