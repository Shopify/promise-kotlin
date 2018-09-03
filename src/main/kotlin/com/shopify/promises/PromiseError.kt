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
@file:JvmName("PromiseErrorUtils")

package com.shopify.promises

/**
 * Ignore error result of this [Promise]
 *
 * **NOTE: in case if this promise failed, new promise will be never resolved.**
 *
 * @return [Promise]`<T, Nothing>`
 */
fun <T, E> Promise<T, E>.ignoreError(): Promise<T, Nothing> {
  return this.errorThen {
    Promise.never<T, Nothing>()
  }
}

/**
 * Convenience method to transform this [Promise]`T, Nothing` that can be never resolved as error to a new one with typed error [Promise]`<T, E>`
 *
 * @return [Promise]`<T, E>`
 */
fun <T, E> Promise<T, Nothing>.promoteError(): Promise<T, E> {
  return mapError {
    throw IllegalStateException("should never happen")
  }
}

/**
 * Create promise that will be never executed
 *
 * @return [Promise]`<T, E>`
 */
fun <T, E> Promise.Companion.never(): Promise<T, E> = Promise<T, E> {}



