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
@file:JvmName("PromiseSideEffect")

package com.shopify.promises

/**
 * Register action to be performed before executing this [Promise]
 *
 * @param block to be performed `() -> Unit`
 * @return [Promise]`<T, E>`
 */
inline fun <T, E> Promise<T, E>.onStart(crossinline block: () -> Unit): Promise<T, E> {
  return Promise<Unit, Nothing> {
    block()
    resolve(Unit)
  }
    .promoteError<Unit, E>()
    .then { this }
}

/**
 * Specify action to be performed when this [Promise] succeed
 *
 * **NOTE: this call does not start promise task execution.**
 *
 * @param block to be performed
 * @return [Promise]`<T, E>`
 */
inline fun <T, E> Promise<T, E>.onResolve(crossinline onResolve: PromiseOnResolveCallback<T>): Promise<T, E> {
  return then {
    onResolve(it)
    Promise.ofSuccess<T, E>(it)
  }
}

/**
 * Specify action to be performed when this [Promise] failed
 *
 * **NOTE: this call does not start promise task execution.**
 *
 * @param block to be performed
 * @return [Promise]`<T, E>`
 */
inline fun <T, E> Promise<T, E>.onReject(crossinline onReject: PromiseOnRejectCallback<E>): Promise<T, E> {
  return errorThen {
    onReject(it)
    Promise.ofError<T, E>(it)
  }
}