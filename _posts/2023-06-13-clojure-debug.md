---
layout: post
title:  "Avoid code you cannot debug"
permalink: /en/clojure-debug/
lang: en
tags: clojure programming debugging
---

This is a small tip I'd like to share with Clojure programmers.

**In a project, avoid code you cannot debug.** It's simple: if you can put a
tag like `#debug` or similar somewhere in the middle, run a test and hang in a
debugging session, you're good. But if you cannot, you'll be in trouble one day.

Thus, any kind of DSL or yet another "smart" solution is a source of potential
problems. Take Meander, for example. Imagine I have a map like this:

~~~clojure
{:name "Ivan"
 :address {:city "Chita"}}
~~~

and I want it to become this:

~~~clojure
{:name "Ivan"
 :city "Chita"}
~~~

With Meander, I would write:

~~~clojure
(m/match
  {:name "Ivan" :address {:city "Chita"}}
  {:name ?name :address {:city ?city}}
  {:name ?name :city ?city})
~~~

and it works fine. But one day, my datasource suddenly returns a user without an
address:

~~~clojure
{:name "Ivan" :address nil}
;; or just {:name "Ivan"}
~~~

which is completely fine because a user might have no address associated with
them. By passing that map match, I expect it to return `{:name "Ivan" :city nil}`
but no: there will be an exception:

~~~clojure
(m/match
   {:name "Ivan" :address nil}
   {:name ?name :address {:city ?city}}
   {:name ?name :city ?city})

Unhandled clojure.lang.ExceptionInfo
   non exhaustive pattern match
   {}
~~~

The line "non exhaustive pattern match" tells nothing to me nor the ex-data
does. The message is fuzzy, there is no context, the ex-data has nothing useful
(it's an empty map). Having such an entry in logs or Sentry would not help you
in a bit.

Moreover, you cannot debug it. The `m/match` macro expands into a huge block of
code. Debugging it somewhere in the middle would be quite challenging.

Now compare it with a plain function that splits the data step by step:

~~~clojure
(defn remap-user [entry]

  (let [{username :name
         :keys [address]}
        entry

        {:keys [city]}
        address]

    {:name username
     :city city}))
~~~

First, it works with both maps:

~~~clojure
(remap-user {:name "Ivan"})
=> {:name "Ivan", :city nil}

(remap-user {:name "Ivan" :address {:city "Chita"}})
=> {:name "Ivan", :city "Chita"}
~~~

Second, I can always put a debugging tag into that function and observe the
local variables, the state and even run some expressions. With Meander, it's
just impossible or only possible with certain effort.

Third, if a city is required, I'd put something like this:

~~~clojure
(assert city "The city is missing")
~~~

and get a clear exception I want.

Vast Meander patterns are completely unmaintainable. Pass something weird and
you'll get a "non exhaustive pattern match" message with no idea about what went
wrong.

Finally, debugging is crucial. If you cannot hang in the middle of execution and
observe the state, that's bad. Most Clojure programmers believe it's a special
language liberating you from debugging errors, but it's not true. Debugging has
not gone anywhere even with such a great language as Clojure.
