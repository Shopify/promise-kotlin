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
@file:JvmName("PromiseMonadic")

package com.shopify.promises

/**
 * Bind success resolved value of this [Promise]`<T, E>` and return a new [Promise]`<T1, E>` with transformed value
 *
 * @param transform transformation function `(T) -> T1`
 * @return [Promise]`<T1, E>`
 */
inline fun <T, E, T1> Promise<T, out E>.map(crossinline transform: (T) -> T1): Promise<T1, E> {
  return bind {
    when (it) {
      is Promise.Result.Error -> Promise.ofError<T1, E>(it.error)
      is Promise.Result.Success -> Promise.ofSuccess<T1, E>(transform(it.value))
    }
  }
}

/**
 * Bind success resolved value of this [Promise]`<T, E>` and return a new [Promise]`<T1, E>` with transformed value
 *
 * @param transform transformation function `(T) -> Promise<T1, E>`
 * @return [Promise]`<T1, E>`
 */
inline fun <T, E, T1> Promise<T, out E>.then(crossinline transform: (T) -> Promise<T1, E>): Promise<T1, E> {
  return bind {
    when (it) {
      is Promise.Result.Error -> Promise.ofError<T1, E>(it.error)
      is Promise.Result.Success -> transform(it.value)
    }
  }
}

/**
 * Bind error resolved value of this [Promise]`<T, E>` and return a new [Promise]`<T, E1>` with transformed value
 *
 * @param transform transformation function `(E) -> E1`
 * @return [Promise]`<T, E1>`
 */
inline fun <T, E, E1> Promise<out T, E>.mapError(crossinline transform: (E) -> E1): Promise<T, E1> {
  return bind {
    when (it) {
      is Promise.Result.Success -> Promise.ofSuccess<T, E1>(it.value)
      is Promise.Result.Error -> Promise.ofError<T, E1>(transform(it.error))
    }
  }
}

/**
 * Bind error resolved value of this [Promise]`<T, E>` and return a new [Promise]`<T, E1>` with transformed value
 *
 * @param transform transformation function `(E) -> Promise<T, E1>`
 * @return [Promise]`<T, E1>`
 */
inline fun <T, E, E1> Promise<out T, E>.errorThen(crossinline transform: (E) -> Promise<T, E1>): Promise<T, E1> {
  return bind {
    when (it) {
      is Promise.Result.Success -> Promise.ofSuccess<T, E1>(it.value)
      is Promise.Result.Error -> transform(it.error)
    }
  }
}