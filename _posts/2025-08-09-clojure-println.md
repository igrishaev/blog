---
layout: post
title: "Don't use println with two and more arguments"
permalink: /clojure-println/
tags: clojure programming
telegram_id:
---

When printing, please avoid `println` invocations with more than one argument,
for example:

~~~clojure
(defn process [x]
  (println "processing item" x))
~~~

Above, we have two items passed into the function, not one. This style can let
you down when processing data in parallel.

Let's run this function with a regular `map` as follows:

~~~clojure
(doall
 (map process (range 10)))
~~~

The output looks fair:

~~~text
processing item 1
processing item 2
processing item 3
processing item 4
processing item 5
processing item 6
processing item 7
processing item 8
processing item 9
~~~

Replace `map` with `pmap` which is a semi-parallel method of processing. Now the
output goes nuts:

~~~clojure(doall
(pmap process (range 10)))

processing itemprocessing item  10

processing item processing item8 7
processing item 6
processing itemprocessing item 4
processing item 3
processing item 2
 5
processing item
 9
~~~

Why?

When you pass more than one argument to the `println` function, it doesn't print
them at once. Instead, it sends them to the underlying `java.io.Writer` instance
in a cycle. Under the hood, each `.write` Java invocation is synchronized so no
one can interfere when a certain chunk of characters is being printed.

But then multiple threads print something in a cycle, **they do interfere**. For
example, one thread prints "processing item" and before it prints "1", another
thread prints "processing item". At this moment, you have "processing
itemprocessing item" on your screen.

Then, the first thread prints "1" and since it's the last argument to `println`,
it adds `\n` at the end. Now the second thread prints "2" with a line break at
the end, so you see this:

~~~text
processing itemprocessing item
1
2
~~~

The more cores and threads you computer has, the more entangled the output
becomes.

This kind of a mistake happens often. People do such complex things in a `map`
function like querying DB, fetching data from API and so on. They forget that
`pmap` can bootstrap such cases up to ten times. But unfortunately, all prints
produced by a function passed to `pmap` get entangled.

There are two things to remember. The first one is to not use `println` with
more than one argument. For two and more, use `printf` as follows:

~~~clojure
(defn process [x]
  (printf "processing item %d%n" x))
~~~

Above, the `%n` sequence stands for a platform-specific line-ending character
(or a sequence of characters, if Windows). Let' check it out:

~~~clojure
(pmap process (range 10)))

processing item 0
processing item 2
processing item 1
processing item 4
processing item 3
processing item 5
processing item 6
processing item 8
processing item 9
processing item 7
~~~

Although the order of numbers is random due to the parallel nature of `pmap`,
each line has been consistent.

One may say "just use logging" but too often, setting up logging is another
pain: add `clojure.tools.logging`, add `log4this`, add `log4that`, put
`logging.xml` into the class path and so on.

The second thing: for IO-heavy computations, consider `pmap` over `map`. It
takes an extra "p" character but completes the task ten times faster. Amazing!
