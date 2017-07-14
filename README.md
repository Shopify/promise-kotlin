# Promises.kt

Promises as a pattern is well known in JS world, but it's not so popular among Android folks, maybe because of fact that we have very powerful RxJava library. But what if you use RxJava just for a single value response (`Single`) such as single network request you probably don't need all the features offered by Rx except two: `flatMap` and `map`. If this is the case then you should consider promises pattern that works well for single value response.

[Wiki](https://en.wikipedia.org/wiki/Futures_and_promises) defines Promises pattern as: 

> Promise refer to constructs used for synchronizing program execution in some concurrent programming languages. They describe an object that acts as a proxy for a result that is initially unknown, usually because the computation of its value is yet incomplete.

### Features 
This implementation of `Promise` provides next key-features:

- promises are "cold" by default, that means you must explicitly subscribe in order to start task execution
- implemented in monadic way: it's super easy to extend and introduce own custom behaviour with `bind` operation
- micro framework: very light weight implementation that offers only core functionality but at the same time flexible enough to extend
- implementation synchronization lock free: uses CAS/atomic operations internally
- built on Kotlin

## Quick Start
Let's imagine that we want to fetch user by id with repositories, below example demonstrates how it can be solved with promises:

```kotlin
 Promise.ofSuccess<Long, IOException>(100)
  .then { userId ->
    Promise<User, IOException> {
      val fetchUser: Call = userService.fetchUserById(userId)
      doOnCancel {
        fetchUser.cancel()
      }

      fetchUser.enqueue(object : Callback {
        override fun onFailure(e: IOException) {
          onError(e)
        }

        override fun onResponse(response: User) {
          onSuccess(response)
        }
      })
    }
  }.then { user ->
    Promise<List<Repo>, IOException> {
      val fetchUserRepositories: Call = repoService.fetchUserRepositories(user.id)
      doOnCancel {
        fetchUserRepositories.cancel()
      }

      fetchUserRepositories.enqueue(object : Callback {
        override fun onFailure(e: IOException) {
          onError(e)
        }

        override fun onResponse(response: List<Repo>) {
          onSuccess(response)
        }
      })
    }.map { repositories -> user to repositories }
}.whenComplete {
  when (it) {
    is Promise.Result.Success -> println("User: ${it.value.first} repositories: ${it.value.second}")
    is Promise.Result.Error -> println("Yay, error: ${it.error.message}")
  }
}
```


## Usage 
`Promise` is super easy to use, you should treat them as a task that suppose to be executed sometime in the future and it promises either be resolved successfully or fail.


### Create promise
There are 2 ways to create a `Promise`, provide a task that will do some job:

```kotlin
Promise<User, RuntimeException> {
  val fetchUserRequest = ...;
  doOnCancel {
    // will be called if promise has been canceled
    // it is task responsibility to terminate any internal long running jobs
    // and clean up resources here
    fetchUserRequest.cancel()
  }
  
  // do some real job
  val user = fetchUserRequest.execute();
  
  // notify about success
  onSuccess(user) 
  
  //or report error
  //onError(RuntimeException("Failed"))
}
```

we just created a `Promise` with success result type `User` and error result type `RuntimeException`. This promise will perform some task to fetch user in some future and notify about result via `onSuccess(user)` or if task failed `onError(RuntimeException("Failed"))`. Additionally task can register an action to terminate long running job, clean up resources, etc. and it will be called when this `Promise` is canceled. Keep in mind that this is a cold `Promise` and it won't start executing task until we explicitly subscribe to it.

The second way to create a `Promise` is wrapping existing result into promise:

```kotlin
val promise1 = Promise.of("Boom!")
val promise2 = Promise.ofSuccess<String, RuntimeException>("Kaboom!")
val promise3 = Promise.ofError<String, IOException>(IOException("Smth wrong today"))
```

`promise1` will be resolved with success result value `Boom1`, `promise2` will be resolved with success result value `Kaboom!`. The difference between these two promises is in types: `promise1` has type `Promise<String, Nothing>` that means this promise doesn't know what type of error the result pipeline will be, while `promise2` declare that any next chained promise should propagate `RuntimeException`. 
The last `promise3` resolved with error result `IOException("Smth wrong today")`.

### Start promise
To start promise task execution you have to subscribe to it:

```kotlin
Promise<String, RuntimeException> {
  val call = null
  doOnCancel {
    call.cancel()
  }

  val result = try {
    call.execute() // long running job
  } catch (e: Exception) {
    onError(RuntimeException(e))
    return@Promise
  }
  onSuccess(result)
}.whenComplete { result: 
  when (it) {
    is Promise.Result.Success -> println("We did it: ${it.value}")
    is Promise.Result.Error -> println("Yay, error: ${it.error.message}")
  }
}
```

You can call as many `whenComplete` on the same promise as you want, all will be notified about the promise result but task will be executed only once.

### Chain promises

Now the best part, you can build a chain or pipeline of promises by using: `then`, `map`, `errorThen`, `mapError`:

```kotlin
Promise<String, RuntimeException> {
  ...
}.map {
  it.toLong()
}.then {
  Promise<User, IOException> {
    val fetchUserByIdCall = null
    doOnCancel {
      fetchUserByIdCall.cancel()
    }

    val user = try {
      fetchUserByIdCall.execute() // long running job
    } catch (e: IOException) {
      onError(e)
      return@Promise
    }

    onSuccess(user)
  }.mapError { 
    RuntimeException(it)
  }
}.whenComplete { result: Promise.Result<User, RuntimeException> ->
  when (result) {
    is Promise.Result.Success -> println("User : ${result.value}")
    is Promise.Result.Error -> println("Yay, error: ${result.error.message}")
  }
}
```

### Scheduling
Even though it is up to promise task to decide what thread to use for job execution and notify about result, but sometimes as a consumer you need a way to specify explicitly the thread on which you want receive the result:

```kotlin
val mainThreadExecutor: Executor = ...;
Promise<User, IOException> {
   //do some job
}
  .completeOn(mainThreadExecutor)
  .whenComplete { result: Promise.Result<User, RuntimeException> ->
    when (result) {
      is Promise.Result.Success -> println("User : ${result.value}")
      is Promise.Result.Error -> println("Yay, error: ${result.error.message}")
    }
  }
```
Now the result will be delivered on `mainThreadExecutor`. 
You probably will ask what if I need to be notified on main Android thread, you can easily extend existing functionality by:

```kotlin
fun <T, E> Promise<T, E>.completeOn(handler: Handler): Promise<T, E> {
  return bind { result ->
    Promise<T, E> {
      val canceled = AtomicBoolean()
      doOnCancel {
        canceled.set(true)
      }
      handler.post {
        if (!canceled.get()) {
          when (result) {
            is Promise.Result.Success -> onSuccess(result.value)
            is Promise.Result.Error -> onError(result.error)
          }
        }
      }
    }
  }
}
```
This a good example of using `bind` operation to extend and bring your custom behaviour for this micro-framework.

You can even specify on what thread `Promise` job should be started:

```kotlin
Promise<User, IOException> {
   //do some job
}
  .startOn(Executors.newSingleThreadExecutor())
  .whenComplete { result: Promise.Result<User, RuntimeException> ->
    when (result) {
      is Promise.Result.Success -> println("User : ${result.value}")
      is Promise.Result.Error -> println("Yay, error: ${result.error.message}")
    }
  }
```
Now the job inside promise will be started with the executor we just provided.

### Other API
Additionally this micro-framework provides next util functions:

- `Promise#onSuccess(action: PromiseSuccessAction)`, `Promise#onSuccess(action: PromiseSuccessAction)` like in Rx it allows to register some action to be performed when this promise resolved with success or failure
- `Promise#onStart(block: () -> Unit)` allows to register some action to be performed before promise task execution, useful when some initialization is required
- `Promise.Companion#all(promises: Sequence<Promise>)` creates a `Promise` that will wait until all provided promises are successfully resolved or one of them fails
- `Promise.Companion#any(promises: Sequence<Promise>)` creates a `Promise` that will be resolved as soon as the first of the provided promises resolved or failed
