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

import com.google.common.truth.Truth.assertThat
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class CancellationTestCase {

  @Test fun do_on_cancel_task() {
    val latch = CountDownLatch(1)
    val taskCancelCalled = AtomicBoolean()
    val whenCalled = AtomicBoolean()
    val promise = Promise<String, RuntimeException> {
      onCancel {
        taskCancelCalled.set(true)
      }
      Thread.sleep(400)

      resolve("Done")
      latch.countDown()
    }
      .startOn(Executors.newSingleThreadExecutor())
      .whenComplete { whenCalled.set(true) }

    Thread.sleep(200)
    promise.cancel()

    latch.await()

    assertThat(taskCancelCalled.get()).isTrue()
    assertThat(whenCalled.get()).isFalse()
  }

  @Test fun then_not_called() {
    val latch = CountDownLatch(1)
    val canceled = AtomicBoolean()
    val thenCalled = AtomicBoolean()
    val whenCalled = AtomicBoolean()
    val promise = Promise<String, RuntimeException> {
      onCancel {
        canceled.set(true)
      }
      Thread.sleep(400)

      resolve("Done")
      latch.countDown()
    }.then {
      thenCalled.set(true)
      Promise.ofSuccess<String, RuntimeException>("Done!")
    }
      .startOn(Executors.newSingleThreadExecutor())
      .whenComplete { whenCalled.set(true) }

    Thread.sleep(200)
    promise.cancel()

    latch.await()

    assertThat(canceled.get()).isTrue()
    assertThat(thenCalled.get()).isFalse()
    assertThat(whenCalled.get()).isFalse()
  }

  @Test fun chained_promise_canceled() {
    val latch = CountDownLatch(1)
    val task1CancelCalled = AtomicBoolean()
    val task2CancelCalled = AtomicBoolean()
    val whenCalled = AtomicBoolean()
    val promise = Promise<String, RuntimeException> {
      onCancel {
        task1CancelCalled.set(true)
      }
      resolve("Done")
    }.then {
      Promise<String, RuntimeException> {
        onCancel {
          task2CancelCalled.set(true)
        }
        Thread.sleep(400)
        resolve("Done!")
        latch.countDown()
      }
    }
      .startOn(Executors.newSingleThreadExecutor())
      .whenComplete { whenCalled.set(true) }

    Thread.sleep(200)
    promise.cancel()

    latch.await()

    assertThat(task1CancelCalled.get()).isFalse()
    assertThat(task2CancelCalled.get()).isTrue()
    assertThat(whenCalled.get()).isFalse()
  }

  @Test fun static_cancel() {
    val promise = Promise.ofSuccess<String, RuntimeException>("Done")
    promise.cancel()

    promise.whenComplete {
      Assert.fail("Should be canceled")
    }
  }

  @Test fun all_canceled() {
    val latch = CountDownLatch(3)
    val task1CancelCalled = AtomicBoolean()
    val task2CancelCalled = AtomicBoolean()
    val task3CancelCalled = AtomicBoolean()
    val whenCalled = AtomicBoolean()
    val promise = Promise.all(
      Promise<String, RuntimeException> {
        onCancel {
          task1CancelCalled.set(true)
        }
        Thread.sleep(400)
        resolve("Done!")
        latch.countDown()
      }.startOn(Executors.newSingleThreadExecutor()),
      Promise<String, RuntimeException> {
        onCancel {
          task2CancelCalled.set(true)
        }
        Thread.sleep(400)
        resolve("Done!")
        latch.countDown()
      }.startOn(Executors.newSingleThreadExecutor()),
      Promise<String, RuntimeException> {
        onCancel {
          task3CancelCalled.set(true)
        }
        Thread.sleep(400)
        resolve("Done!")
        latch.countDown()
      }.startOn(Executors.newSingleThreadExecutor())
    )
      .whenComplete { whenCalled.set(true) }

    Thread.sleep(200)
    promise.cancel()

    latch.await()

    assertThat(task1CancelCalled.get()).isTrue()
    assertThat(task2CancelCalled.get()).isTrue()
    assertThat(task3CancelCalled.get()).isTrue()
    assertThat(whenCalled.get()).isFalse()
  }
}