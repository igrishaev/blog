---
layout: post
title: "On cowardness in Clojure code"
permalink: /on-clojure-cowardness/
tags: programming clojure
telegram_id:
---

Imagine you have a function accepting a value and doing something with it:

~~~clojure
(defn double-number [x]
  (when x
    (* x 2)))

;; or

(defn get-user [id]
  (when id
    (jdbc/execute! *db* ["select..." id])))
~~~

A common pattern which comes all the time is to wrap the entire body with a
`(when id ...)` form. You don’t want to process nil values so it’s safer to
protect yourself against NPEs. Without `(when ...)`, a nil value can ruin the
entire pipeline, cause a null pointer error, trigger DB queries in vain and so
on. These all sound reasonable, yet I’ve got my own term to describe such a kind
of code: “coward style”.

A person who is wrapping the whole function with `(when)` isn’t getting one
thing. **If a function had been given nil, it should have never been called
instead**. Clojure provides a number of macros to call a function conditionally
depending on arguments, for example:

~~~clojure
(some-> (get-user-id) (get-user))
~~~

Should `(get-user-id)` return nil, the `(get-user nil)` form never gets
called. The same applies to `(cond->)` and other macros that build an execution
form conditionally.

In other words: a function must not check its input parameters for nils. But
those people who call this function must.

Now let me explain the "coward style" I mentioned before. It’s when people write
like this:

~~~clojure
(defn double-number [x]
  (when x
    (* x 2)))
~~~

I don’t know what this code tells you, but to me, it’s clearly this: *“Guys, I
don’t want any problems. I don’t want any exceptions to be raised. If you supply
me with a number, I’ll double it but won’t do anything if you pass nil. I cannot
process it but won’t argue on you. Let’s keep it all quiet. Deal?”*

This is a speech of a typical coward: I don’t want problems. I don’t want stack
traces. I don't want alerts and investigations. Let's be quiet. The job is
half-way done in fact as the function works partially. It won’t tell you when
something is not quite right.

I’ve seen plenty of computation chains like this:

~~~clojure
(-> some-param
    (parse-param)
    (pre-process-param)
    (get-use-id)
    (get-user-by-id)
    (send-user-somewhere))
~~~

Now imagine that **every** function starts with `(when ...)`, and the final
function crashes with NPE. It will be quite challenging to find who is
guilty. These functions are real cowards: nobody wants to take blame. *"I got
nil &rarr; I returned nil. Not my business. I washed my hands".*

Thus, stop writing functions starting with `(when ...)`. If a function silently
swallows a nil value doing nothing, sooner or later you’ll pay for that. Or your
teammates will.

There is still a way though to protect yourself against nils which I like a
lot. Use built-in pre- and post conditions:

~~~clojure
(defn double-number [x]
  {:pre [(number? x)]}
  (* x 2))
~~~

Now if you pass nil, you’ll get a clear error

~~~clojure
(double-number nil)

;; Execution error (AssertionError) at … (REPL:211).
;; Assert failed: (number? x)
~~~

Preconditions help a lot with guessing types. Above, they clearly say `x` must
be a number and nothing else. In addition to `:pre` and `:post` forms, the
standard `(assert ...)` form might help in the middle of a function to interrupt
execution when you know it makes no sense to go on with a weird value.

Keen mind that `:pre`, `:post`, and `assert` forms rely on the global `*assert*`
variable. It’s a good practice to assertions a lot but wipe them off on
production as they slow down the code. When baking an uberjar, set
`clojure.core/*assert*` to false. If it’s ClojureScript with a shadow compiler,
pass `{:elide-asserts true}` into the `:compiler-options` map for a production
release.

I agree that pre/post and assertions take lines of code, and sometimes they make
code a bit noisy. But they will save you hours of debugging. Don’t be a coward
whose main goal is to avoid exceptions. Don’t hide weird things. Be simple and
explicit, and let your code express these two qualities.
