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
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class BasicPromiseTestCase {

  @Test fun create() {
    Promise.of(1)
    Promise.ofSuccess<String, RuntimeException>("success")
    Promise.ofError<String, IOException>(IOException())
    Promise<String, RuntimeException> {}
  }

  @Test fun static_value() {
    Promise.of("Done").validateSuccess("Done")
    Promise.ofSuccess<String, RuntimeException>("Done").validateSuccess("Done")
    with(RuntimeException("failed")) {
      Promise.ofError<String, RuntimeException>(this).validateError(this)
    }
    Promise<String, RuntimeException> { resolve("Done") }.validateSuccess("Done")
  }

  @Test fun map_same_type() {
    Promise<String, RuntimeException> {
      resolve("Done")
    }.validateMap("Done") {
      "Done!"
    }.validateSuccess("Done!")
  }

  @Test fun map_different_type() {
    Promise<String, RuntimeException> {
      resolve("Done")
    }.validateMap("Done") {
      true
    }.validateSuccess(true)
  }

  @Test fun then_same_type() {
    Promise<String, RuntimeException> {
      resolve("Done")
    }.validateThen("Done") {
      Promise.ofSuccess<String, RuntimeException>("Done!")
    }.validateSuccess("Done!")
  }

  @Test fun then_different_type() {
    Promise<String, RuntimeException> {
      resolve("Done")
    }.validateThen("Done") {
      Promise.ofSuccess<Boolean, RuntimeException>(true)
    }.validateSuccess(true)
  }

  @Test fun map_error_same_type() {
    val error1 = RuntimeException("failed")
    val error2 = RuntimeException("failed!")
    Promise<String, RuntimeException> {
      reject(error1)
    }.validateMapError(error1) {
      error2
    }.validateError(error2)
  }

  @Test fun map_error_different_type() {
    val error1 = RuntimeException("failed")
    val error2 = IOException("failed!")
    Promise<String, RuntimeException> {
      reject(error1)
    }.validateMapError(error1) {
      error2
    }.validateError(error2)
  }

  @Test fun error_then_same_type() {
    val error1 = RuntimeException("failed")
    val error2 = RuntimeException("failed!")
    Promise<String, RuntimeException> {
      reject(error1)
    }.validateErrorThen(error1) {
      Promise.ofError<String, RuntimeException>(error2)
    }.validateError(error2)
  }

  @Test fun error_then_different_type() {
    val error1 = RuntimeException("failed")
    val error2 = IOException("failed!")
    Promise<String, RuntimeException> {
      reject(error1)
    }.validateErrorThen(error1) {
      Promise.ofError<String, IOException>(error2)
    }.validateError(error2)
  }

  @Test fun on_start() {
    var onStart = false
    Promise<String, RuntimeException> {
      assertThat(onStart).isTrue()
      resolve("Done")
    }.onStart {
      onStart = true
    }.validateSuccess("Done") {
      assertThat(onStart).isTrue()
    }
  }

  @Test fun on_start_static_value() {
    var onStart = false
    Promise
      .of("Done")
      .onStart { onStart = true }
      .validateSuccess("Done") {
        assertThat(onStart).isTrue()
      }
  }

  @Test fun on_start_nested() {
    var onStart1 = false
    var onStart2 = false
    Promise<String, RuntimeException> {
      assertThat(onStart1).isTrue()
      assertThat(onStart2).isFalse()
      resolve("Done")
    }.then {
      Promise<String, RuntimeException> {
        assertThat(onStart1).isTrue()
        assertThat(onStart2).isTrue()
        resolve("Done!")
      }.onStart { onStart2 = true }
    }.onStart {
      onStart1 = true
    }.validateSuccess("Done!") {
      assertThat(onStart1).isTrue()
      assertThat(onStart2).isTrue()
    }
  }

  @Test fun on_success() {
    var onSuccess = false
    Promise<String, RuntimeException> {
      resolve("Done")
    }.onResolve {
      onSuccess = true
    }.validateSuccess("Done") {
      assertThat(onSuccess).isTrue()
    }
  }

  @Test fun on_success_static_value() {
    var onSuccess = false
    Promise<String, RuntimeException> {
      resolve("Done")
    }.onResolve {
      onSuccess = true
    }.validateSuccess("Done") {
      assertThat(onSuccess).isTrue()
    }
  }

  @Test fun ignore_error() {
    Promise<String, RuntimeException> {
      reject(RuntimeException())
    }.ignoreError().whenComplete {
      fail("Should never be resolved")
    }
  }

  @Test fun ignore_error_static_value() {
    Promise.ofError<String, RuntimeException>(RuntimeException())
      .ignoreError()
      .whenComplete {
        fail("Should never be resolved")
      }
  }

  @Test fun all_success() {
    Promise
      .all(
        Promise<String, RuntimeException> {
          resolve("a")
        },
        Promise<String, RuntimeException> {
          resolve("b")
        },
        Promise<String, RuntimeException> {
          resolve("c")
        }
      )
      .map { it.contentToString() }
      .validateSuccess("[a, b, c]")
  }

  @Test fun all_error() {
    val error = RuntimeException("Failed")
    Promise
      .all(
        Promise<String, RuntimeException> {
          resolve("a")
        },
        Promise<String, RuntimeException> {
          reject(error)
        },
        Promise<String, RuntimeException> {
          resolve("c")
        }
      )
      .validateError(error)
  }

  @Test fun all_heterogeneous_2() {
    Promise.all(
      Promise<String, RuntimeException> {
        resolve("a")
      },
      Promise<Long, RuntimeException> {
        resolve(1)
      }
    ).validateSuccess(Tuple<String, Long>("a", 1))
  }

  @Test fun all_heterogeneous_3() {
    Promise.all(
      Promise<String, RuntimeException> {
        resolve("a")
      },
      Promise<Long, RuntimeException> {
        resolve(1)
      },
      Promise<Boolean, RuntimeException> {
        resolve(true)
      }
    ).validateSuccess(Tuple3<String, Long, Boolean>("a", 1, true))
  }

  @Test fun all_heterogeneous_4() {
    Promise.all(
      Promise<String, RuntimeException> {
        resolve("a")
      },
      Promise<Long, RuntimeException> {
        resolve(1)
      },
      Promise<Boolean, RuntimeException> {
        resolve(true)
      },
      Promise<Double, RuntimeException> {
        resolve(1.5)
      }
    ).validateSuccess(Tuple4<String, Long, Boolean, Double>("a", 1, true, 1.5))
  }

  @Test fun any() {
    Promise.any(
      Promise<String, RuntimeException> {
        resolve("a")
      },
      Promise<String, RuntimeException> {
        resolve("b")
      },
      Promise<String, RuntimeException> {
        resolve("c")
      }
    ).validateSuccess("a")
  }

  @Test fun any_error() {
    val error = RuntimeException("Failed")
    Promise.any(
      Promise<String, RuntimeException> {
        reject(error)
      },
      Promise<String, RuntimeException> {
        resolve("b")
      },
      Promise<String, RuntimeException> {
        resolve("c")
      }
    ).validateError(error)
  }

  @Test(expected = IllegalStateException::class) fun resolve_multiple_times() {
    val promise = Promise<String, RuntimeException> {
      resolve("first")
      resolve("second")
    }

    promise.whenComplete { }
  }

  private fun <T, T1, E> Promise<T, E>.validateMap(expected: T, transform: (T) -> T1): Promise<T1, E> {
    return map {
      assertThat(it).isEqualTo(expected)
      transform(it)
    }
  }

  private fun <T, T1, E> Promise<T, E>.validateThen(expected: T, transform: (T) -> Promise<T1, E>): Promise<T1, E> {
    return then {
      assertThat(it).isEqualTo(expected)
      transform(it)
    }
  }

  private fun <T, E, E1> Promise<T, E>.validateMapError(expected: E, transform: (E) -> E1): Promise<T, E1> {
    return mapError {
      assertThat(it).isEqualTo(expected)
      transform(it)
    }
  }

  private fun <T, E, E1> Promise<T, E>.validateErrorThen(expected: E, transform: (E) -> Promise<T, E1>): Promise<T, E1> {
    return errorThen {
      assertThat(it).isEqualTo(expected)
      transform(it)
    }
  }

  private fun <T, E> Promise<T, E>.validateSuccess(expected: T, onComplete: (Promise.Result<T, E>) -> Unit = {}) {
    validateWhenComplete(this, Promise.Result.Success(expected), onComplete)
  }

  private fun <T, E> Promise<T, E>.validateError(expected: E, onComplete: (Promise.Result<T, E>) -> Unit = {}) {
    validateWhenComplete(this, Promise.Result.Error(expected), onComplete)
  }

  private fun <T, E> validateWhenComplete(promise: Promise<T, E>, expected: Promise.Result<T, E>, onComplete: (Promise.Result<T, E>) -> Unit = {}) {
    promise.whenComplete {
      when (it) {
        is Promise.Result.Success -> {
          if (expected is Promise.Result.Success) {
            assertThat(it.value).isEqualTo(expected.value)
          } else {
            fail("Promise expected to fail")
          }
        }
        is Promise.Result.Error -> {
          if (expected is Promise.Result.Error) {
            assertThat(it.error).isEqualTo(expected.error)
          } else {
            fail("Promise expected to succeed")
          }
        }
      }

      onComplete(it)
    }
  }
}