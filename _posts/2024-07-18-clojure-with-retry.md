---
layout: post
title: "Clojure AntiPatterns: the with-retry macro "
permalink: /en/clojure-with-retry/
tags: programming clojure retry s3
---

Most of clojurians write good things about Clojure only. I decided to start
sharing techniques and patterns that I consider bad practices. We still have
plenty of them in Clojure projects, unfortunately.

My first candidate is widely used, casual macro called `with-retry`:

~~~clojure
(defmacro with-retry [[attempts timeout] & body]
  `(loop [n# ~attempts]
     (let [[e# result#]
           (try
             [nil (do ~@body)]
             (catch Throwable e#
               [e# nil]))]
       (cond

         (nil? e#)
         result#

         (> n# 0)
         (do
           (Thread/sleep ~timeout)
           (recur (dec n#)))

         :else
         (throw (new Exception "all attempts exhausted" e#))))))
~~~

This is a very basic implementation. It catches all possible exceptions, has a
strict number of attempts, and the constant delay time. Typical usage:

~~~clojure
(with-retry [3 2000]
  (get-file-from-network "/path/to/file.txt"))
~~~

Should network blink, most likely you'll get a file anyway.

Clojure people who don't like macros write a function like this:

~~~clojure
(defn with-retry [[attempts timeout] func]
  (loop [n attempts]
    (let [[e result]
          (try
            [nil (func)]
            (catch Throwable e
              [e nil]))]
      (cond

        (nil? e)
        result

        (> n 0)
        (do
          (Thread/sleep timeout)
          (recur (dec n)))

        :else
        (throw (new Exception "all attempts exhausted" e))))))
~~~

It acts the same but accepts not arbitrary code but a function. A form can be
easily turned into a function by putting a sharp sign in front of it. After all,
it looks almost the same:

~~~clojure
(with-retry [3 2000]
  #(get-file-from-network "/path/to/file.txt"))
~~~

Although it is considered being a good practice, here is the outcome of using it
in production.

Practice proves that, even if you wrap something into that macro, you cannot
recover from a failure anyway. Imagine you’re downloading a file from S3 and
pass wrong credentials. You cannot recover no matter how many times you
retry. Wrong creds remain wrong forever. Now there is missing file: again, no
matter how hard you retry, it’s all in vain and you only waste resources. Should
you put a file into S3, and submit wrong headers, it’s the same. If your network
is misconfigured or some resources are blocked, or you have no permissions, it’s
the same again: **no matter how long have you been trying, it's useless**.

There might be dozens of reasons when your request fails, and there is no way to
recover. Instead of invoking a resource again and again, you must investigate
what went wrong.

There might be some rare cases which are worth retrying though. One of them is
an `IOException` caused by a network blink. But in fact, modern HTTP clients
already handle it for you. If you `GET` a resource and receive an `IOException`,
most likely your client has already done three attempts silently with growing
timeouts. By wrapping the call `with-retry`, you perform 9 attempts or so under
the hood.

Another case might be 429 error code which stands for rate limitation on the
server side. Personally I don’t think that a slight delay may help. Most likely
you need to bump the limits, rotate API keys and so on but not `Thread.sleep` in
the middle of code.

I’ve seen terrible usage of `with-retry` macro across various projects. One
developer specified 10 attempts with 10 seconds timeout to reach a remote API
for sure. But he was calling the wrong API handler in fact.

Another developer put two nested `with-macro` forms. They belonged to different
functions and this could not be visible at once. I’m reproducing a simplified
version:

~~~clojure
(with-retry [4 1000]
  (do-this ...)
  (do-that ...)
  (with-retry [3 2000]
    (do-something-else...)))
~~~

According to math, 4 times 3 is 12. When the `(do-something-else)` function
failed, the whole top-level block started again. It led to 12 executions in
total with terrible side effects and logs which I could not investigate.

One more case: a developer wrapped a chunk of logic that inserted something into
the database. He messed up with foreign keys so the records could not be
stored. Postgres replied with an error "foreign key constraint violation" yet
the macro tried to store them three times before failing completely. Three
broken SQL invocations... for what? Why?

So. Whenever you use `with-retry`, most likely it’s a bad sign. Most often you
cannot recover from a failure no matter if you add two numbers, upload a file,
or write into a database. You should only retry in certain situations like
`IOException` or rate limiting. But even those cases are questionable and might
be mitigated with no retrying.

Next time you're going to cover a block of logic `with-retry`, think hard if you
really need to retry. Will it really help in case of wrong creds, a missing
file, incorrect signature or similar things? Perhaps not. Thus, don't retry in
vain. Just fail and write detailed logs. Then find the real problem, fix it and
let it never happen again.
