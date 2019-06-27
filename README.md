[![GitHub license](https://img.shields.io/badge/license-MIT-lightgrey.svg?maxAge=2592000)](https://raw.githubusercontent.com/Shopify/promise-kotlin/master/LICENSE)
[![Build Status](https://travis-ci.org/Shopify/promise-kotlin.svg?branch=master)](https://travis-ci.org/Shopify/promise-kotlin)
[![GitHub release](https://img.shields.io/github/release/Shopify/promise-kotlin.svg)](https://github.com/Shopify/promise-kotlin/releases)


# Promise.kt

`Promise` as a pattern is well known in JS world, but it's not so popular among Android folks, maybe because of the fact that we have very powerful `RxJava` library. 
But what if you need `RxJava` just for a single value response (`Single`) such as single network request and couple transformation operation like: `flatMap` and `map`. If this is the case then you should consider `Promise` pattern that works well for single value response.

### What is Promise ?

[Wiki](https://en.wikipedia.org/wiki/Futures_and_promises) defines Promises pattern as: 

> Promise refer to constructs used for synchronizing program execution in some concurrent programming languages. They describe an object that acts as a proxy for a result that is initially unknown, usually because the computation of its value is yet incomplete.

Technically, it's a wrapper for an async function with result returned via callback. Besides being an async computation wrapper, `Promise` can also act as [continuation monad](https://en.wikipedia.org/wiki/Monad_(functional_programming)#Continuation_monad) opening up a new way of solving many problems related to async computations.

### Features 
This implementation of `Promise` provides next key-features:

- cold by default, what means until you explicitly subscribe to it via `whenComplete` task won't be executed
- cached, once value computed subsequent calls to `whenComplete` will not trigger async computation again.
- parameterized with both value and error: `Promise<Int, Error>`
- implemented as a continuation monad, defines `bind` operator that provides extension point for new custom operators
- cancelable, `Promise` can be canceled with triggering action if provided in task declaration via `doOnCancel`  
- very light weight implementation that offers only core functionality but at the same time flexible enough to extend
- synchronization lock free, uses CAS/atomic operations only

## Quick Start

As a quick start let's imagine the next scenario, we want to fetch user by id first and then fetch user's repositories. Below example that demonstrates how it can be solved with `Promise`:

```kotlin
 Promise.ofSuccess<Long, IOException>(100)
  .then { userId ->
    Promise<User, IOException> {
      val fetchUser: Call = userService.fetchUserById(userId)
      onCancel {
        // when Promise.cancel() is called this action will be performed
        // to give you a chance cancel task execution and clean up resources
        fetchUser.cancel()
      }

      fetchUser.enqueue(object : Callback {
        override fun onFailure(e: IOException) {
          // notify promise about error
          reject(e)
        }

        override fun onResponse(response: User) {
          // notify promise about success
          resolve(response)
        }
      })
    }
  }.then { user ->
    // chain another promise that will be executed when previous one
    // resolved with success
    Promise<List<Repo>, IOException> {
      val fetchUserRepositories: Call = repoService.fetchUserRepositories(user.id)
      onCancel {
        // when Promise.cancel() is called this action will be performed
        // to give you a chance cancel task execution and clean up resources
        // any chained promises will get a chance to cancel their task
        // if they alread started execution
        fetchUserRepositories.cancel()
      }

      fetchUserRepositories.enqueue(object : Callback {
        override fun onFailure(e: IOException) {
          // notify promise about error
          reject(e)
        }

        override fun onResponse(response: List<Repo>) {
          // notify promise about success
          resolve(response)
        }
      })
    }.map { 
      // chain implicitly new promise with transformation function
      repositories -> user to repositories 
    }
}.whenComplete {
  // subscribe to pipeline of chained promises
  // this call will kick-off promise execution
  when (it) {
    is Promise.Result.Success -> println("User: ${it.value.first} repositories: ${it.value.second}")
    is Promise.Result.Error -> println("Yay, error: ${it.error.message}")
  }
}
```

## Usage 
`Promise` is super easy to use, you should treat them as a task that suppose to be executed sometime in the future but it won't be started unless you subscribe and it promises either be successfully resolved or failed.


### Create promise
There are 2 ways to create a `Promise`, provide a task that will do some job:

```kotlin
Promise<User, RuntimeException> {
  val fetchUserRequest = ...;
  onCancel {
    // will be called if promise has been canceled
    // it is task responsibility to terminate any internal long running jobs
    // and clean up resources here
    fetchUserRequest.cancel()
  }
  
  // do some real job
  val user = fetchUserRequest.execute();
  
  // notify about success
  resolve(user) 
  
  //or report error
  //reject(RuntimeException("Failed"))
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
  onCancel {
    call.cancel()
  }

  val result = try {
    call.execute() // long running job
  } catch (e: Exception) {
    reject(RuntimeException(e))
    return@Promise
  }
  resolve(result)
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
Promise.ofSuccess<Long, IOException>(100)
  .then { userId ->
    Promise<User, IOException> {
      val fetchUser: Call = userService.fetchUserById(userId)
      onCancel {
        // when Promise.cancel() is called this action will be performed
        // to give you a chance cancel task execution and clean up resources
        fetchUser.cancel()
      }

      fetchUser.enqueue(object : Callback {
        override fun onFailure(e: IOException) {
          // notify promise about error
          reject(e)
        }

        override fun onResponse(response: User) {
          // notify promise about success
          resolve(response)
        }
      })
    }
  }.then { user ->
    // chain another promise that will be executed when previous one
    // resolved with success
    Promise<List<Repo>, IOException> {
      val fetchUserRepositories: Call = repoService.fetchUserRepositories(user.id)
      onCancel {
        // when Promise.cancel() is called this action will be performed
        // to give you a chance cancel task execution and clean up resources
        // any chained promises will get a chance to cancel their task
        // if they alread started execution
        fetchUserRepositories.cancel()
      }

      fetchUserRepositories.enqueue(object : Callback {
        override fun onFailure(e: IOException) {
          // notify promise about error
          reject(e)
        }

        override fun onResponse(response: List<Repo>) {
          // notify promise about success
          resolve(response)
        }
      })
    }.map { 
      // chain implicitly new promise with transformation function
      repositories -> user to repositories 
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
      onCancel {
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

### Groups
There are circunstances where you would like to handle a *cluster* of promises. `Promise.all()` receive as argument a collection of promises, it will return on success an array of results and on failure will return the first rejected value (indicated on that promise).

Let's imagine we have a list of users ids and we want to know the latest activity for each user.

```
val ids = mutableListOf(1, 2, 3, 4)
val promises : mutableListOf(Promises<List<UserActivity>, IOException>)
ids.map{
	val promise = Promise<List<UserActivity>, IOException> {
			//Remember to handle cancel
			try {
				//it is our user id
				val lastActivity = fetchLatestActivity(it).execute()
				resolve(lastActivity)
			} catch (e : IOException) {
				reject(e)
			}
	}
	promises.add(promise)
}
//After ids iteration we have a list of promises, we have to susbcribe now
Promise.all(promises).whenComplete {
	//You can use reserved word it, this is for clarification
	result: Promise.Result<Array<List<UserActivity>>, Int>
	when (result) {
		is Promise.Result.Success<Array<List<UserActivity>>, Int> -> {
			//Every promise success value was a list, we have an array of lists
			val arrayOfLists : <Array<List<UserActivity>> = result.value
			//You now can iterate, save it to the database
		}
		is Promise.Result.Error<Array<List<UserActivity>>, Int> -> {
			//This is the error of the first promise to fail
			Log.d("SHOPIFY_PROMISE", "request fail", result.error)
		}
	}
}

```


### Gradle Integration

```
implementation 'com.shopify.promises:promises:0.0.8'
```

You can see the [lastest version in Bintray](https://bintray.com/shopify/shopify-android/Promises) 

### Other API
Additionally this micro-framework provides next util functions:

- `Promise#cancel()` signals promise to cancel task execution if it hasn't been completed yet
- `Promise#onResolve(block: (T) -> Unit)`, `Promise#onReject(block: (E) -> Unit)` like in Rx it allows to register some action to be performed when this promise resolved with success or failure
- `Promise#onStart(block: () -> Unit)` allows to register some action to be performed before promise task execution, useful when some initialization is required
- `Promise.Companion#all(promises: Sequence<Promise>)` creates a `Promise` that will wait until all provided promises are successfully resolved or one of them fails
- `Promise.Companion#any(promises: Sequence<Promise>)` creates a `Promise` that will be resolved as soon as the first of the provided promises resolved or failed

## Contributions

We welcome contributions. Please follow the steps in our [contributing guidelines](CONTRIBUTING.md).

## License

`Promise-Kotlin` is distributed under MIT license [MIT License](LICENSE).
