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
    retryPromise<String, RuntimeException>(2, TimeUnit.SECONDS, MockScheduledThreadPoolExecutor()) {
      generatedPromiseCount++
      Promise.ofError(RuntimeException("boom"))
    }.maxAttempts(4)
      .promise()
      .checkPromiseResult {
        Truth.assertThat(generatedPromiseCount).isEqualTo(5)
        Truth.assertThat(it).isInstanceOf(Promise.Result.Error::class.java)
      }
  }

  @Test
  fun delayWithoutBackoffMultiplier() {
    val scheduler = MockScheduledThreadPoolExecutor { delay, timeUnit ->
      Truth.assertThat(timeUnit).isEqualTo(TimeUnit.MILLISECONDS)
      Truth.assertThat(delay).isEqualTo(TimeUnit.SECONDS.toMillis(2))
    }
    retryPromise<String, RuntimeException>(2, TimeUnit.SECONDS, scheduler) {
      Promise.ofError(RuntimeException("boom"))
    }.maxAttempts(2)
      .promise()
      .checkPromiseResult {
        Truth.assertThat(it).isInstanceOf(Promise.Result.Error::class.java)
      }
  }

  @Test
  fun delayWithBackoffMultiplier() {
    var generatedPromiseCount = 0
    val scheduler = MockScheduledThreadPoolExecutor { delay, timeUnit ->
      Truth.assertThat(timeUnit).isEqualTo(TimeUnit.MILLISECONDS)
      Truth.assertThat(delay).isEqualTo(delayForAttempt(generatedPromiseCount - 1, TimeUnit.SECONDS.toMillis(2), 1.2f))
    }
    retryPromise<String, RuntimeException>(2, TimeUnit.SECONDS, scheduler) {
      generatedPromiseCount++
      Promise.ofError(RuntimeException("boom"))
    }.maxAttempts(4)
      .backoffMultiplier(1.2f)
      .promise()
      .checkPromiseResult {
        Truth.assertThat(it).isInstanceOf(Promise.Result.Error::class.java)
      }
  }

  @Test
  fun errorCondition() {
    var generatedPromiseCount = 0
    retryPromise<String, RuntimeException>(2, TimeUnit.SECONDS, MockScheduledThreadPoolExecutor()) {
      if (generatedPromiseCount++ == 4) {
        Promise.ofError(IllegalStateException("boom"))
      } else {
        Promise.ofError(NullPointerException("boom"))
      }
    }.maxAttempts(10)
      .ifError { it is NullPointerException }
      .promise()
      .checkPromiseResult {
        Truth.assertThat(it).isInstanceOf(Promise.Result.Error::class.java)
        Truth.assertThat((it as Promise.Result.Error).error).isInstanceOf(IllegalStateException::class.java)
      }

    Truth.assertThat(generatedPromiseCount).isEqualTo(5)
  }

  @Test
  fun successCondition() {
    var generatedPromiseCount = 0
    retryPromise<Int, RuntimeException>(2, TimeUnit.SECONDS, MockScheduledThreadPoolExecutor()) {
      Promise.ofSuccess(generatedPromiseCount++)
    }.maxAttempts(10)
      .ifSuccess { it < 4 }
      .promise()
      .checkPromiseResult {
        Truth.assertThat(it).isInstanceOf(Promise.Result.Success::class.java)
        Truth.assertThat((it as Promise.Result.Success).value).isEqualTo(4)
      }

    Truth.assertThat(generatedPromiseCount).isEqualTo(5)
  }

  @Test
  fun successAndErrorCondition() {
    var generatedPromiseCount = 0
    retryPromise<Int, RuntimeException>(2, TimeUnit.SECONDS, MockScheduledThreadPoolExecutor()) {
      generatedPromiseCount++
      if (generatedPromiseCount < 4) {
        Promise.ofError(NullPointerException("boom"))
      } else {
        Promise.ofSuccess(generatedPromiseCount)
      }
    }.maxAttempts(10)
      .ifError { it is NullPointerException }
      .ifSuccess { it < 6 }
      .promise()
      .checkPromiseResult {
        Truth.assertThat(it).isInstanceOf(Promise.Result.Success::class.java)
        Truth.assertThat((it as Promise.Result.Success).value).isEqualTo(6)
      }

    Truth.assertThat(generatedPromiseCount).isEqualTo(6)
  }

  @Test
  fun cancelRetry() {
    val maxRetryCount = 2
    var generatedPromiseCount = 0

    var retryPromise: Promise<String, RuntimeException>? = null

    retryPromise = retryPromise(1, TimeUnit.SECONDS, MockScheduledThreadPoolExecutor()) {
      generatedPromiseCount++

      if (generatedPromiseCount == maxRetryCount) {
        retryPromise?.cancel()
      }

      if (generatedPromiseCount > maxRetryCount) {
        Assert.fail("Expected to be canceled")
      }

      Promise.ofError<String, RuntimeException>(RuntimeException("boom"))
    }.maxAttempts(10).promise()

    retryPromise.whenComplete { Assert.fail("Expected to be canceled") }

    Truth.assertThat(generatedPromiseCount == maxRetryCount)
  }

  private fun <T, E> Promise<T, E>.checkPromiseResult(assert: (Promise.Result<T, E>) -> Unit) {
    var result: Promise.Result<T, E>? = null
    whenComplete {
      result = it
    }
    result?.run { assert(this) } ?: Assert.fail("Expected result")
  }
}