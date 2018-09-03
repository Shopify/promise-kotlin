package com.shopify.promises

import com.google.common.truth.Truth
import org.junit.Assert
import org.junit.Test

class SequenceTestCase {

  @Test
  fun reducePromiseSequenceWithSeedPromise() {
    Promise.sequence<Int, Throwable>(0) {
      if (it < 3) {
        Promise.ofSuccess(it + 1)
      } else {
        null
      }
    }
      .reduce()
      .whenComplete(
        {
          Truth.assertThat(it).hasSize(3)
          Truth.assertThat(it).isEqualTo(listOf(1, 2, 3))
        },
        { Assert.fail("Expected resolve") }
      )
  }

  @Test
  fun reducePromiseSequenceWithSeedWhenError() {
    Promise.sequence<Int, Throwable>(0) {
      if (it < 3) {
        Promise.ofError(RuntimeException())
      } else {
        null
      }
    }
      .reduce()
      .whenComplete(
        { Assert.fail("Expected reject") },
        {}
      )
  }

  @Test
  fun reducePromiseSequenceWithoutSeed() {
    var index = 0
    Promise.sequence<Int, Throwable> {
      if (index++ < 3) {
        Promise.ofSuccess(index)
      } else {
        null
      }
    }
      .reduce()
      .whenComplete(
        {
          Truth.assertThat(it).hasSize(3)
          Truth.assertThat(it).isEqualTo(listOf(1, 2, 3))
        },
        { Assert.fail("Expected resolve") }
      )
  }

  @Test
  fun reducePromiseSequenceWithoutSeedWhenError() {
    var index = 0
    Promise.sequence<Int, Throwable> {
      if (index++ < 3) {
        Promise.ofError(RuntimeException())
      } else {
        null
      }
    }
      .reduce()
      .whenComplete(
        { Assert.fail("Expected reject") },
        {}
      )
  }
}