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
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

class SchedulingTestCase {

  @Test fun task_in_background() {
    val latch = CountDownLatch(1)
    val threadRef = AtomicReference<Thread>()
    val mainThread = Thread.currentThread()
    Promise<String, RuntimeException> {
      threadRef.set(Thread.currentThread())
      latch.countDown()
    }
      .startOn(Executors.newSingleThreadExecutor())
      .whenComplete { }

    latch.await()

    assertThat(threadRef.get()).isNotEqualTo(mainThread)
  }

  @Test fun task_in_main() {
    val threadRef = AtomicReference<Thread>()
    val mainThread = Thread.currentThread()
    Promise<String, RuntimeException> {
      threadRef.set(Thread.currentThread())
    }.whenComplete { }

    assertThat(threadRef.get()).isEqualTo(mainThread)
  }

  @Test fun on_start_in_background() {
    val latch = CountDownLatch(1)
    val threadRef = AtomicReference<Thread>()
    val mainThread = Thread.currentThread()
    Promise<String, RuntimeException> {}
      .onStart {
        threadRef.set(Thread.currentThread())
        latch.countDown()
      }
      .startOn(Executors.newSingleThreadExecutor())
      .whenComplete { }

    latch.await()

    assertThat(threadRef.get()).isNotEqualTo(mainThread)
  }

  @Test fun on_start_in_main() {
    val threadRef = AtomicReference<Thread>()
    val mainThread = Thread.currentThread()
    Promise<String, RuntimeException> {}
      .startOn(Executors.newSingleThreadExecutor())
      .onStart { threadRef.set(Thread.currentThread()) }
      .whenComplete { }

    assertThat(threadRef.get()).isEqualTo(mainThread)
  }

  @Test fun on_success_in_background() {
    val latch = CountDownLatch(1)
    val threadRef = AtomicReference<Thread>()
    val mainThread = Thread.currentThread()
    Promise<String, RuntimeException> { onSuccess("Done") }
      .completeOn(Executors.newSingleThreadExecutor())
      .onSuccess {
        threadRef.set(Thread.currentThread())
        latch.countDown()
      }
      .whenComplete { }

    latch.await()

    assertThat(threadRef.get()).isNotEqualTo(mainThread)
  }

  @Test fun on_success_in_main() {
    val threadRef = AtomicReference<Thread>()
    val mainThread = Thread.currentThread()
    Promise<String, RuntimeException> { onSuccess("Done") }
      .onSuccess { threadRef.set(Thread.currentThread()) }
      .completeOn(Executors.newSingleThreadExecutor())
      .whenComplete { }

    assertThat(threadRef.get()).isEqualTo(mainThread)
  }

  @Test fun on_error_in_background() {
    val latch = CountDownLatch(1)
    val threadRef = AtomicReference<Thread>()
    val mainThread = Thread.currentThread()
    Promise<String, RuntimeException> { onError(RuntimeException()) }
      .completeOn(Executors.newSingleThreadExecutor())
      .onError {
        threadRef.set(Thread.currentThread())
        latch.countDown()
      }
      .whenComplete { }

    latch.await()

    assertThat(threadRef.get()).isNotEqualTo(mainThread)
  }

  @Test fun on_error_in_main() {
    val threadRef = AtomicReference<Thread>()
    val mainThread = Thread.currentThread()
    Promise<String, RuntimeException> { onError(RuntimeException()) }
      .onError { threadRef.set(Thread.currentThread()) }
      .completeOn(Executors.newSingleThreadExecutor())
      .whenComplete { }

    assertThat(threadRef.get()).isEqualTo(mainThread)
  }

  @Test fun when_complete_in_background() {
    val latch = CountDownLatch(1)
    val threadRef = AtomicReference<Thread>()
    val mainThread = Thread.currentThread()
    Promise<String, RuntimeException> { onSuccess("Done") }
      .completeOn(Executors.newSingleThreadExecutor())
      .whenComplete {
        threadRef.set(Thread.currentThread())
        latch.countDown()
      }

    latch.await()

    assertThat(threadRef.get()).isNotEqualTo(mainThread)
  }

  @Test fun when_complete_in_main() {
    val threadRef = AtomicReference<Thread>()
    val mainThread = Thread.currentThread()
    Promise<String, RuntimeException> { onSuccess("Done") }
      .whenComplete {
        threadRef.set(Thread.currentThread())
      }

    assertThat(threadRef.get()).isEqualTo(mainThread)
  }

  @Test fun task_and_when_complete_in_separate_thread() {
    val latch = CountDownLatch(2)
    val taskThreadRef = AtomicReference<Thread>()
    val whenCompleteThreadRef = AtomicReference<Thread>()
    Promise<String, RuntimeException> {
      taskThreadRef.set(Thread.currentThread())
      onSuccess("Done")
      latch.countDown()
    }
      .startOn(Executors.newSingleThreadExecutor())
      .completeOn(Executors.newSingleThreadExecutor())
      .whenComplete {
        whenCompleteThreadRef.set(Thread.currentThread())
        latch.countDown()
      }

    latch.await()

    assertThat(taskThreadRef.get()).isNotEqualTo(whenCompleteThreadRef.get())
  }

  @Test fun when_complete_in_last_task_thread() {
    val latch = CountDownLatch(3)
    val task1ThreadRef = AtomicReference<Thread>()
    val task2ThreadRef = AtomicReference<Thread>()
    val whenCompleteThreadRef = AtomicReference<Thread>()
    Promise<String, RuntimeException> {
      task1ThreadRef.set(Thread.currentThread())
      onSuccess("Done")
      latch.countDown()
    }.then {
      Promise<String, RuntimeException> {
        task2ThreadRef.set(Thread.currentThread())
        onSuccess("Done!")
        latch.countDown()
      }
        .startOn(Executors.newSingleThreadExecutor())
    }
      .startOn(Executors.newSingleThreadExecutor())
      .whenComplete {
        whenCompleteThreadRef.set(Thread.currentThread())
        latch.countDown()
      }

    latch.await()

    assertThat(task1ThreadRef.get()).isNotEqualTo(whenCompleteThreadRef.get())
    assertThat(task2ThreadRef.get()).isEqualTo(whenCompleteThreadRef.get())
    assertThat(task1ThreadRef.get()).isNotEqualTo(task2ThreadRef.get())
  }

  @Test fun tasks_and_when_complete_in_separate_thread() {
    val latch = CountDownLatch(3)
    val task1ThreadRef = AtomicReference<Thread>()
    val task2ThreadRef = AtomicReference<Thread>()
    val whenCompleteThreadRef = AtomicReference<Thread>()
    Promise<String, RuntimeException> {
      task1ThreadRef.set(Thread.currentThread())
      onSuccess("Done")
      latch.countDown()
    }.then {
      Promise<String, RuntimeException> {
        task2ThreadRef.set(Thread.currentThread())
        onSuccess("Done!")
        latch.countDown()
      }
        .startOn(Executors.newSingleThreadExecutor())
    }
      .startOn(Executors.newSingleThreadExecutor())
      .completeOn(Executors.newSingleThreadExecutor())
      .whenComplete {
        whenCompleteThreadRef.set(Thread.currentThread())
        latch.countDown()
      }

    latch.await()

    assertThat(task1ThreadRef.get()).isNotEqualTo(whenCompleteThreadRef.get())
    assertThat(task2ThreadRef.get()).isNotEqualTo(whenCompleteThreadRef.get())
    assertThat(task1ThreadRef.get()).isNotEqualTo(task2ThreadRef.get())
  }
}