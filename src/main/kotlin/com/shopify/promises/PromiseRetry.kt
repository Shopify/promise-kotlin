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
@file:JvmName("PromiseRetryUtils")

package com.shopify.promises

import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Creates [RetryPromiseBuilder] to retry [Promise] execution provided by specified generator function [next]
 *
 * @param delay between the attempts
 * @param timeUnit of the provided delay
 * @param scheduledExecutor executor on which delay is waited before the next attempt of [Promise] execution
 * @param next function to provide [Promise] for the next attempt
 */
fun <T, E> Promise.Companion.retry(delay: Long, timeUnit: TimeUnit, scheduledExecutor: ScheduledExecutorService, next: () -> Promise<T, E>) =
  RetryPromiseBuilder<T, E>(delay, timeUnit, scheduledExecutor, next)

/**
 * [Promise] execution retry builder
 */
class RetryPromiseBuilder<T, E> internal constructor(
  private val delay: Long,
  private val timeUnit: TimeUnit,
  private val scheduledExecutor: ScheduledExecutorService,
  private val next: () -> Promise<T, E>
) {
  private var maxAttempts = 0
  private var backoffMultiplier = 1f
  private var successPredicate: (T) -> Boolean = { false }
  private var errorPredicate: (E) -> Boolean = { true }
  private val condition: (Promise.Result<T, E>) -> Boolean = { result ->
    when (result) {
      is Promise.Result.Success -> successPredicate(result.value)
      is Promise.Result.Error -> errorPredicate(result.error)
    }
  }

  /**
   * The maximum amount of times to retry the [Promise] execution
   *
   * @param value max count
   */
  fun maxAttempts(value: Int) = apply { maxAttempts = Math.max(value, 0) }

  /**
   * The exponential factor to use to calculate the delay
   *
   * @param value backoff multiplier
   */
  fun backoffMultiplier(value: Float) = apply { backoffMultiplier = value }

  /**
   * The predicate to check if [Promise.Result.Success] result is not fulfilled and next retry attempt is required
   *
   * @param predicate to be checked before retrying [Promise] execution
   */
  fun ifSuccess(predicate: (T) -> Boolean) = apply { successPredicate = predicate }

  /**
   * The predicate to check if [Promise.Result.Error] result is not fulfilled and next retry attempt is required
   *
   * @param predicate to be checked before retrying [Promise] execution
   */
  fun ifError(predicate: (E) -> Boolean) = apply { errorPredicate = predicate }

  /**
   * Constructs the promise with incorporated retry execution logic
   */
  fun promise(): Promise<T, E> {

    fun Iterator<IndexedValue<Promise<T, E>>>.retry(subscriber: Promise.Subscriber<T, E>) {
      if (!hasNext()) return

      val (attempt, promise) = next()
      promise.whenComplete { result ->
        if (attempt == maxAttempts || !condition(result)) {
          subscriber.dispatch(result)
        } else {
          retry(subscriber)
        }
      }
    }

    return Promise {
      val canceled = AtomicBoolean()
      val delegateCancel = AtomicReference({})
      onCancel {
        canceled.set(true)
        delegateCancel.get().invoke()
      }
      generateSequence { if (canceled.get()) null else next() }
        .mapIndexed { attempt, promise ->
          if (attempt == 0) promise else promise.delayStart(delayForAttempt(attempt, timeUnit.toMillis(delay), backoffMultiplier), TimeUnit.MILLISECONDS, scheduledExecutor)
        }
        .onEach { delegateCancel.set { it.cancel() } }
        .withIndex()
        .iterator()
        .retry(this)
    }
  }
}

internal fun delayForAttempt(attempt: Int, delay: Long, backoffMultiplier: Float): Long {
  return Math.max((delay * Math.pow(backoffMultiplier.toDouble(), attempt.toDouble())).toLong(), delay)
}