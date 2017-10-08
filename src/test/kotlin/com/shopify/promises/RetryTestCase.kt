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
package com.shopify.promises

import com.google.common.truth.Truth
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.TimeUnit

class RetryTestCase {
  @Test
  fun retryCountEqualsPromiseGeneratedCount() {
    var generatedPromiseCount = 0
    val scheduler = MockScheduledThreadPoolExecutor { delay, timeUnit ->
      if (generatedPromiseCount == 1 || generatedPromiseCount == 3) {
        Truth.assertThat(delay).isEqualTo(TimeUnit.SECONDS.toMillis(0))
        Truth.assertThat(timeUnit).isEqualTo(TimeUnit.NANOSECONDS)
      } else {
        Truth.assertThat(delay).isEqualTo(TimeUnit.SECONDS.toMillis(10))
        Truth.assertThat(timeUnit).isEqualTo(TimeUnit.MILLISECONDS)
      }
    }

    val maxAttemptCount = 4
    PromiseGenerator<String, RuntimeException> {
      generatedPromiseCount++
      Promise.ofError(RuntimeException("boom"))
    }.retry(scheduler) { attempt, _ ->
      when (attempt) {
        maxAttemptCount -> RetryPolicy.Cancel
        2 -> RetryPolicy.Immediately
        else -> RetryPolicy.WithDelay(10, TimeUnit.SECONDS)
      }
    }.whenComplete { result ->
      Truth.assertThat(generatedPromiseCount).isAtMost(maxAttemptCount + 1)
      Truth.assertThat(result).isInstanceOf(Promise.Result.Error::class.java)
    }
  }

  @Test
  fun cancelRetry() {
    val maxAttemptCount = 2
    var generatedPromiseCount = 0
    var promise: Promise<String, RuntimeException>? = null
    promise = PromiseGenerator<String, RuntimeException> {
      generatedPromiseCount++
      Promise.ofError(RuntimeException("boom"))
    }.retry(MockScheduledThreadPoolExecutor()) { attempt, _ ->
      if (attempt == maxAttemptCount) {
        promise?.cancel()
      } else if (attempt > maxAttemptCount) {
        Assert.fail("Expected to be canceled")
      }
      RetryPolicy.Immediately
    }

    promise.whenComplete {
      Assert.fail("Expected to be canceled")
    }

    Truth.assertThat(generatedPromiseCount).isAtMost(maxAttemptCount + 1)
  }

  @Test
  fun defaultRetryHandlerDelayed() {
    val retryHandler = DefaultRetryHandler.Delayed(5, TimeUnit.SECONDS)
      .backoffMultiplier(2f)
      .maxCount(2)
      .build()

    with(retryHandler(0, Promise.Result.Success<String, RuntimeException>("test"))) {
      Truth.assertThat(this).isEqualTo(RetryPolicy.Cancel)
    }
    with(retryHandler(0, Promise.Result.Error<String, RuntimeException>(RuntimeException()))) {
      Truth.assertThat(this).isEqualTo(RetryPolicy.WithDelay(5, TimeUnit.SECONDS))
    }
    with(retryHandler(1, Promise.Result.Error<String, RuntimeException>(RuntimeException()))) {
      Truth.assertThat(this).isEqualTo(RetryPolicy.WithDelay(10, TimeUnit.SECONDS))
    }
    with(retryHandler(2, Promise.Result.Error<String, RuntimeException>(RuntimeException()))) {
      Truth.assertThat(this).isEqualTo(RetryPolicy.WithDelay(20, TimeUnit.SECONDS))
    }
    with(retryHandler(3, Promise.Result.Error<String, RuntimeException>(RuntimeException()))) {
      Truth.assertThat(this).isEqualTo(RetryPolicy.Cancel)
    }
  }

  @Test
  fun defaultRetryHandlerConditional() {
    val retryHandler = DefaultRetryHandler.Conditional<String, RuntimeException>(5, TimeUnit.SECONDS)
      .retryWhen { result ->
        when (result) {
          is Promise.Result.Success -> result.value == "retry"
          is Promise.Result.Error -> result.error.message == "retry"
        }
      }
      .backoffMultiplier(2f)
      .maxCount(2)
      .build()

    with(retryHandler(0, Promise.Result.Success("retry"))) {
      Truth.assertThat(this).isEqualTo(RetryPolicy.WithDelay(5, TimeUnit.SECONDS))
    }
    with(retryHandler(0, Promise.Result.Success(""))) {
      Truth.assertThat(this).isEqualTo(RetryPolicy.Cancel)
    }
    with(retryHandler(0, Promise.Result.Error(RuntimeException("retry")))) {
      Truth.assertThat(this).isEqualTo(RetryPolicy.WithDelay(5, TimeUnit.SECONDS))
    }
    with(retryHandler(0, Promise.Result.Error(RuntimeException()))) {
      Truth.assertThat(this).isEqualTo(RetryPolicy.Cancel)
    }
    with(retryHandler(0, Promise.Result.Success("retry"))) {
      Truth.assertThat(this).isEqualTo(RetryPolicy.WithDelay(5, TimeUnit.SECONDS))
    }
    with(retryHandler(1, Promise.Result.Error(RuntimeException("retry")))) {
      Truth.assertThat(this).isEqualTo(RetryPolicy.WithDelay(10, TimeUnit.SECONDS))
    }
    with(retryHandler(2, Promise.Result.Success("retry"))) {
      Truth.assertThat(this).isEqualTo(RetryPolicy.WithDelay(20, TimeUnit.SECONDS))
    }
    with(retryHandler(3, Promise.Result.Error(RuntimeException()))) {
      Truth.assertThat(this).isEqualTo(RetryPolicy.Cancel)
    }
  }
}