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
    val promise1: Promise<Long, Nothing> = Promise.of(1)
    val promise2: Promise<String, RuntimeException> = Promise.ofSuccess("success")
    val promise3: Promise<String, IOException> = Promise.ofError(IOException())
    val promise4: Promise<String, RuntimeException> = Promise {}
  }

  @Test fun static_value() {
    Promise.of("Done")
      .whenComplete {
        assertThat(it).isInstanceOf(Promise.Result.Success::class.java)
        with(it as Promise.Result.Success) {
          assertThat(value).isEqualTo("Done")
        }
      }

    Promise.ofSuccess<String, RuntimeException>("Done")
      .whenComplete {
        assertThat(it).isInstanceOf(Promise.Result.Success::class.java)
        with(it as Promise.Result.Success) {
          assertThat(value).isEqualTo("Done")
        }
      }

    Promise.ofError<String, RuntimeException>(RuntimeException("failed"))
      .whenComplete {
        assertThat(it).isInstanceOf(Promise.Result.Error::class.java)
        with(it as Promise.Result.Error) {
          assertThat(error.message).isEqualTo("failed")
        }
      }
  }

  @Test fun simple_task_promise() {
    Promise<String, RuntimeException> {
      onSuccess("Done")
    }.whenComplete {
      assertThat(it).isInstanceOf(Promise.Result.Success::class.java)
      with(it as Promise.Result.Success) {
        assertThat(value).isEqualTo("Done")
      }
    }
  }

  @Test fun map() {
    Promise<String, RuntimeException> {
      onSuccess("Done")
    }.map {
      assertThat(it).isEqualTo("Done")
      "Done!"
    }.whenComplete {
      assertThat(it).isInstanceOf(Promise.Result.Success::class.java)
      with(it as Promise.Result.Success) {
        assertThat(value).isEqualTo("Done!")
      }
    }
  }

  @Test fun then() {
    Promise<String, RuntimeException> {
      onSuccess("Done")
    }.then {
      Promise.ofSuccess<String, RuntimeException>("Done!")
    }.whenComplete {
      assertThat(it).isInstanceOf(Promise.Result.Success::class.java)
      with(it as Promise.Result.Success) {
        assertThat(value).isEqualTo("Done!")
      }
    }
  }

  @Test fun map_error() {
    Promise<String, RuntimeException> {
      onError(RuntimeException("failed"))
    }.mapError {
      assertThat(it.message).isEqualTo("failed")
      RuntimeException("failed!")
    }.whenComplete {
      assertThat(it).isInstanceOf(Promise.Result.Error::class.java)
      with(it as Promise.Result.Error) {
        assertThat(error.message).isEqualTo("failed!")
      }
    }
  }

  @Test fun error_then() {
    Promise<String, RuntimeException> {
      onError(RuntimeException("failed"))
    }.errorThen {
      assertThat(it.message).isEqualTo("failed")
      Promise.ofError<String, RuntimeException>(RuntimeException("failed!"))
    }.whenComplete {
      assertThat(it).isInstanceOf(Promise.Result.Error::class.java)
      with(it as Promise.Result.Error) {
        assertThat(error.message).isEqualTo("failed!")
      }
    }
  }

  @Test fun on_start() {
    var onStart = false
    Promise<String, RuntimeException> {
      assertThat(onStart).isTrue()
      onSuccess("Done")
    }.onStart {
      onStart = true
    }.whenComplete {}
  }

  @Test fun on_start_static_value() {
    var onStart = false
    Promise
      .of("Done")
      .onStart {
        onStart = true
      }
      .whenComplete {
        assertThat(onStart).isTrue()
      }
  }

  @Test fun on_success() {
    var onSuccess = false
    Promise<String, RuntimeException> {
      onSuccess("Done")
    }.onSuccess {
      onSuccess = true
    }.whenComplete {
      assertThat(onSuccess).isTrue()
    }
  }

  @Test fun on_success_static_value() {
    var onSuccess = false
    Promise<String, RuntimeException> {
      onSuccess("Done")
    }.onSuccess {
      onSuccess = true
    }.whenComplete {
      assertThat(onSuccess).isTrue()
    }
  }

  @Test fun ignore_error() {
    Promise<String, RuntimeException> {
      onError(RuntimeException())
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
    Promise.all(
      Promise<String, RuntimeException> {
        onSuccess("a")
      },
      Promise<String, RuntimeException> {
        onSuccess("b")
      },
      Promise<String, RuntimeException> {
        onSuccess("c")
      }
    ).whenComplete {
      when (it) {
        is Promise.Result.Success -> assertThat(it.value.contentToString()).isEqualTo("[a, b, c]")
        is Promise.Result.Error -> fail("Expected success")
      }
    }
  }

  @Test fun all_error() {
    Promise.all(
      Promise<String, RuntimeException> {
        onSuccess("a")
      },
      Promise<String, RuntimeException> {
        onError(RuntimeException("Failed"))
      },
      Promise<String, RuntimeException> {
        onSuccess("c")
      }
    ).whenComplete {
      when (it) {
        is Promise.Result.Success -> fail("Expected failure")
        is Promise.Result.Error -> assertThat(it.error.message).isEqualTo("Failed")
      }
    }
  }

  @Test fun all_heterogeneous_2() {
    Promise.all(
      Promise<String, RuntimeException> {
        onSuccess("a")
      },
      Promise<Long, RuntimeException> {
        onSuccess(1)
      }
    ).whenComplete {
      when (it) {
        is Promise.Result.Success -> {
          assertThat(it.value.value1).isEqualTo("a")
          assertThat(it.value.value2).isEqualTo(1)
        }
        is Promise.Result.Error -> fail("Expected success")
      }
    }
  }

  @Test fun all_heterogeneous_3() {
    Promise.all(
      Promise<String, RuntimeException> {
        onSuccess("a")
      },
      Promise<Long, RuntimeException> {
        onSuccess(1)
      },
      Promise<Boolean, RuntimeException> {
        onSuccess(true)
      }
    ).whenComplete {
      when (it) {
        is Promise.Result.Success -> {
          assertThat(it.value.value1).isEqualTo("a")
          assertThat(it.value.value2).isEqualTo(1)
          assertThat(it.value.value3).isTrue()
        }
        is Promise.Result.Error -> fail("Expected success")
      }
    }
  }

  @Test fun all_heterogeneous_4() {
    Promise.all(
      Promise<String, RuntimeException> {
        onSuccess("a")
      },
      Promise<Long, RuntimeException> {
        onSuccess(1)
      },
      Promise<Boolean, RuntimeException> {
        onSuccess(true)
      },
      Promise<Double, RuntimeException> {
        onSuccess(1.5)
      }
    ).whenComplete {
      when (it) {
        is Promise.Result.Success -> {
          assertThat(it.value.value1).isEqualTo("a")
          assertThat(it.value.value2).isEqualTo(1)
          assertThat(it.value.value3).isTrue()
          assertThat(it.value.value4).isWithin(1.5)
        }
        is Promise.Result.Error -> fail("Expected success")
      }
    }
  }

  @Test fun any() {
    Promise.any(
      Promise<String, RuntimeException> {
        onSuccess("a")
      },
      Promise<String, RuntimeException> {
        onSuccess("b")
      },
      Promise<String, RuntimeException> {
        onSuccess("c")
      }
    ).whenComplete {
      when (it) {
        is Promise.Result.Success -> assertThat(it.value).isEqualTo("a")
        is Promise.Result.Error -> fail("Expected success")
      }
    }
  }

  @Test fun any_error() {
    Promise.any(
      Promise<String, RuntimeException> {
        onError(RuntimeException("Failed"))
      },
      Promise<String, RuntimeException> {
        onSuccess("b")
      },
      Promise<String, RuntimeException> {
        onSuccess("c")
      }
    ).whenComplete {
      when (it) {
        is Promise.Result.Success -> fail("Expected failure")
        is Promise.Result.Error -> assertThat(it.error.message).isEqualTo("Failed")
      }
    }
  }
}