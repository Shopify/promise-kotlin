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
@file:JvmName("PromiseCombine")

package com.shopify.promises

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Create [Promise]`Array<T>, E` that will wait until all provided promises are successfully resolved or one of them fails
 *
 * If one of provided promises failed remaining promises will be terminated and this [Promise] will fail too with the same error.
 * Execution results are kept in order.
 *
 * @param promises sequence of [Promise]`<T, E>` to be executed
 * @return [Promise]`<Array<T>, E>`
 */
inline fun <reified T, E> Promise.Companion.all(promises: List<Promise<T, E>>): Promise<Array<T>, E> {
  return Promise {
    if (promises.isEmpty()) {
      resolve(emptyArray<T>())
      return@Promise
    }


    val subscriber = this

    val remainingCount = AtomicInteger(promises.size)
    val canceled = AtomicBoolean()
    val cancel = {
      canceled.set(true)
      promises.forEach { it.cancel() }
    }
    onCancel(cancel)

    val result = Array<Any>(promises.size) { Unit }
    promises.forEachIndexed { index, promise ->
      if (canceled.get()) return@forEachIndexed
      promise.whenComplete(
        onResolve = {
          result[index] = it as Any
          if (remainingCount.decrementAndGet() == 0 && !canceled.get()) {
            subscriber.resolve(Array(result.size) { result[it] as T })
          }
        },
        onReject = {
          cancel()
          subscriber.reject(it)
        }
      )
    }
  }
}

/**
 * Create [Promise]`Array<T>, E` that will wait until all provided promises are successfully resolved or one of them fails
 *
 * If one of provided promises failed remaining promises will be terminated and this [Promise] will fail too with the same error.
 * Execution results are kept in order.
 *
 * @param promises array of [Promise]`<T, E>` to be executed
 * @return [Promise]`<Array<T>, E>`
 */
inline fun <reified T, E> Promise.Companion.all(vararg promises: Promise<T, E>): Promise<Array<T>, E> {
  return all(promises.asList())
}

/**
 * Create [Promise]`<Tuple<T1, T2>, E>` that will wait until all provided promises are success resolved or one of them fails
 *
 * If one of provided promises failed remaining promises will be terminated and this [Promise] will fail too with the same error.
 *
 * @param p1 [Promise]`<T1, E>` to be executed
 * @param p2 [Promise]`<T2, E>` to be executed
 * @return [Promise]`<Tuple<T1, T2>, E>`
 *
 * @see Tuple
 */
@Suppress("UNCHECKED_CAST", "NAME_SHADOWING")
fun <T1 : Any?, T2 : Any?, E> Promise.Companion.all(p1: Promise<out T1, out E>, p2: Promise<out T2, out E>): Promise<Tuple<T1, T2>, out E> {
  val p1 = p1.map { it as Any? }
  val p2 = p2.map { it as Any? }
  return all(p1, p2).map {
    Tuple(it[0] as T1, it[1] as T2)
  }
}

/**
 * Create [Promise]`<Tuple3<T1, T2, T3>, E>` that will wait until all provided promises are success resolved or one of them fails
 *
 * If one of provided promises failed remaining promises will be terminated and this [Promise] will fail too with the same error.
 *
 * @param p1 [Promise]`<T1, E>` to be executed
 * @param p2 [Promise]`<T2, E>` to be executed
 * @param p3 [Promise]`<T3, E>` to be executed
 * @return [Promise]`<Tuple3<T1, T2, T3>, E>`
 *
 * @see Tuple3
 */
@Suppress("UNCHECKED_CAST", "NAME_SHADOWING")
fun <T1 : Any?, T2 : Any?, T3 : Any?, E> Promise.Companion.all(p1: Promise<T1, E>, p2: Promise<T2, E>, p3: Promise<T3, E>): Promise<Tuple3<T1, T2, T3>, E> {
  val p1 = p1.map { it as Any? }
  val p2 = p2.map { it as Any? }
  val p3 = p3.map { it as Any? }
  return all(p1, p2, p3).map {
    Tuple3(it[0] as T1, it[1] as T2, it[2] as T3)
  }
}

/**
 * Create [Promise]`<Tuple4<T1, T2, T3, T4>, E>` that will wait until all provided promises are success resolved or one of them fails
 *
 * If one of provided promises failed remaining promises will be terminated and this [Promise] will fail too with the same error.
 *
 * @param p1 [Promise]`<T1, E>` to be executed
 * @param p2 [Promise]`<T2, E>` to be executed
 * @param p3 [Promise]`<T3, E>` to be executed
 * @param p4 [Promise]`<T4, E>` to be executed
 * @return [Promise]`<Tuple4<T1, T2, T3, T4>, E>`
 *
 * @see Tuple4
 */
@Suppress("UNCHECKED_CAST", "NAME_SHADOWING")
fun <T1 : Any?, T2 : Any?, T3 : Any?, T4 : Any?, E> Promise.Companion.all(p1: Promise<T1, E>, p2: Promise<T2, E>, p3: Promise<T3, E>, p4: Promise<T4, E>): Promise<Tuple4<T1, T2, T3, T4>, E> {
  val p1 = p1.map { it as Any? }
  val p2 = p2.map { it as Any? }
  val p3 = p3.map { it as Any? }
  val p4 = p4.map { it as Any? }
  return all(p1, p2, p3, p4).map {
    Tuple4(it[0] as T1, it[1] as T2, it[2] as T3, it[3] as T4)
  }
}

/**
 * Create [Promise] that will be resolved as soon as the first of the provided promises resolved
 *
 * If resolved [Promise] resolved with error this one will fail with the same error.
 *
 * @param promises sequence of [Promise]`<T, E>` to be resolved
 * @return [Promise]`<T, E>`
 */
inline fun <reified T, E> Promise.Companion.any(promises: List<Promise<T, E>>): Promise<T, E> {
  return Promise {
    val subscriber = this
    val promiseList = promises.toList()
    val canceled = AtomicBoolean()
    val cancel = {
      canceled.set(true)
      promiseList.forEach { it.cancel() }
    }
    onCancel(cancel)

    promiseList.forEach {
      if (canceled.get()) return@forEach
      it.whenComplete {
        cancel()
        when (it) {
          is Promise.Result.Success -> subscriber.resolve(it.value)
          is Promise.Result.Error -> subscriber.reject(it.error)
        }
      }
    }
  }
}

/**
 * Create [Promise] that will be resolved as soon as the first of the provided promises resolved
 *
 * If resolved [Promise] resolved with error this one will fail with the same error.
 *
 * @param promises vararg of [Promise]`<T, E>` to be resolved
 * @return [Promise]`<T, E>`
 */
inline fun <reified T, E> Promise.Companion.any(vararg promises: Promise<T, E>): Promise<T, E> {
  return any(promises.asList())
}