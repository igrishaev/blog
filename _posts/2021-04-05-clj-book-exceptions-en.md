---
layout: post
title:  "Exceptions in Clojure"
permalink: /en/clj-book-exceptions/
tags: clojure book programming exceptions
lang: en
---

{% include toc-clj-book-en.md %}

{% include toc.html id="clj-book-exceptions-en" title="In This Chapter" %}

# Exceptions

*This chapter considers exceptions in Clojure. How do they work and how do they differ from their Java counterparts? When is it better to throw and when to catch exceptions? What and how to write in logs to investigate an incident quickly?*

Somebody might find it strange to devote an entire chapter to exceptions. The topic, after all, is simple: exceptions can only be thrown, caught, and logged. Theoretically, this is enough to work on a project.

Exceptions are technically simple, but they have rich *semantics*. When exactly should you throw and catch exceptions? What useful information do they carry? Where to write exceptions? Can we catch them with predicates? In practice, we are overwhelmed with countless specific cases.

Newbies tend to follow the positive path. They write code such as there basically cannot be exceptions in their work. This is why it is so hard to troubleshoot errors afterwards. Why did the server respond with code 500? There are innumerable possible reasons why the request failed. However, the log entry provides too little information to understand what has happened.

A good programmer pays close attention to errors. With experience, it becomes clear -- refusing exceptions does not pay off. Without them, we will complete the task faster, and there will be less code -- that is right. But later, you will have problems with detailing and fixing said errors.

Exceptions in code are just as important as normal behavior. If you think this kind of problem will not happen to you, think again. If your project encounters trouble due to uncaught errors, then it is time to study the topic.

<!-- more -->

## Basics of Exceptions

Before going into detail, let us remember what exceptions are and how they behave.

An exception is an object, most often an instance of the `Exception` class. It differs from other classes in that we can throw it. In different languages, there are operators which serve this purpose. Among those are `throw`, `raise`, and others.

The thrown object interrupts execution and floats up the call stack. There are two possible outcomes: either the `catch` statement catches it at one of the levels or not -- knowingly or by mistake.

In the first case, we will get an exception object. We access it as usual: get fields, call methods, pass to functions. Further behavior depends on program logic. Sometimes an exception is logged, and the program is terminated; otherwise, the program continues running.

If we do not catch the exception, the program will exit with nonzero code. Unless otherwise specified, the program will write the exception to `stderr` (the standard error channel) before exiting. We will see its class, text, and stack trace there. The last is the chain of calls that an exception passed through from the moment it was thrown to caught.

Some platforms allow you to specify a reaction to an uncaught exception. For example, to write it to a file or terminate the program in a special way.

Clojure is a hosted language: it relies on the capabilities that the host offers. Exceptions are an area where Clojure leverages Java best practices. By default, Clojure uses the `try` and `catch` forms similar to Java's.

Let's take a look at Java's exceptions. The platform contains the `Throwable` base class, the ancestor of all exceptions. Other classes inherit from it and extend its semantics. The `Error` and `Exception` classes are first-level heirs. The `RuntimeException` class inherits from from the `Exception` one and so on.

{:.code_chart}
~~~
                ┌─────────────┐
                │   Object    │
                └─────────────┘
                       │
                       ▼
                ┌─────────────┐
             ┌──│  Throwable  │──┐
             │  └─────────────┘  │
             │                   │
             ▼                   ▼
      ┌─────────────┐     ┌─────────────┐
      │    Error    │     │  Exception  │──┐
      └─────────────┘     └─────────────┘  │
                                           │
                                           ▼
                                ┌─────────────────────┐
                                │  RuntimeException   │
                                └─────────────────────┘

~~~

Java packages contain additional exceptions inherited from those described above. For example, `java.io.IOException` is for I/O errors, `java.net.ConnectException` -- for network problems. Throwing `Throwable` is considered bad manner because this class carries too little information about what happened.

In the exception tree, each class complements the semantics of the ancestor. Consider the `FileNotFoundException` exception. It arises when a file is not on disk. The class pedigree looks like this:

~~~
java.lang.Object.
  java.lang.Throwable.
    java.lang.Exception.
      java.io.IOException.
        java.io.FileNotFoundException.
~~~

The diagram is read like this "object &rarr; throwable &rarr; exception &rarr; I/O error &rarr; file not found". It's easy to guess by the name `FileNotFoundException` what the problem is. If a developer threw a `Throwable`, it would make it harder to find the cause of the error.

There are checked and unchecked exceptions in Java. They differ in semantics. The developer must anticipate checked exceptions and handle them in code. When we read a file, it's quite normal that the file is missing. Thus, the `FileNotFoundException` class is a checked exception.

However, it is difficult to predict a memory shortage, so `OutOfMemoryError` is an unchecked exception. Like any other resource, memory is limited, and our careless actions can exhaust it. There is no point in catching this exception since the system is unstable when there is insufficient memory.

The classes inherited from `Error` and `RuntimeException` are unchecked exceptions. Those inherited from `Exception` are checked exceptions.

To throw an exception, we pass its instance to the `throw` statement. The `catch` statement catches exceptions. In Java and other languages, it relies on a class hierarchy. If the type we are looking for is `IOException`, we will catch all exceptions inherited from this class.

The higher the class in the inheritance tree, the more exceptions the `catch` statement will cover. In Java, it is bad manner to catch errors with the `Throwable` or `Exception` classes. Modern IDEs generate the warning "too broad catch expression" or similar. It is better to replace the `Exception` class with some more precise ones: for example, I/O errors, network errors, and others separately.

One class is not enough to understand an exception cause.
`FileNotFoundException` does not have the `file` field to track down which particular file does not exist. Most exceptions take an error message string. The message must be understandable to a person.
If we see the message "File C:/work/test.txt not found", it becomes clear which file we were trying to access.

Sometimes a text is not enough to explain the reason for the error. Let us say data validation failed, and we would like to investigate it later. If you write the data to the message, the text will be too large. Additionally, this is not secure: the data may contain personal information or access keys. Such a message must not be logged or shown to a user. Even the file path can reveal valuable information to outsiders.

If you need to store data for investigation, create a new exception class with a separate field for the data that caused the error. The field is populated in the exception constructor. Create your message so that it does not reveal private information.

## Chains and Context

Exceptions are chained. Each instance takes an optional `cause` argument. It stores either `Null` or a link to another exception.

Chains appear when the code catches an exception but does not know how to deal with it. Since the code does not see the full picture at a low level, this is normal. Suppose the method writes data to a file. It has no authority to decide what to do if the file does not exist so an exception will be thrown. Another method that also does not make decisions will catch this exception. You only need to create a new exception with a link to the first one. This is the chain.

Eventually, control will pass to the method that knows what to do. The logic depends on an exception type and business rules. If a file does not exist, the program will create it or search elsewhere. If an HTTP request fails, the method waits for a second and repeats it, and after the third attempt, the program shuts down.

The system should have the last frontier where all exceptions are caught. If the error has reached this level, then the rules below did not catch it. This indicates abnormal system behavior. A client will receive a text stating that the request has failed. For investigating the cause, the exception is logged and passed to the error collector.

