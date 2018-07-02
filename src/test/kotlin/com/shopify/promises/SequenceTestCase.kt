package com.shopify.promises

import com.google.common.truth.Truth
import org.junit.Assert
import org.junit.Test

class SequenceTestCase {

  @Test
  fun generatePromiseSequenceWithSeedPromiseError() {
    generatePromiseSequence(Promise.ofError<Int, Throwable>(RuntimeException("boom"))) {
      Assert.fail("Called when is not supposed to")
      Promise.ofSuccess(it + 1)
    }.checkPromiseErrorResult {
      Truth.assertThat(it).isInstanceOf(RuntimeException::class.java)
    }
  }

  @Test
  fun generatePromiseSequenceWithSeedPromiseSuccess() {
    generatePromiseSequence(Promise.ofSuccess<Int, Throwable>(0)) {
      Promise.ofSuccess(it + 1)
    }.checkPromiseSuccessResult {
      it.take(3).forEachIndexed { index, promise ->
        promise.checkPromiseSuccessResult {
          Truth.assertThat(it).isEqualTo(index + 1)
        }
      }
    }
  }

  @Test
  fun generatePromiseSequenceWithSeedPromiseResultSuccess() {
    generatePromiseSequence(Promise.Result.Success<Int, Throwable>(0)) {
      Truth.assertThat(it).isInstanceOf(Promise.Result.Success::class.java)
      Promise.ofSuccess((it as Promise.Result.Success).value + 1)
    }.checkPromiseSuccessResult {
      it.take(3).forEachIndexed { index, promise ->
        promise.checkPromiseSuccessResult {
          Truth.assertThat(it).isEqualTo(index + 1)
        }
      }
    }
  }

  @Test
  fun generatePromiseSequenceWithSeedPromiseResultError() {
    var generatedCount = 0
    generatePromiseSequence(Promise.Result.Error<Int, Throwable>(RuntimeException("boom"))) {
      Promise.ofSuccess(generatedCount++)
    }.checkPromiseSuccessResult {
      it.take(3).forEach { promise ->
        promise.checkPromiseResult {
          if (generatedCount == 0) {
            Truth.assertThat(it).isInstanceOf(Promise.Result.Error::class.java)
          } else {
            Truth.assertThat(it).isInstanceOf(Promise.Result.Success::class.java)
            Truth.assertThat((it as Promise.Result.Success).value).isEqualTo(generatedCount - 1)
          }
        }
      }
    }
  }

  @Test
  fun generatePromiseSequenceWithNoSeed() {
    var generatedCount = 0
    generatePromiseSequence<Int, Throwable> {
      Promise.ofSuccess(generatedCount++)
    }.checkPromiseSuccessResult {
      it.take(3).forEach { promise ->
        promise.checkPromiseSuccessResult {
          Truth.assertThat(it).isEqualTo(generatedCount - 1)
        }
      }
    }
  }

  @Test
  fun generatePromiseSequenceWithSeedPromiseCanceled() {
    generatePromiseSequence(Promise.ofSuccess<Int, Throwable>(0).apply { cancel() }) {
      Assert.fail("Called when is not supposed to")
      Promise.ofSuccess(it + 1)
    }.whenComplete {
      Assert.fail("Called when is not supposed to")
    }
  }

  @Test
  fun generatePromiseSequenceWithSeedPromiseNextError() {
    generatePromiseSequence(Promise.ofSuccess<Int, Throwable>(0)) {
      if (it == 0) {
        Promise.ofSuccess(1)
      } else {
        Promise.ofError(RuntimeException("boom"))
      }
    }.checkPromiseSuccessResult {
      it.forEachIndexed { index, promise ->
        when (index) {
          0 -> promise.checkPromiseSuccessResult {
            Truth.assertThat(it).isEqualTo(index + 1)
          }
          1 -> promise.checkPromiseErrorResult { }
          else -> Assert.fail("Expected only one item")
        }
      }
    }
  }

  @Test
  fun generatePromiseSequenceWithSeedPromiseResultNextError() {
    generatePromiseSequence(Promise.Result.Success<Int, Throwable>(0)) {
      if (it is Promise.Result.Success) {
        if (it.value == 0) {
          Promise.ofSuccess<Int, Throwable>(1)
        } else {
          Promise.ofError<Int, Throwable>(RuntimeException("boom"))
        }
      } else {
        null
      }
    }.checkPromiseSuccessResult {
      it.forEachIndexed { index, promise ->
        when (index) {
          0 -> promise.checkPromiseSuccessResult {
            Truth.assertThat(it).isEqualTo(index + 1)
          }
          1 -> promise.checkPromiseErrorResult { }
          else -> Assert.fail("Expected only one item")
        }
      }
    }
  }

  @Test
  fun reducePromiseSequence() {
    generatePromiseSequence(Promise.ofSuccess<Int, Throwable>(0)) {
      if (it < 4) Promise.ofSuccess(it + 1) else null
    }.then {
      it.reduce(0) { acc, value -> acc + value }
    }.checkPromiseSuccessResult {
      Truth.assertThat(it).isEqualTo(10)
    }
  }

  private fun <T, E> Promise<T, E>.checkPromiseSuccessResult(assert: (T) -> Unit) {
    checkPromiseResult {
      when (it) {
        is Promise.Result.Success -> assert(it.value)
        is Promise.Result.Error -> Assert.fail("Expected resolve")
      }
    }
  }

  private fun <T, E> Promise<T, E>.checkPromiseErrorResult(assert: (E) -> Unit) {
    checkPromiseResult {
      when (it) {
        is Promise.Result.Success -> Assert.fail("Expected reject")
        is Promise.Result.Error -> assert(it.error)
      }
    }
  }

  private fun <T, E> Promise<T, E>.checkPromiseResult(assert: (Promise.Result<T, E>) -> Unit) {
    var result: Promise.Result<T, E>? = null
    whenComplete {
      result = it
    }
    result?.run { assert(this) } ?: Assert.fail("Expected result")
  }
}