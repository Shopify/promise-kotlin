@file:JvmName("Tuples")

package com.shopify.promises

data class Tuple<out T1, out T2>(val value1: T1, val value2: T2)
data class Tuple3<out T1, out T2, out T3>(val value1: T1, val value2: T2, val value3: T3)
data class Tuple4<out T1, out T2, out T3, out T4>(val value1: T1, val value2: T2, val value3: T3, val value4: T4)