We, programmers, mostly work on expressing the business logic of a company in code. The logic lies at the top level of code, but more technical parts come into play at lower levels. Let us say the `get_user` function finds a user by their number. It's a black box from the outside: we do not know where the data comes from. Suppose the function is communicating with a network. Let us write this in Python:

~~~python
def get_user(id):
  url = "http://api.company.com/user/" + str(id)
  return http.GET(url).body.json()
~~~

If we call a function with a number that does not exist in the system, we will get the `HTTP Error: status 404` exception. This text does not say anything about the user. We will not even understand from this wording which service we tried to reach.

The more we go into technical details, the less we know about the business. In HTTP, there is no such thing as a company user. Only the method, address, and other fields of the request are known. Let us divide the error into two halves. The top half indicates the business reason: User 5 was not found. Why? The GET request to the `http://api.company.com/user/5` address returned status 404, which we consider as a negative one.

Let us place `try/catch` in our code. If the exception came from technical levels, we will add context and send it up. The pattern is called `re-throw`. Here is the new Python code:

~~~python
def get_user(id):
  try:
    url = "http://api.company.com/user/" + str(id)
    return http.GET(url).body.json()
  except Exception as e:
    raise Exception("Cannot fetch user " + str(id)) \
      from e
~~~

The new exception describes a business problem while referring to a technical one. An HTTP error might also have its own causes: an expired certificate or connection problems. A chain of exceptions is formed. There might be up to five or more links in exceptions from real projects.

Descending a chain is like an interrogation. In response to each "why" question, we receive new information until we get to the heart of the matter. This is not to say that the innermost exception is more important than the others: the `HTTPError` exception cannot answer all questions alone. The important thing is how you came to it.

That is how exceptions work in languages like Java, Python, and others. Each platform has its features, but the overall picture stays the same. Now let us take a look at what Clojure has to offer.

## Moving on to Clojure

To get acquainted with the exception, let's provoke it. A surefire trick is to divide a number by zero. Turn on REPL and do `(/ 1 0)`. The following text will appear:

~~~
Execution error (ArithmeticException)...
Divide by zero
~~~

This is the error report. It looks different depending on the editor and settings. Emacs with the CIDER module will open the `*cider-error*` buffer with detailed information.

Note that an exception in the REPL does not stop it: the program still waits for us to type something. The REPL catches errors and only displays them on a screen. In production, Clojure programs work as usual. If no exception is caught in the main thread, the program will stop.

To catch the exception, place the code in the `try` form. It is followed by one or more `catch` forms. They indicate which classes to catch and what to do with an exception. Here's how to safely divide a number:

~~~clojure
(try
  (/ 1 0)
  (catch ArithmeticException e
    (println "Weird arithmetics")))
~~~

The `catch` form takes a class and an arbitrary symbol. An exception will be bound to that symbol if control is taken to this branch. Next goes the arbitrary code. There, the exception is available as the local variable, which is `e` in our case.

We display the text about the failure of calculations without explaining the reason. Let the text be more detailed. The `.getMessage` method will return the message assigned to the exception when it was created. Since version 1.10, Clojure offers the `ex-message` function that does the same:

~~~clojure
(try
  (/ 1 0)
  (catch ArithmeticException e
    (println (ex-message e))))
;; Divide by zero
~~~

Clojure beginners might be surprised that the `ArithmeticException` class fails to catch some computation errors. What will happen if you add 1 to `nil`? Even if we put the calculations in `try/catch`, we won't catch the exception:

~~~clojure
(try
  (+ 1 nil)
  (catch ArithmeticException e
    (println "Weird arithmetics")))
;; Execution error (NullPointerException)...
~~~

That is because the `ArithmeticException` and `NullPointerException` classes do not overlap. They have different paths in the inheritance tree, so catching one does not affect the other. And rightly so, because the classes have different semantics. An arithmetic error is not the same as `Null` instead of a value.

The `try` form takes several `catch` ones. Let's catch both cases:

~~~clojure
(try
  (+ 1 nil)
  (catch ArithmeticException e
    (println "Weird arithmetics"))
  (catch NullPointerException e
    (println "You've got a null value")))
~~~

The macro iterates over the classes from each `catch` and stops at the first one that matches. The `try` will result in the last expression from the `catch` block that matched. In the example above, the result will be `nil` because the `println` function will return it. If no branches match, the exception will continue to go up the call stack.

The higher the exception class in the tree, the more cases it covers. If you replace `ArithmeticException` with `Throwable`, the branch will catch any exception, whether it's division by zero or `NPE`:

~~~clojure
(try
  (/ 1 0)
  (+ 1 nil)
  (catch Throwable e
    (println "I catch everything!")))
~~~

Unlike Java, Clojure doesn't have strict rules about which exceptions to catch. That's up to you. Common sense dictates that intercepting with `Throwable` should not be used: there will be a false impression that the code works without errors.

In the case of `ArithmeticException`, the problem is in the calculations, but for `NPE` -- it is not. `Nil` instead of a number tells us that the problem is in the source, which passed `nil`, and not in arithmetic. That is why catching `NPE` might put you on the wrong track. We'll explore this issue in detail in the chapter on tests.

Usually, we put the `try/catch` form on the top level of the code with a broad scope. That is needed so that the program never stops. That is how web servers, message queues, and networking software work.

Sometimes an exception is thrown deliberately to report an abnormal situation. The `new` operator creates a new Java object. It accepts a class name and its constructor parameters. The `throw` form takes an exception and runs the throwing mechanism.

~~~clojure
(let [e (new Exception "Something is wrong!")]
  (throw e))
~~~

The message above is not informative. But even if we indicated that the problem is in arithmetic or the database, we would like to know which values exactly caused the error. For this, build the message using the `format` function. It takes the template and substitution parameters:

~~~clojure
(defn add [a b]
  (if (and a b)
    (+ a b)
    (let [message
          (format "Value error, a: %s, b: %s" a b)]
      (throw (new Exception message)))))
~~~

Calling `add` with `nil` makes the message clearer:

~~~
Execution error at book.exceptions/add (exceptions.clj:86).
Value error, a: 1, b: null
~~~

The `format` function is useful in that it displays `nil` as `null`. That is its advantage over `str`, which converts `nil` to an empty string. The `str` variant would look like this:

~~~clojure
(str "Value error, a:" 1 ", b: " nil)
;; Value error, a:1, b:
~~~

The text is misleading: is `b` an empty string or `nil`? In the case of `format`, you won't get confused.

## More about Context

The data in a text leads to risk. A message may be too large or disclose unnecessary information. The `ExceptionInfo` class, a Clojure exception, solves this problem. It is designed to carry arbitrary data. Clojure offers some functions for working with it.

The key `ex-info` function creates an instance of `ExceptionInfo`. The former takes a message and a data map. That is the context in which the exception arose. For example, if an HTTP request fails, the map will contain the method, address, and response code.

`Ex-info` only creates an exception but doesn't throw it. The result goes to `throw`:

~~~clojure
(throw (ex-info
        "Cannot fetch user."
        {:user-id 5
         :http-status 404
         :http-method "GET"
         :http-url "https://host.com/users/5"}))
~~~

We have separated the message and the data. The text does not reveal a user number and service address. It will be logged or sent to a user, and the context will be processed separately.

Remember a few rules when working with context. A map must not be `nil`. That is the rare case where Clojure distinguishes `nil` from an empty map. Do not store values that cannot be written to a file, such as a stream or a network connection, in a map. Ideally, the context is transmitted over the network in JSON format. We'll look at what to do with the context later.

The `ex-data` function will return the exception data. If this is a native `ExceptionInfo`, we'll get a map. For other classes, the function will return `nil`.

Let's catch the exception: in the `catch` form, we'll specify the `ExceptionInfo` class. The `ex-data` function retrieves the map that was passed to `ex-info`.
Let's split it into fields and build a message.

~~~clojure
(try
  (get-user 5)
  (catch clojure.lang.ExceptionInfo e
    (let [{:keys [http-method http-url]} (ex-data e)]
      (format "HTTP error: %s %s" http-method http-url))))
;; HTTP error: GET https://host.com/users/5
~~~

## When to Throw Exceptions

It is still unclear when to throw exceptions and when to catch them. Let's look at typical situations and solutions.

There is no point in throwing exceptions when walking through collections. `Nil` behaves like an empty collection of the type that the function accepts.

~~~clojure
(assoc nil :test 5)
(update nil :test (fnil inc 0))
(into nil [1 2 3])
(merge nil {:test 5})
~~~

The expressions above should return lists and maps. The key for `nil` will return `nil`. Splitting a map or vector will set the variables to `nil` if the fields don't match. Both of the `let` directives below will create a vector of three `nil` references.

~~~clojure
(let [{:keys [a b c]} nil]
  [a b c])

(let [[a b c] nil]
  [a b c])
~~~

The "nil punning" term means the language is tolerant to `nil` values and might deal with it without raising exceptions. For example, all the collections functions treat `nil` as an empty collection of a type that is actually meant. Of course, Clojure doesn't make full use of nil punning. In Clojure, `nil` works with collections, but not arithmetic and regular expressions.

When in doubt about a collection, use a spec. The `s/valid?` and `s/conform` functions from the last chapter will help you make sure the collection is correct. That will separate validation from data manipulation. If validation fails — throw an exception. Pass the `explain` data into the context so you can parse it later.

~~~clojure
(require '[clojure.spec.alpha :as s])

(s/def ::data (s/coll-of int?))

(when-let [explain (s/explain-data ::data [1 2 nil])]
  (throw (ex-info "Some item is not an integer"
                  {:explain explain})))
~~~

For the sake of shortness, use the `assert` macro from Spec (don't mix it with the standard `assert` from `clojure.core`). It does the same: validates the data and either returns it or throws an exception with `explain` data. The `check-asserts` global flag defines the macro behavior. If it is off, the effect of `assert` disappears, and there will be no exception.

~~~clojure
(require '[clojure.spec.alpha :as s])

(s/def ::ne-string (s/and string? not-empty))

(s/check-asserts true)
(s/assert ::ne-string "test") ;; ok

(s/assert ::ne-string nil)
;; Execution error - invalid arguments
;; nil - failed: string?

(s/check-asserts false)
(s/assert ::ne-string nil) ;; nil
~~~

Another case of resorting to an exception is resource failure. If the file could not be read, simply reporting this fact is not enough. That might have happened for various reasons: the file does not exist or is occupied by another process; there is insufficient disk space or an encoding error, etc. The error category determines what to do with it and how to prevent it from recurring.

There is no industry consensus on whether to throw an exception on a negative HTTP response. According to the protocol, a 404 response is as correct as 200. Advanced libraries offer the flag to choose whether to throw an exception at 4xx statuses or not.

Let's say our HTTP client doesn't throw an exception. Let's do it manually -- check the status and run `ex-info` with details:

~~~clojure
(defn authenticate-user [user-id]
  (let [url (str "http://auth.company.com/" user-id)
        {:keys [status body]} (client/get url)]
    (if (= status 200)
      body
      (throw (ex-info "Authentication error"
                      {:http-url url
                       :http-status status
                       :http-body body})))))
~~~

Exceptions are helpful in libraries. At their level, we don't make business decisions because we don't see the whole context. Suppose if an image library cannot find a file, it will throw an exception. The code above will catch it and perform a fallback script: maybe it will download the image from the Internet, open another file, or fail. But the library doesn't know for sure what will happen, so raising an error is the only way to report abnormal behaviour.

Now imagine that if the file does not exist, the library will silently skip writing data. Users will be dissatisfied with this logic: how to make sure everything went smoothly? This also applies to the pattern "return `nil` and write to the log", that is, ignore the problem.

## More about Chains

Now let's see how to work with exception chains exactly. The `ex-info` function takes a third optional `cause` parameter. It might be `nil` or another exception that will become part of the new one. Below, the `divide` function catches an arithmetic error and throws a new exception with a full context.

~~~clojure
(defn divide [a b]
  (try
    (/ a b)
    (catch ArithmeticException e
      (throw (ex-info
               "Calculation error"
               {:a a :b b}
               e)))))
~~~

The `ex-cause` function returns a cause of an exception. If there is no cause, we'll get `nil`.

~~~clojure
(try
  (divide 1 0)
  (catch Exception e
    (println (ex-message e))
    (println (ex-message (ex-cause e)))))
~~~

The code will output:

~~~
Calculation error
Divide by zero
~~~

Let's write a function that returns an exception list in descending order of precedence. The original exception comes first, the cause of the exception comes second, then -- the cause of the cause, and so on. Using the `loop` form is the simplest way of doing this:

~~~clojure
(defn ex-chain [e]
  (loop [e e
         result []]
    (if (nil? e)
      result
      (recur (ex-cause e) (conj result e)))))
~~~

To experiment, let's declare the variable `e`. That is a three-link chain of exceptions. At the first level, there is a business logic error: Failed to retrieve a user. At the second level -- a problem with authorization: Not enough permissions for the resource. There is a transport error at the third one: The HTTP request returned status 403.

~~~clojure
(def e
  (ex-info
   "Get user info error"
   {:user-id 5}
   (ex-info "Auth error"
            {:token "........."}
            (ex-info "HTTP error"
                     {:method "POST"
                      :url "http://api.site.com"}))))
~~~

We got an exception tree as a result, but it is not always convenient to work with the tree. Walking through a flat structure is better. The `ex-chain` function we wrote will be useful for us. Here's how to get messages of all exceptions:

~~~clojure
(map ex-message (ex-chain e))
;; ("Get user info error" "Auth error" "HTTP error")
~~~

And print line by line (we'll get a column of the same lines):

~~~clojure
(doseq [e (ex-chain e)]
  (-> e ex-message println))
~~~

Let's express `ex-chain` in short through the `iterate` function. This function applies another one to the initial argument, then to the result, then to the new result and so on. We need the `take-while` constraint to stop before the first `nil` item.

~~~clojure
(defn ex-chain [e]
  (take-while some? (iterate ex-cause e)))
~~~

## Printing of Exceptions

Something went wrong, and we found ourselves in the `catch` thread. A local variable points to the exception. What can we do about it?

The simplest thing is to print the exception to the console. The `println` function is smart enough: it converts the exception to a map before printing. The map is easy to divide into parts, so it is convenient for editors and IDE. For example, to show only that part of the stack trace that concerns project-related namespaces.

The last exception in the chain is called the root. For convenience, `println` duplicates it to the beginning, so that we can see the root cause immediately. This is what `(println e)` will output:

~~~clojure
#error {
 :cause HTTP error
 :data {:method POST, :url http://api.site.com}
 :via
 [{:type clojure.lang.ExceptionInfo
   :message Get user info error
   :data {:user-id 5}}
  {:type clojure.lang.ExceptionInfo
   :message Auth error
   :data {:token .........}}
  {:type clojure.lang.ExceptionInfo
   :message HTTP error
   :data {:method POST, :url http://api.site.com}}]
 :trace
 [[clojure.lang.AFn applyToHelper AFn.java 156]
  [clojure.lang.AFn applyTo AFn.java 144]
  [clojure.lang.Compiler$InvokeExpr eval Compiler.java 3701]
  ..........]}
~~~

The `:trace` vector is called a stack trace. It is a list of Java methods that the exception passed through, from being thrown to being caught. A trace element is a vector with a class, method, and file names as well as a line number.

We have shortened the trace, but usually, they take up several screens. It's important to understand that they signify not Clojure, but Java code made after compilation. When analysing the trace, you should perform a reverse action in your mind: match compiled names with your Clojure code. The trick is not always easy and confuses beginners. Noisy traces are a fair reproach to Clojure. On the other hand, this is a common flaw of most of the JVM languages.

The `clojure.stacktrace` package has several functions for printing exceptions. `Print-throwable` shows a message and a context:

~~~clojure
(require '[clojure.stacktrace :as trace])

(trace/print-throwable e)
;; clojure.lang.ExceptionInfo: Get user info error
;; {:user-id 5}
~~~

The `with-out-str` macro intercepts the output to the console. The code below does not print anything, but it will return a string:

~~~clojure
(with-out-str
  (clojure.stacktrace/print-stack-trace e))
~~~

The `print-stack-trace` and `print-cause-trace` functions
print a~trace with slight differences. They take the `n` parameter
to~specify the depth of the trace.

## Logging

Console output helps in debugging, but, in a production run, it is not quite usefull. Here's what you can expect from code that catches exceptions.

For any message, we expect to see its auxiliary data. These are the time, namespace, process number. Messages differ in importance: informational, warning, alarm, and others. Printing functions lack this information, and if you collect it manually, the code grows.

When the program runs on multiple servers, message collection is centralized. Imagine an employee switching between hundreds of machines to read logs -- that is no good. If the message came over the network, you need to remember the source address.

Not all messages are of equal importance. Sometimes libraries generate hundreds of debug messages per minute. We need an algorithm to drop some of them by rules.

Plus, the console and file aren't the only data feeds. Especially important messages need to be written to the operating system log, sent to email and chatbots, or people in charge as text messages.

Logging solves the above problems. This system gets messages and sends them to the required channels. The `clojure.tools.logging` library offers functions and macros to log messages. That is a third-party project, so add the dependency:

~~~clojure
[org.clojure/tools.logging "0.4.1"]
~~~

Let's write a simple message:

~~~clojure
(require '[clojure.tools.logging :as log])
(log/info "A message from my module")
~~~

A line will appear in REPL. By default, the log adds a severity level. In our case, this is an informational message, `INFO`.

~~~
INFO: A message from my module
~~~

Clojure logging has two levels. The first one is the entry point, the `log/info`, `log/error`, and other macros. The second level is called the backend. It is a Java library that does the bulk of the work: writes messages to files, sends them over the network, and so on.

Such a structure has advantages. Historically, many logging libraries have written for Java. Each of them offers its classes and methods. If the project uses a particular Java backend, you won't be able to change it without code editing.

In Clojure, this problem was solved by design. At startup, the `logging` module looks for the `Logback`, `Log4j`, and other libraries. If not, it uses the standard `java.util.logging`. The `log/info` call boils down to calling a class from the found library.

To pick up the required backend, add it to the dependencies. The `Logback` project is especially popular among other logging backends. It has a large selection of appenders, unlike its analogs. An appender determines the destination where to write messages. It may be a file, a remote syslog, or a mail server.

Add Logback to your project:

~~~clojure
[ch.qos.logback/logback-classic "1.2.3"]
~~~

Logging libraries look for settings in standard locations. If you put an XML file in the `resources` folder, the backend will read it at startup. Write the following `logback.xml` file there:

~~~xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
  <appender name="STDOUT" class="...ConsoleAppender">
    <encoder>
      <charset>UTF-8</charset>
      <pattern>
        %date %-5level %logger{36} - %msg %n
      </pattern>
    </encoder>
  </appender>
  <root level="INFO">
    <appender-ref ref="STDOUT"/>
  </root>
</configuration>
~~~

So, we have set the appender to output to the console. The `pattern` tag contains a message template. Template parameters start with a percent sign. Substitution `%msg` stands for a message, `%date`  -- for the current date. Its format is specified in curly braces, for example, `%date{ISO8601}`. Restart the REPL to apply the new settings and log something. The output will change:

~~~clojure
(log/info "Hello Logback!")
;; 2019-05-03 17:36:04,001 INFO book.exceptions - Hello Logback!
~~~

## Exception Context

The `log/info`, `log/error`, and other macros first argument might be not a text, but an exception. Above, we have declared the variable `e` for experiments. Let's write it to the log:

~~~clojure
(log/error e "HTTP Error")
~~~

~~~
2019-05-03 17:41:03,913 ERROR book.exceptions - HTTP Error
clojure.lang.ExceptionInfo: Get user info error
    at java.lang.Thread.run(Thread.java:745)
Caused by: clojure.lang.ExceptionInfo: Auth error
    at clojure.lang.Compiler$InvokeExpr.eval(Compiler.java:3701)
    ... 30 common frames omitted
Caused by: clojure.lang.ExceptionInfo: HTTP error
    at clojure.lang.Compiler$InvokeExpr.eval(Compiler.java:3701)
    ... 31 common frames omitted
~~~

To save space, we have removed part of the trace. From the example, you can see the chain consists of three links, which we specified as `e`. But each link has lost data! The problem is that Logback is unaware that the `data` field of the `ExceptionInfo` class is so important to us.

The library offers several templates for exceptions (`%xEx`, `%xException`, and others). They affect the length and detail of the trace but ignore data. Log4j and other projects behave similarly.

In Java, the problem is solved with a new class. Typically, the logging backend offers a class that converts the exception into a string. We create a descendant class and override specific methods. Then we specify a full path to that class in logging settings.

This approach works in Clojure as well, but with some problems. Clojure requires a separate module with the `:gen-class` directive to inherit a class. You will have to read the library documentation, and explore the classes and interfaces. The decision will depend on the specific backend. Switching to Log4 will force us to inherit from another class. There is another way you can follow in Clojure.

First, we write a function ex-print which prints an exception the way we need. Using ex-chain, we traverse the elements and for each one print its class, message and contex. To visually separate parts, we use indents and the pretty printing function which aligns data.

First, we write a function `ex-print` that prints an exception the way we need. With `ex-chain`, we traverse the elements and, for each one, print its class, message and context. To visually separate parts, we use indents and the pretty-printing function, which aligns data.

~~~clojure
(defn ex-print
  [^Throwable e]
  (let [indent "  "]
    (doseq [e (ex-chain e)]
      (println (-> e class .getCanonicalName))
      (print indent)
      (println (ex-message e))
      (when-let [data (ex-data e)]
        (print indent)
        (clojure.pprint/pprint data)))))
~~~

The result looks more attractive than the trace pyramid. The data that led to the error is more visible:

~~~clojure
(ex-print e)

clojure.lang.ExceptionInfo
  Get user info error
  {:user-id 5}
clojure.lang.ExceptionInfo
  Auth error
  {:token "........."}
clojure.lang.ExceptionInfo
  HTTP error
  {:method "POST", :url "http://api.site.com"}
~~~

Now we add a `log-error` function which is a wrapper on top of the `log/error` macro. The difference is, the macro accepts not an instance of an exception but a finite message produced by `ex-print`. To catch printing into a string, we put `ex-print` call inside the `with-out-str` macro.

~~~clojure
(defn log-error
  [^Throwable e & [^String message]]
  (log/error
   (with-out-str
     (println (or message "Error"))
     (ex-print e))))
~~~

We pass a message to `log-error` as the second argument. If it does not exist, write a neutral Error. To prevent the message and body from sticking together, we use `println` to break a line. Here are some examples:

~~~clojure
(log-error e)
(log-error e "HTTP Error 500")
~~~

The second call will log the following:

~~~
2019-05-03 19:00:05,590 ERROR book.exceptions - HTTP Error 500
clojure.lang.ExceptionInfo
  Get user info error
  ...
~~~

Modify the `log-error` so that the message will be a template, and the function will take substitution parameters like `format` does:

~~~clojure
(log-error e "Cannot find user %s, status %s" 5 404)
~~~

In the chapter on mutability, we'll show you how to boil down the `log/error` macro to calling `log-error`. This way, we get rid of importing `log-error` into each module where an error is logged. However, it is too early to talk about it now.

## Collecting Exceptions

Error messages are separate from other entries. There are at least two appenders for this: a console and a file. All messages are output to the console to keep the programmer informed. Errors require our attention so they are written to a file or sent over the network. Later we analyze them manually or with special programs.

We've figured out how to express an exception as text. Now you can write it to a file, send it by email or even print it. A disadvantage to text is that it is not structured. From a code point of view, this is a stream of characters, and it is not clear where things are. When collecting errors, it is crucial to distinguish the key fields such as severity, a subsystem, and module. They are necessary for the following reasons.

**Prevent duplication.** During an influx of customers, there might pop up an error that we did not notice before. If we write it to a file every time, we will get a ton of identical messages. That is resource-intensive and makes it difficult to find other errors.

A collector determines the error similarity according to special rules. The "user1 not found" and "user2 not found" messages are slightly different, but the collector will combine them into one entry and show the rest on demand.

**Search.** You cannot build an efficient search without a structure.
When errors are in the form of text, you can search only by the occurrence of a word or regular expression. This search is not relevant; it ignores how closely the item matches the request. It also does not cut out duplicates: we run the risk of being overwhelmed by identical messages in the search results.

**Statistics.** When we have identified error parts, we can build reports on them. For example, we can find out how many errors happened in the last month or year, or group data by projects or teams, or identify projects where mistakes happen most often.

**Knowledge base.** In a collection system, every error becomes an artifact. We write comments to it, attach screenshots and screen recordings. You can refer to an error in a task or review. If exceptions are logged to plain files, these options are not available.

[Sentry](https://sentry.io), a web application based on Django, looks decent compared to other systems. One uses it to create projects that accumulate customer errors. To send an error to Sentry, use the HTTP POST method. Its body contains JSON with various fields. Sentry offers dozens of fields to describe an error with. There are parameters of a machine, an operating system, HTTP request details, a stack trace, and others.

An application does not collect these data on its own but rather uses libraries. They are often called Raven or similar. It's like a pun -- a raven carries news to a sentry.

For Clojure, [Sentry-clj](https://github.com/getsentry/sentry-clj) and [Exoscale Raven](https://github.com/exoscale/raven) libraries are available. The former relies on the official Java library. Add it to your project:

~~~clojure
[io.sentry/sentry-clj "0.7.2"]
~~~

Set the required DSN to the library. DSN is a project address in Sentry. The project combines messages according to the main feature: backend errors will be in one project, front-end errors -- in the second one, mobile appl's -- in the third. DSN located in the project settings of the Integration section.

~~~clojure
(require '[sentry-clj.core :as sentry])

(def DSN "https://user:pass@sentry.io/project-id")
(sentry/init! DSN)
~~~

Once the library knows the DSN, send the message using the `send-event` function. We're especially interested in the `:throwable` parameter that accepts an exception.

~~~clojure
(sentry/send-event {:throwable e})
~~~

In response, we will receive the event number, and a new entry will appear in the project. Suppose we passed *e*, which is a chain of three exceptions defined above. In the Sentry interface, we will see information about each level. The `:extra` field will contain the data of the top-level exception -- the map `{:user-id 5}`.

`Sentry-clj` does not transmit the entire context; this is its drawback. We would like to see data from all levels, not just the top one. Otherwise, we will not know what the token was, at what address we tried to access the network, and other details. It is possible to collect data manually and pass it to the `:extra` key, but this will increase the code.

The Exoscale Raven library is written in Clojure and therefore takes the language nuances into account. The recent release of the library transmits the complete exception data. Include it in the project and send an exception `e`:

~~~clojure
[exoscale/raven "0.4.13"] ;; project.clj

(require '[raven.client :as raven])
(raven/capture! DSN e)
~~~

~~~json
[ {
  "type": "clojure.lang.ExceptionInfo",
  "message": "Get user info error",
  "data": {
    "user-id": 5
  },
  "at": ["clojure.lang.AFn", "applyToHelper", "AFn.java", 160]
}, {
  "type": "clojure.lang.ExceptionInfo",
  "message": "Auth error",
  "data": {
    "token": "........."
  },
  "at": ["clojure.lang.AFn", "applyToHelper", "AFn.java", 160]
} ]
~~~




Open the event in Sentry and scroll down. In the `extra` section, the `:via` key with detailed information will appear. Below is a snippet of it. Each map consists of an exception class, message, and data. The `:at` field is a vector of four elements: a class, a method, a filename, and the line where an exception was thrown. Such a report makes it easier to find the cause.

The library builds the structure above with the `Throwable->map` function. It takes an exception and returns a map with the `:via`, `:cause`, and other keys. Map items are strings and characters, so the result can be easily written in JSON or EDN formats.

## Sentry and Ring

Now that you are familiar with Sentry, let's write a protective decorator for a Ring application. It catches errors, sends them to Sentry, and returns a neutral response that the request failed. The decorator stays on top of the middleware stack.

~~~clojure
(require '[raven.client :as r])

(defn wrap-exception
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (report-exception request e)
        {:status 500
         :body "Internal error"}))))
~~~

The ninth line carries the main feature of the decorator. This is where we decide what to do with the exception which occurred in the HTTP request. To keep the code simple, let's move this part into a separate function. In addition to the exception, it accepts the request so we can report its method and URI. The overall function is pretty dense, so let's review it in chunks.

~~~clojure
(defn report-exception [request e]
  (let [event (-> nil
                  (r/add-exception! e)
                  (r/add-ring-request! request)
                  (r/add-extra! {:some "data"}))]
~~~

We are passing to Sentry not just an exception, but an event (line 2). First, we augment it with the exception, the request data and some extra fields. Functions named `(r/add-...)` complement the event with various fields. Raven offers functions to add tags, user and other entities. The second step is to send the event safely:

~~~clojure{firstnumber=6}
    (try
      @(r/capture! DSN event)
      (catch Exception e-sentry
        (log/errorf e-sentry "Sentry error: %s" DSN)
        (log/error e "Request failed")))))
~~~

Note that calling Sentry is also wrapped in `try/catch`. Since Sentry is a separate service, it might be unavailable. In this case, both exceptions are written to the log: the original one and the latter we got when calling Sentry. If you forgot how decorators and middleware work, go back to the web-development chapter.

Let's explain the `@` operator before calling `r/capture!` (line 7). The function works asynchronously due to the Manifold library. The result is a special `deferred` object which acts as the Java Future object. We won't know about an error until we dereference the former. We'll encounter the `@` operator in following chapters.

In production systems, one doesn't usually do the dereference of `deferred` objects but wraps them in `d/catch` and `d/chain` macros -- asynchronous analogs of `catch` and the arrow operator &rarr;. Programming in Manifold is a separate topic; we won't touch on it in this book.

## Jumping Code

Until now, we have regarded an exception as an error signal. Recall the fundamental property of an exception -- it interrupts the program flow and bubbles up until it is caught. Sometimes you can use that to move up the stack. If we throw a custom `MyGOTOException` and, at the top, put `catch` with this class, we'll get `GOTO` behaviour:

~~~clojure
(try
  (do-step-1)
  (do-step-2)
  (when (condition)
    (throw (new MyGOTOException)))
  (do-step-3)
  (catch MyGOTOException e
    (println "The third step has been skipped")))
~~~

If `(condition)` in the fourth line returns true, we'll skip the third
step. This is known as the "exception as a control flow mechanism"
technique. The method is controversial; use it carefully. Code becomes obscure
and difficult to maintain.

Still, there are times when you need to interrupt execution. For example, we have found that a user does not have permissions to the resource. Let's complicate the task: there are several checks, and you need to cancel request if any of them fails. Writing in imperative languages, this is easy to solve. Python code might look like this:

~~~python
class AccountHandler(RequestHandler):
  def on_get(self, request):

    if not self.check_params(request):
      return BadRequest("Wrong input data")

    if not self.check_account(request):
      return NotFound("No such an account")

    if not self.check_quotas(request):
      return QuotasReached("Request rate is limited")

    return JSONResponse(self.get_data_from_db())
~~~

There is no `return` statement in Clojure. The result of multiple forms is the result of the last one. We cannot place multiple `when` on the same level, one below the other. Even if only one of them returns false, execution will go to the next form.

The `if/else` cascade works, but looks unwieldy. The issue is also known as the pyramid of doom. The deeper the nesting, the more problems the developer has. For fun, add another condition to the middle of the Clojure code:

~~~clojure
(defn account-handler [request]
  (if (check-this request)
    (if (check-that request)
      (if (check-quotas request)
        {:status 200
         :body (get-data-from-db)}
        (quotas-reached "Request rate is limited"))
      (not-found "No such an account"))
    (bad-request "Wrong input data")))
~~~

You can solve the pyramid problem in different ways, including exceptions. The Ring HTTP Response library offers functions to throw HTTP response exceptions. There is also a decorator which is put on top of the middleware. It catches HTTP-specific exceptions and return the response brought by them. Add the library to your project:

~~~clojure
[metosin/ring-http-response "0.9.1"]
~~~

Let's rewrite `account-handler` using exceptions for control flow:

~~~clojure
(require '[ring.util.http-response
           :refer [not-found!
                   bad-request!
                   enhance-your-calm!]])

(defn account-handler [request]
  (when-not (check-params request)
    (bad-request! "Wrong input data"))
  (when-not (check-account request)
    (not-found! "No such an account"))
  (when-not (check-quotas request)
    (enhance-your-calm! "Request rate is limited"))
  {:status 200
   :body (get-data-from-db)})
~~~

Our new code is now similar to its Python version. Some people feel confused when Clojure code gets reduced to imperative style. Never mind. Our goal is to make the code maintainable. Loyalty to the paradigm doesn't matter here.

Functions of the `ring.util.http-response` module are available with and without an exclamation mark. It signals that a function throws an exception. For example, `not-found!` will throw the `ex-info` which body contains an HTTP response with a 404 status. The ordinary `not-found` would only return the negative response without throwing it.

To make the scheme work, let's add the `wrap-http-response` decorator. It catches errors from functions with an exclamation mark, extracts an answer from them, and returns it to a client. Thus, calling `(not-found!)` in a handler will lead to the 404 Not Found response but not 500 which denotes a server failure.

~~~clojure
(require '[ring.middleware.http-response
           :refer [wrap-http-response]])

(def app
  (-> app-naked
      wrap-params
      wrap-session
      wrap-cookies
      wrap-http-response))
~~~

Try to use code jumping only as a last resort. You should have a powerful argument for that, say, shorten the code or quickly solve the problem. If you have to take this step, use the library to refer to examples and documentation.

## Finally Form and Context Manager

Sometimes the code is executed within a resource. It might be a file, socket, or a database transaction. At the beginning of work, one "opens" the resource and -- at the end -- "closes" it. An open resource is considered busy. When a resource is busy, other clients cannot make full use of it. You can read someone's open file, but can't change it.

Close the resource as soon as you no longer need it. But an unhandled exception can prevent this. If the code working with the resource does not catch the exception, the file or port will remain open until the end of the work. To close the resource even in the case of an error, resort to the `finally` form.

It is placed last in the `try` block. If there is no error, the `finally` form is executed after the main code of `try`. If an exception arises, `finally` will perform between its occurrence and throwing.

Let's take a look at manual access to a file: write a few bytes to it. `Finally` ensures that the file will close even though an exception is thrown in the process.

~~~clojure
(import '[java.io File FileWriter])

(let [out (new FileWriter (new File "test.txt"))]
  (try
    (.write out "Hello")
    (.write out " ")
    (.write out "Clojure")
    (finally
      (.close out))))
~~~

Today's languages offer context managers, so you don't forget to close the file. A manager can be a class, operator, or macro. The manager executes a block of code with side effects. The entry logic is triggered before the block, and the exit logic -- after it. The exit logic will take control even if the block has thrown an exception.
The platform will hold it, execute the exit code, and throw it again.

The Python manager is remarkably elegant. It is the `with` statement that expects an object. The object must have the `__enter__` and `__exit__` methods that will work when entering and exiting the block of code. You can use the `with` statement for files, database transactions, and test fixtures. Here's an example with a file:

~~~python
with open("/path/to/file.txt", "w") as f:
    f.write("test")
~~~

Let's write a similar manager for Clojure. It is a macro that takes a symbol and a file path. Within a macro, a file is bound to a symbol we specified. `Finally` ensures the file will close when exiting the block.

~~~clojure
(defmacro with-file-writer
  [[bind path] & body]
  `(let [~bind (new FileWriter (new File ~path))]
     (try
       ~@body
       (finally
         (.close ~bind)))))
~~~

Execute the code and check the `test.txt` file. It must be with the text and also closed. To ensure it is, delete it using your file manager.

~~~clojure
(with-file-writer [out "test.txt"]
  (.write out "Hello macro!")
  (/ 0 0))
~~~

Context managers' name begins with the prefix `with-`. So they emphasize that the code works within the resource.

Clojure provides the `with-open` macro. It is an improved version of the code we have written. Unlike our example, `with-open` works with a more abstract source, including a file. Exiting the macro, even with an error, closes the resource.

It is important to remember that the code from `finally` will not be included in the result. The `try` expression is designed so that if an error occurs, the result comes from one of the `catch` branches. The `finally` branch serves only for side effects. This is best seen in the example with printing:

~~~clojure
(try (/ 0 0)
     (catch Exception e
       (println "catch")
       :result-catch)
     (finally
       (println "finally")
       :result-finally))
~~~

narrow
Running this form in REPL will print:

~~~clojure
catch
finally
:result-catch
~~~

This order means that Clojure will first execute the code from `catch` (print "catch") and remember the result (keyword `:result-catch`). Then it will execute `finally` (print <<finally>>) and discard the result (keyword `:result-finally`). The `:result-catch` keyword will be the result of the whole `try`.

For comparison, let's give an unfortunate example from practice. It was necessary to perform some action and, regardless of the outcome, return a positive result. The programmer wrapped the code into the `try/catch` block and logged the exception. However, he put the result ino the `finally` branch, which caused strange behavior: `nil` was returned instead of a map.

~~~clojure
(try
  (do-something)
  (catch Exception e
    (log/error e "some error"))
  (finally
    {:status 200}))
~~~

Apparently, the programmer decided that `finally` is literally the final result of `try`. This is not entirely true: `finally` will actually work last, but `try` will return either the result of the main code or one of the `catch` branches.

There is no point in using `finally` when working with collections or pure functions. Use it only when you need to free a busy resource.

## Exceptions and Predicates

The `try/catch` system relies on classes and inheritance. If you need to catch a particular exception, write a class and throw an instance of it. A typical Java or Python project carries a module with custom exceptions. Usually, there is a `ProjectException` class, and others like `UserNotFound` or `AccessDenied` inherited from it.

In the chapter on Spec, we've said that predicates were more powerful than types. The same is true for exceptions: if you can catch them with a function, you don't need a class. The [Slingshot](https://github.com/scgilardi/slingshot) library offers a predicate approach. There are improved versions of `try`, `catch` and `throw` macros in Slingshot.

Those who are not familiar with Clojure will be surprised: you can change even such fundamental things as catching exceptions with the help of macros in it. In other languages, such changes have been waiting for years. In Clojure, you just add a library.

Slingshot contains the `throw+` and `try+` macros. They are compatible with regular `throw` and `try`; if you replace the usual forms with the plus versions, nothing changes. However, these new forms are capable of more.

The `throw+` macro accepts any object, not just an exception. A map works best since it combines multiple values. The code below will throw `ex-info` with the given map:

~~~clojure
(require '[slingshot.slingshot :refer [try+ throw+]])

(throw+ {:user-id 3 :action :create})
~~~

`Throw+` takes an object, a cause exception, a message template, and substitution parameters. Below, here is a code snippet that takes all the arguments into account. Note: the order of the arguments is different from `ex-info`. As Slingshot relies on data, it accepts the object in the first place and a message in the last.

~~~clojure
(let [path "/var/lib/file.txt"]
  (try
    (slurp path)
    (catch Exception e
      (throw+ {:path path} e "File error: %s" path))))
~~~

The `catch` form inside `try+` catches exceptions not only with classes. Slingshot offers selectors and predicates for that. Here, a selector is a vector: its odd element is a map key, and even one is a value. The selector checks if keys and values are included in the context. If so, control passes to the branch with that selector.

~~~clojure
(try+
 (throw+ {:type ::user-error
          :user 5
          :action :update
          :data {:name "Ivan"}})
 (catch [:type ::user-error] e
   (clojure.pprint/pprint e)))
~~~

When the map is thrown, usually we add the `:type` field to the map (line 2). It specifies a key with the current namespace, in our case -- `::user-error`. When catching exceptions in another module, the selector looks like this: `[:type :book.exceptions/user-error]`. The namespace guarantees that we will not intercept `:user-error` of someone else's library.

The example above should print a thrown map. The variable `e`, which is inside `catch`, does not link to the exception, but the data we passed to `throw+`.

Instead of a selector, we can use a predicate, a function of one argument. If the error class is `ExceptionInfo`, the predicate will get a data map; otherwise, an exception object. The function should take into account an argument type. When the predicate returns true, control will pass to its `catch` branch.

Use predicates when you need fine tuned interception conditions. Let's see how to upload a file to Amazon S3. In special cases, Java SDK throws an exception *after* the file has been uploaded to S3 (for example, if the checksums do not match). We have to delete the unsuccessful uploaded file so as not to waste resources.

Unfortunately, there is neither the `ChecksumError` class nor an equivalent in SDK. Instead, we'll receive a regular `AmazonS3Exception` with a long text. To highlight our case, let's compare the message with the template. Let's make a check function first:

~~~clojure
(defn aws-checksum-error? [e]
  (and (instance? AmazonS3Exception e)
       (some?
        (re-find
         #"(?i)The Content-Md5 you specified did not match"
         (ex-message e)))))
~~~

Now we put it in `try+` to catch the checksum mismatch case:

~~~clojure
(try+
  (s3/put-object ...)
  (catch aws-checksum-error? e
    (s3/delete-object ...)))
~~~

[Clj-http](https://github.com/dakrone/clj-http), a popular HTTP client for Clojure, uses Slingshot. In case of an error, it throws a response with `throw+`. If we wrap the request in `try+`, we'll get a more detailed analysis of errors. For example, we can get separate branches for status 403, 500, and for the general negative response.

~~~clojure
(require '[clj-http.client :as client])

(try+
 (client/get "http://example.com/test")
 (catch [:status 403] e
  (println "Access denied"))
 (catch [:status 500] e
   (println "The service is unavailable"))
 (catch [:type :client/unexceptional-status] e
   (println "Negative response")))
~~~

Slingshot relies on data, not classes, as Clojure encourages. Benefits of this approach are not always apparent to newbies. It is helpful to understand the usual `try/catch` method first. Include Slingshot only if it makes sense to do so.

Slingshot is not the only example of exceptions for Clojure. Pay attention to [Ex](https://github.com/exoscale/ex) -- the analog developed at Exoscale. The library takes into account the semantics of the keys (a failure, conflict, etc), and their inheritance, supports Manifold, and much more.

## Techniques and Functions

Let's look at several techniques for exceptions. They are simple, so you shouldn't put them in a separate library. They are usually copied from a project to a project with some changes.

**Safe function call.** Lua language has no `try` and `catch` statements. To make the call safe from error, use the [`pcall`](https://www.lua.org/pil/8.4.html) operator. It is short for a protected call. Sometimes this technique is useful in Clojure, too.

`Pcall` takes a function and arguments and returns a pair of values. The first of them signifies a successful execution. If it is true, the second value will be the result of the computation, otherwise -- an error instance. Clojure version:

~~~clojure
(defn pcall [f & args]
  (try
    [true (apply f args)]
    (catch Exception e [false e])))
~~~

To make the result more concise, process it with `let`:

~~~clojure
(let [[ok? result-error] (pcall inc 1)]
  (if ok?
    (println (str "The result is " result-error))
    (println "Failure")))
~~~

A different semantics is popular in JavaScript: callback functions take the `error` and `result` arguments. The `pcall-js` function is a modified version of `pcall` that returns an &#x3008;&nbsp;error, result&nbsp;&#x3009; pair. When the first item if the pair is not `nil`, it means the call failed.

~~~clojure
(defn pcall-js [f & args]
  (try
    [nil (apply f args)]
    (catch Exception e [e nil])))
~~~

The split looks different:

~~~clojure
(let [[e user] (pcall-js get-user-by-id 7)]
  (if e
    (println (ex-message e))
    (println user)))
~~~

**Retries with delay.** Sometimes a third party service is unstable, and you have to access it multiple times. Let's wrap the retrying in a function so as not to copy the code. `Call-retry` tries to call the target function in multiple passes. The result will be the first successful calling. If an error occurs but the number of attempts haven't completed, the function waits and retries the call. When the number of attempts is over, the function will throw an exception. Internally, we use the `pcall` function to prevent an exception from raising.

~~~clojure
(defn call-retry [n f & args]
  (loop [attempt n]
    (let [[ok? res] (apply pcall f args)]
      (cond
        ok? res

        (< attempt n)
        (do (Thread/sleep (* attempt 1000))
            (recur (inc n)))

        :else (throw res)))))
~~~

Here is how we fetch a user by id from a remote server in three attempts:

~~~clojure
(call-retry 3 get-user-by-id user-id)
~~~

That is a naive version; you can improve it by changing the waiting strategy and adding logs. However, the principle remains the same.

By the way, these functions are compatible with each other. Let's build a combination of `pcall` and `call-retry`. Even if the service is not available and we have knocked on it many times, we will get a pair `[ok? result]`.

~~~clojure
(pcall call-retry get-user-by-id 5)
~~~

**Exceptions in a loop.** The `loop` form is designed in a special way: its `recur` part cannot be inside `try`. Let's say we want to express retry logic without `pcall`. Here is a snippet of code:

~~~clojure
(defn call-retry [n f & args]
  (loop [attempt n]
    ...
    (try
      (apply f args)
      (catch Exception e
        (recur (inc n))))))
~~~

The compiler will not accept the code with the message: "Can only recur from tail position". To fix the error, you need to take out `recur` from `try`. The easiest way to do this is with `pcall`, as in the example with `call-retry`.

**Throw in place.** By now, we have thrown exceptions using `ex-info` and `throw`. The former builds an exception, the latter throws it. Let's combine them in the `error!` function and simplify the arguments:

~~~clojure
(defn error! [message & [data e]]
  (throw (ex-info message (or data {}) e)))
~~~

The function needs only one parameter -- a string -- to throw an exception, and the others are optional:

~~~clojure
(error! "Error")
(error! "Error" {:type ::error})
(error! "Error" {:type ::error} e)
~~~

**Messages with parameters.** Sometimes a detailed string is enough for an error. In this case, `ex-info` is redundant because it requires a map as an argument. Now, we'll write a function that throws `Exception` with a formatted message. It takes a template and substitution values.
The `f` added to the function name means formatting.

~~~clojure
(defn errorf! [template & args]
  (let [message (apply format template args)]
    (throw (new Exception ^String message))))

(errorf! "Error, user: %s, action: %s" 5 :delete)
~~~

**Safe macro.** Catching of exceptions works in macros as well. Code with macros is usually shorter and more expressive than a regular function (of course, it doesn't mean you should misuse them). The following example shows how to execute code in safe mode, ignoring an exception:

~~~clojure
(defmacro with-safe [& body]
  `(try
     ~@body
     (catch Exception e#)))
~~~

An empty `catch` form will return `nil`. We'll get it if an error occurs:

~~~clojure
(with-safe (/ 0 0))
nil
~~~

In practice, `nil` does not always mean an error, so the `:error` or `:invalid` keys are returned. That is how the Spec library works, which we discussed in the last chapter. Sometimes, we add logging or a Sentry call in the `catch` block. Only suppress the error if the result is unimportant at all.

## Summary

Exceptions in Clojure are similar to Java's. The `try` and `catch` forms are similar to Java statements of the same name. Interception works on classes and inheritance. The higher the class in the hierarchy, the more error cases it covers.

The `ExceptionInfo` class is specifically designed for Clojure. Its `data` field takes any map. The exception type is determined by the `:type` field of the map which usually carries a full-qualified keyword. The `ex-info` function is an exception constructor.

An error may have a `cause`. If you catch an exception but don't know what to do with it, throw a new one with a context and a link to the initial exception. That is how exception chains are built. Data from the entire chain is taken into account, not just from the top link, to investigate the issue.

Special code on top of an application decides what to do with an exception. The best choice would be to pass it to the error collection system. Sentry or its analogs can be such a system. Make sure the Sentry client passes complete information about an exception, not just the stack trace.

If an error occurs, we usually close the resource in the `finally` branch -- to prevent it from staying busy. A context manager makes it easy to access the resource. Usually, it is a `with-<something>` macro wrapping a block of code in `try/finally`. Clojure offers a `with-open` macro working with various data sources.

Sometimes we can use an exception to jump to a particular place in the code. That is a controversial approach, and you must have good reasons for using it. If jumping code is vital, use a particular library but not a handmade solution.

Slingshot offers the improved macros `try+` and `throw+`. With them, you catch errors with selectors and predicates, not classes. Instead of exception objects, Slingshot uses plain maps.

Some functions simplify the exception control. Please take note of them to shorten your code.
