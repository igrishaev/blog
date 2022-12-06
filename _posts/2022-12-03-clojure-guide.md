---
layout: post
title:  "Clojure Coding Guide"
permalink: /en/clojure-guide/
tags: clojure code
lang: en
draft: true
---

[bbatsov]: https://github.com/bbatsov/clojure-style-guide

*TL;DR: this is a detailed description of how to write good Clojure code. It's based on my 8 years of experience with Clojure for both commercial purposes and side projects as well. Some parts of this document repeat the [well-known guide][bbatsov] by Bojidar. Other parts instead break the conventional rules in Clojure development. For such cases, I give an explanation of why they are what they are. Everything written below has come from practice, and I hope you'll find it useful.*

{% include toc.html id="clojure-guide" %}

## Parentheses

Let's start with something obvious yet worthy of repeating. When writing Lisp code, don't balance parentheses as you did before in Python or JavaScript. Not like this:

~~~clojure
(time
 (doseq [a [1 2 3]]
   (let [b (* a a)]
     (println b)
     )
   )
 )
~~~

But this:

~~~clojure
(time
 (doseq [a [1 2 3]]
   (let [b (* a a)]
     (println b))))
~~~

The same rule applies to collections. The following code fragments look weird in Clojure:

~~~clojure
(def mapping {
   :name "Ivan"
   :email "test@test.com"
})

(def numbers [
  1,
  2,
  3
])
~~~

It is a strong rule for every Lisp dialect and you've got to get on with it. The sooner you get an editor powered with a plugin to manage parenthesis the better it is for you as a programmer. Emacs + Paredit is a good choice but it's a matter of preference.

<!-- more -->

## Namespaces

In the `:require` sub-form of the `ns` header, place the imports on the next line but not on the same. Namely, likes this:

~~~clojure
(ns some.ns
  (:require
   [clojure.string :as str]
   [clojure.java.io :as io]))
~~~

But not this:

~~~clojure
(ns some.ns
  (:require [clojure.string :as str]
            [clojure.java.io :as io]))
~~~

The second example takes 9 more spaces in breadth. Both our software and hardware are developed such that it's easier to scroll the text down rather than to the right. Thus, grow your code vertically but not horizontally. When it's possible to shorter a line by pressing enter, always do this.

When importing a namespace, specify its full path. Don't use nested vectors although technically it's possible:

~~~clojure
(ns some.ns
  (:require
   [clojure [edn :as edn]
            [walk :as walk]
            [string :as str]]))
~~~

In this case, you'll get troubles with grepping the code with `clojure.walk` or any other namespace — there won't be such a match.

For aliases, use the part of a namespace after the last dot. If there are two and more namespaces with the same ending, use two last parts, and so on:

~~~clojure
(ns some.ns
  (:require
   [clojure.walk :as walk]))

(ns some.ns
  (:require
   [clojure.walk :as clojure.walk]
   [some.utils.walk :as utils.walk]))
~~~

Don't use one-letter aliases even for the built-in modules like `clojure.string` or `clojure.walk`:

~~~clojure
(ns some.ns
  (:require
   [clojure.string :as s]))

(s/starts-with? ...)
~~~

Instead, use `string`, `walk`, `edn`, and similar. By the way, the `str` alias is OK for `clojure.string` but it's a well-known exception:

~~~clojure
(ns some.ns
  (:require
   [clojure.string :as str]))

(str/starts-with? ...)
~~~

Never use the `:use` clause in the namespace form. The use form acts like the `import *` clause in Python or `import foo.bar.*` in Java. Both two are considered bad practices as the code becomes confusing. The same works for Clojure: without an alias, it's unobvious where a certain function comes from:

~~~clojure
(ns some.ns
  (:use clojure.walk))
~~~

That's a good idea to group imports by their semantics, namely:

1. modules of this project;
2. third-party modules;
3. built-in Clojure modules.

For example:

~~~clojure
(ns project.core
  (:require
   [project.handlers :as handlers]
   [project.routes :as routes]
   [project.server :as server]

   [honey.sql :as sql]
   [cheshire.core :as json]
   [clojure.java.jdbc :as jdbc]

   [clojure.string :as str]
   [clojure.java.io :as io]))
~~~

Should the `:require` section grow in time, such grouping will help you to review and control the dependencies.

And instead, when all the imports are dumped in a single vector, that's difficult to understand what's going on.

~~~clojure
(ns project.core
  (:require
   [project.handlers :as handlers]
   [clojure.java.io :as io]
   [project.routes :as routes]
   [cheshire.core :as json]
   [clojure.java.jdbc :as jdbc]
   [honey.sql :as sql]
   [project.server :as server]
   [clojure.string :as str]))
~~~

Sorting modules is useful even if they're not grouped as described in the previous paragraph. In Emacs, there is a `M-x clojure-sort-ns` command that does it for you. You can also sort the imports manually using `M-x sort-lines`.

Some modules change the global state, for example extend multimethods, protocols and so on. Always import them in the core module of your project to prevent loosing their effect. Use neither alias nor square brackets for them to highlight it's a special case. Put a short comment on the right saying what it does:

~~~clojure
(ns project.core
  (:require
   project.db.json ;; extends JDBC PGObject for json(b)
   [project.handlers :as handlers]
   [project.routes :as routes]))
~~~

Without this precaution, you teammates might take it as an unused namespace, delete it and face a weird behaviour when a multimethod or a protocol lacks implementation.

## Variables and defs

Both `def` and `defn` forms must only be on top of a module but never inside `let` or other forms. Not this:

~~~clojure
(defn get-user-info [user-id]
  (def user (get-user user-id))
  (def profile-id (:profile-id user))
  (def profile (get-profile profile-id))
  {:user user
   :profile profile})
~~~

But this:

~~~clojure
(defn get-user-info [user-id]
  (let [user (get-user user-id)
        profile-id (:profile-id user)
        profile (get-profile profile-id)]
    {:user user
     :profile profile}))
~~~

Inner `define` forms are OK for Scheme which most people familiar with by SICP. But this is Clojure not Scheme.

In some rare cases though, we *do use* inner Clojure definitions. This is useful when a function is closed over some precalculated value and thus is put inside `let`:

~~~clojure
(let [-rules (read-json "resources/rules.json")]
  (defn get-rule [id]
    (get-in -rules [:system :rules id])))
~~~

The `-rules` variable is processed once so there is no need to parse JSON for every call. Also, it is only visible to the `get-rule` function so no one can interfere.

When introducing a dynamic variable, don't anticipate the users typing something like this every time:

~~~clojure
(binding [*locale* :ru]
  (translate :message/hello))
~~~

Instead, provide `with-locale` macro that does everything for them:

~~~clojure
(with-locale :ru
  (translate :message/hello))
~~~

Thanks to its name, the macro gives more sense of what's happening. Also, you can adjust it in time (add logs, side effects and so on).

## Atoms

In general, avoid using atoms. Every time you're about to bring one into your code, think if it's possible to get rid of it. `Loop`, `reduce` and other patterns most likely will satisfy your intentions with no atoms.

Even if you drive your code with atoms, avoid storing them on the top of a module. This is a bad example:

~~~clojure
(def storage
  (atom {}))

(defn set-item [idx item]
  (swap! storage assoc idx item))

(defn get-item [idx]
  (get @storage idx))
~~~

Such code is quite tricky to test as you continuously need to reset the state between the tests. The state, although driven by an atom, must have been a component of the system (see below).

Don't use `*` or `%` characters to highlight it's an atom. This is bad:

~~~clojure
(def *storage
  (atom {}))

(def %storage
  (atom {}))
~~~

Stars, ticks and similar bring noise into the code. The `atom-` or just `a-` prefix usually is enough:

~~~clojure
(def atom-storage (atom {}))

(def a-storage (atom {}))
~~~

The better choice is an exclamation mark at the end of the name:

~~~clojure
(def storage!
  (atom {}))
~~~

The `storage!` name is in line with the functions that operate on atoms: `swap!`, `reset!` and so on. Thus, the code with ! looks neat:

~~~clojure
(def storage! (atom {}))

(swap! storage! ...)
(reset! storage! ...)
~~~

Talking more generally, an exclamation mark is a good option to highlight *any mutable type*, for example, a transient collection. Their functions also end with !:

~~~clojure
(loop [acc! (transient [])]
  (if ...
    (persistent! acc!)
    (recur (conj! acc item))))
~~~

## Exceptions

Use exceptions but not monads. Exceptions are dull yet straightforward and predictable. Clojure ecosystem is not monad-friendly at all. Although we do have some developed libraries for monands, they're rarely used in production.

The main problem with monads is, implementing a `Maybe` type is not enough; you've got to write monadic versions of `let`, `if`, `when`, `cond`, `case`, `->`, `->>` and so forth. Moreover, people must learn all this which they rarely have time and motivation for.

I recall I only needed something like monads once before. Throwing an exception was not the case so I made `Success` and `Failure` wrappers on top of ordinary data types. With a couple of custom macros, it worked fine without introducing a new library to the project.

Don't return exceptions as values. Everyone who uses your code has to check the result with if/else which brings nesting and complexity:

~~~clojure
(defn get-user [user-id]
  (try
    (jdbc/get-by-id *db* :users user-id)
    (catch Throwable e
      e)))

(let [user (get-user 1)]
  (if (throwable? user)
    (do
      (log/error ...)
      (throw user))
    (process user ...)))
~~~

The same applies to returning error maps. Again, everyone has to if/else on the result, which most likely they will forget one day:

~~~clojure
(defn get-user [user-id]
  (try
    (jdbc/get-by-id *db* :users user-id)
    (catch Throwable e
      {:type :error :message (ex-message e)})))

(let [user (get-user 1)]
  (if (error-map? user)
    (do
      (log/error ...)
      (throw ...))
    (process user ...)))
~~~

The standard way of throwing exceptions in Clojure is verbose: it requires two forms (`throw` and `ex-info`) as well as a context map where sometimes you have nothing to put into it yet you must:

~~~clojure
(throw (ex-info "Some message" {:some "context"}))
~~~

A good idea would be to create a `project.error` namespace in your project and provide shortcuts that combine initialisation and throwing:

~~~clojure
(defn error!

  ([message]
   (throw (ex-info message {})))

  ([message data]
   (throw (ex-info message data)))

  ([message data cause]
   (throw (ex-info message data cause))))
~~~

The example below takes less code and is more readable:

~~~clojure
(when-not user
  (error/error! "User not found" {:id user-id}))

# or when the error! function was :refer-red

(when-not user
  (error! "User not found" {:id user-id}))
~~~

Having a bottleneck that all the exceptions pass through, you can adjust it in the future. Say, add more data into the context map (time, host, type, the namespace, etc). Introduce this error module as soon as possible and stick with it through the whole code base.

An error module is also a good place for various exception-handling macros. Namely, retrying, protected call which returns a pair of `[result, exception]`, and so on.

## Assertions

Assertions are somewhat exceptions as they blow up when something goes wrong. Having assertions is good for development but not for production. The thing is, the `assert` macro relies on the `*assert*` global dynamic var. When it's false, none of the `assert` forms works as expected:

~~~clojure
(set! *assert* false)

(let [data {:foo 1}]
  (assert (get data :bar) "Bar is missing")) => nil
~~~

Setting `*assert*` to false is common when building an uberjar. In this case, all the assertions are removed completely from the resulting bytecode. If you rely on assertions in code a lot, keep in mind they can be disabled on prod.

The pre- and post- checks in `defn` rely on `*assert*` as well:

~~~clojure
(set! *assert* false)

(defn process-user
  [user]
  {:pre [(some? user)]}
  (assoc user :some "field"))

(process-user nil)
;; {:some "field"}
~~~

Anyway, pre- and post- are great for critical parts of the code; using them is considered as a good practice.

## Functions

Never use the `#(...)` syntax for anonymous functions since they're really difficult to read. The `%1`, `%2`, ... symbols bring chaos to code. Whenever you typed `#(...`, delete it and provide an ordinary named function. No:

~~~clojure
(filter #(= (:id %) product-id) products)
;; or
(update :body #(json/parse-string % keyword))
~~~

Yes:

~~~clojure
(filter (fn [product]
          (= (:id product) product-id))
        products)
;; or
(update :body json/parse-string keyword)
~~~

What is really bad with `#(...)`, the arguments have no names and thus lack semantics. The `(fn ...)` form can be easily put on the top level of a module and transformed into `defn`, so it can be tested in REPL or unit tests.

~~~clojure
(fn [product]
  (= (:id product) product-id))
~~~

Use `map` only for primitive cases like `inc`-ing a collection of numbers or similar:

~~~clojure
(map inc numbers)

(map str objects)
~~~

Passing vast anonymous functions into `map` makes the code unreadable:

~~~clojure
(map (fn [{:keys [...]}]
       (if this
         (with-something
           (log/info ...)
           (do-that this))
         (let [foo 1
               bar 2]
           (therwise this ...)))) entities)
~~~

This is taken from a real project I've worked on. I always hang when facing such code. The first thing I usually do is take the function out from the map either on top of the module or at least into the preceding `let` clause. Then, pass the function as a variable:

~~~clojure
(defn process-entity [entity]
  ...)

(map process-entity entities)
~~~

The better approach would be just to get rid of `map` in favour of `for` macro:

~~~clojure
(for [entity entities]
  (let [{:keys [...]}]
    (return something)))
~~~

It's clearer and simpler for the readers.

Don't use `partial` for the same reason: it ruins the easiness of reading. The worst thing is a combination of `map` and `partial`:

~~~clojure
(->> items
     (map (partial process-item context current-time))
     (filter (partial check-item event-type)))
~~~

`Partial` is sometimes good for declarations where it's isolated from the outer world:

~~~clojure
(def enumerate
  (partial map-indexed vector))

(enumerate [:a :b :c])
;; ([0 :a] [1 :b] [2 :c])
~~~

`Comp` is also hard to read as the order of functions is opposite: not left to right but right to left:

~~~clojure
((comp str inc abs) -3)
;; 4
~~~

Like partial, `comp` is good when it's hidden from the outer code. The in-place use of `comp` is horrible, please don't do that:

~~~clojure
(map (comp this that more) [...])
~~~

A good example of `comp` usage where it's isolated:

~~~clojure
(defn make-xform [...]
  (comp (map ...)
        (filter ...)))

(let [xform (make-xform)]
  ...)
~~~

## Arguments

For optional arguments, use a single map but not a list. Not like in the following example:

~~~clojure
(defn connect [host port & {:keys [log-level
                                   log-file]}]
  ...)

(connect "localhost" 5432 :log-level "info" :log-file "log.txt")
~~~

But like in this one:

~~~clojure
(defn connect
  ([host port]
   (connect host port nil))

  ([host port {:keys [log-level log-file]}]
   ...))

(let [options {...}]
  (connect host port options))
~~~

The reason is, to collect the options you need a map. Then you turn it into a flat list and apply it to the function. The underlying function builds a map from that list to parse values, then turns it into a map and applies it to the third function. Eventually, you find the code full of `apply`s rather than normal function calls.

Since Clojure 1.11 or something both types of functions work with a map, but honestly, it's a compromise to end the stand between the two approaches. Someone may still want to use your code with Clojure below 1.11, so it's better to not rely on this automatic solver.

## Destructuring

When destructuring a map on variables, don't go deeper than one level at once. This code is OK:

~~~clojure
(def response
  {:status 200
   :headers {,,,}
   :body {:user {:email "test@test.com"
                 :full-name "Ivan"}}})

(let [{:keys [status body]}
      response

      {:keys [user]}
      body

      {:keys [email full-name]}
      user]

  (println email full-name))
~~~

But this is ambiguous:

{% raw %}
~~~clojure
(let [{{{:keys [email full-name]} :user} :body}
      response]
  (println email full-name))
~~~
{% endraw %}

The problem with map destructuring syntax is, it works from the right to the left which conflicts with the ordinary way of reading. Doing it by one level takes longer, but the code is more readable.

Don't use keywords inside the `:keys` vector. Technically they're allowed, but look weird:

~~~clojure
(let [{:keys [:status :body]}
      response]
  ...)
~~~

## Keywords

This section is the most controversial in this document as it conflicts with what most Clojure developers do.

First, avoid using namespaces in keywords. Instead of `:user/name` or `:book/title` use :`user-name` and `:book-title`. Here is why.

**1.** Imagine you have a map like this:

~~~clojure
(def data
  {:user/name "Ivan"
   :event/name "Party"})
~~~

If you transform this map into JSON with Cheshire, you'll get the keys `user/name` and `event/name`. That's difficult to work with such keys in JavaScript on the client side. In wider terms, your clients can handle namespaces only if they use Clojure or ClojureScript. Any other language like Python has problems with processing keys like `user/name`. There is no an easy way to split such a map on variables with the standard destructuring syntax.

**2.** When using namespaces, you never know for sure what is the right key: `:name` or `:user/name`. That's especially annoying when working with JDBC.next result. By default, it adds namespaces to the selected keys which takes an extra query. For performance, we often pass the `rs/as-unqualified-kebab-maps` parameter to skip the namespaces. But when you edit someone else's code, you've got to scroll up and check what row function was passed to the query. That really slows down the development.

**3.** Maps with namespaces are hard to destructure. Imagine from the map mentioned above, we need to fetch both name of a user and a name of the party. Since the name parts are the same, we cannot use the `:<ns>/keys` syntax as the second clause shadows the first one:

~~~clojure
(let [{:user/keys [name]
       :event/keys [name]} data]
  (println name))
;; Party
~~~

We have to destructure manually, which is boring:

~~~clojure
(let [{user-name :user/name
       event-name :event/name} data]
  (println user-name "@" event-name))
;; Ivan @ Party
~~~

Ideally, a map should be free from namespaces because usually, it's easy to guess the semantics from the context. Let's consider some examples below:

**1.** All the keys have the same namespace:

~~~clojure
(let [user
      {:user/name "Ivan"
       :user/email "test@test.com"
       :user/dob "1985-12-31"
       :user/active? true}]
  ...)
~~~

What is the point to put the same namespace everywhere? The following writing is much simpler and clear:

~~~clojure
(let [user
      {:name "Ivan"
       :email "test@test.com"
       :dob "1985-12-31"
       :active? true}]
  ...)
~~~

Of course, it's possible to shorten it with the `#:<ns>{...}` syntax:

~~~clojure
(let [user #:user{:name "Ivan"
                  :email "test@test.com"
                  :dob "1985-12-31"
                  :active? true}]
  ...)
~~~

Which, anyway, leads do something like this:

~~~clojure
(get :user/email user)
~~~

Since I already know it's a user, why should I use `:user/email` instead of just `:email`?

**2.** There are two and more namespaces in a single map:

~~~clojure
(let [row
      {:user/name "Ivan"
       :user/email "test@test.con"
       :user/dob "1985-12-31"
       :user/active? true
       :profile/user-id 1
       :profile/created-at "2022-01-01"
       :profile/avatar "image.png"}]
  ..)
~~~

Either rewrite it with prefixes:

~~~clojure
(let [row
      {:user-name "Ivan"
       :user-email "test@test.com"
       :user-dob "1985-12-31"
       :user-active? true
       :profile-user-id 1
       :profile-created-at "2022-01-01"
       :profile-avatar "image.png"}]
  ...)
~~~

or group the keys like this:

~~~clojure
(let [row
      {:user {:name "Ivan"
              :email "test@test.con"
              :dob "1985-12-31"
              :active? true}
       :profile {:user-id 1
                 :created-at "2022-01-01"
                 :avatar "image.png"}}]
  ...)
~~~

In both cases, it is much easier to process that map in any way you like. The namespaces would only complicate the process.

Of course, there are systems that rely on namespaces a lot. These are Clojure.spec, Datomic and some Clojure libraries. These are exceptions because their design is built on top of namespaces. But other systems like Postgres, Redis, Cassandra, Kafka and JSON don't need them at all. Don't push namespaces into the areas where they're useless.

## Keyword processing

The second thought about keywords is, never re-process them. By reprocessing, I mean changing the registry and replacing underscores with hyphens. Briefly, always process the data in its original form. Namely, not like this:

~~~clojure
(walk/postwalk
 (fn [x]
   (if (map?)
     (update-keys x ->lower-cebab-case)
     x))
 data)
~~~

But this:

~~~clojure
(let [{:keys [user_id
              user_name]} db-row]
  (println user_id user_name))

(let [{:keys [ResponseError
              ResponseMessage]}
      api-response

      {:keys [Code Category]}
      ResponseError]

  (println Code Category ResponseMessage))
~~~

It's better to get rid of libraries that convert screaming/kabab/whatever keys.

Here is the explanation. First, you waste resources on transforming the keys. Walking through a nested structure was never cheap. So you traverse on it and for each keyword, convert it into a string, match/replace using a regexp and then transform it to a keyword again. It takes CPU time.

Second and much more important: transformed keys do not match the documentation any longer. One day I worked with AppStore API which would return keys in lower camel case, for example, `userName`, `initialTransaction` and so on. So I did:

~~~clojure
(get-in body [:transactionInfo :userName])
~~~

against the JSON response and got nil. Why? Thanks to our HTTP client, it transformed the keys into `:transaction-info` and `:user-name`, and I was unaware of it. That's especially terrible after you've done with a `curl` and `jq` session and now moving to Clojure.

That's completely fine to use `transactionInfo` and `userName` keys in your code. The naming signals they came from the outer world. Please don't transform keywords back and forth when reaching the database, Kafka or whatever else. All you do is waste CPU time and confuse programmers.

## Type hints

Always enable reflection warnings by setting the `*warn-on-reflection*` global variable to true. This can be easily done with lein:

~~~clojure
{:global-vars {*warn-on-reflection* true
               *assert* true
               *print-meta* false}}
~~~

Alternatively, put that map into your local profile to enable warnings in all your Clojure projects:

~~~clojure
# ~/.lein/profiles.clj
{:user
 {:global-vars {*warn-on-reflection* true
                *assert* true
                *print-meta* false}}}
~~~

Reflections significantly slow down the code. Every time you see a warning, put a type hint even if it's out of the scope of the current task.

When building an uberjar, set warnings to true as well and redirect the output into a file. Then `grep` it for "Reflection warning" and terminate the pipeline if anything is found.

~~~bash
lein uberjar > uberjar.log
! grep -i 'Reflection warning' uberjar.log
~~~

## Naming

A common rule of naming a function with side effects is to add an exclamation mark at the end:

~~~clojure
(defn upsert-user! [db fields]
  ...)
~~~

In fact, it depends on the context. The mark by itself is needed to highlight something special among the ordinary things. Thus, if there are plenty of functions that change something, don't use ! in the end, otherwise, the code becomes too noisy. When you highlight everything, nothing in fact is highlighted. The code below is completely fine:

~~~clojure
(defn create-user [db fields]
  ...)

(defn delete-user [db fields]
  ...)

(defn update-user [db fields]
  ...)
~~~

Keep in mind that the exclamation mark is not a part of the word. When selecting such a function by calling `M-x mark-word` or double-clicking on it, the "!" character stays out of the selection and you have to press some extra buttons to include it.

The name of a function ideally starts with a verb: `get-`, `set-`, `process-`, `make-`, and so on:

~~~clojure
(defn get-last-task [db client-id]
  ...)
~~~

Another good pattern is to use `what->what` naming where the first `what` is the input data and the second one is the result. For example:

~~~clojure
(defn orders->total [orders]
  ...)

(defn datetime->date [dt]
  ...)
~~~

It is only a case for a function that takes a single argument. If there are two and more, don't use the following naming:

~~~clojure
(defn orders+clients->total [orders clients]
  ...)
~~~

For the reader, it looks machine-generated and thus weird.

A question mark at the end is OK for predicates or boolean values:

~~~clojure
(defn is-active? [user]
  ...)

(let [active? (is-active? user)]
  ...)
~~~

Don't put `?` in front of the name as it is confusing and mixes with Datalog:

~~~clojure
(let [?active (is-active? user)]
  ...)
~~~

As far as you can, provide hints on the function arguments. Don't use common words like data or similar. In fact, everything is data, so naming this way is useless.

~~~clojure
(defn process-stats [data]
  ...)
~~~

Add plural "s" at the end of vectors or sequences:

~~~clojure
(defn process-stats [orders users]
  ...)

(process-stats [{:order-id 1 ...} {:order-id 2 ...}]
               [{:user-id 10 ...} {:user-id 20 ...}])
~~~

Names like `id->user` is a good choice for maps:

~~~clojure
(defn process-users [id->user]
  ...)

(process-users {1 {:user-id 1 ...},
                2 {:user-id 2 ...}})
~~~

It also applies to the nested maps, for example:

~~~clojure
(def verb->path->response
  {:get {"/" {:status 200 :body ...}
         "/help" {:status 200 :body ...}}
   :post {"/users" {:status 400 :body ...}}})

(with-http-server verb->path->response
  ...)
~~~

If a function accepts a function, name it with the `fn-` prefix:

~~~clojure
(defn error-handler [e]
  ...)

(connect db {:fn-on-error error-handler})
~~~

Some arguments might have a prefix like `map-`, `coll-`, `int-`, `str-` to stress the type. Use them wisely: don't blindly add prefixes to all the vars and arguments because otherwise, the code becomes too noisy:

~~~clojure
(defn process-events [vec-events int-limit str-notice]
  ...)
~~~

Type hints and pre- conditions also help to understand what's behind an argument:

~~~clojure
(defn process-events [events ^Integer limit ^String notice]
  ...)

(defn process-events [events limit notice]
  {:pre [(vector? events) (int? limit) (string? notice)]}
  ...)
~~~

Don't use one-letter naming for the "obvious" — as you might think — cases. If you know that "p" is for the profile and "u" is for the user, it doesn't mean everyone is aware. Use "profile" and "user":

~~~clojure
;; so-so
(defn process-user [u p]
  ...)

;; better
(defn process-user [user profile]
  ...)
~~~

The core Clojure namespace uses its own naming rules. For example, `f` is for a function, `m` is for a map, `k` is for a key and so on. This is not an excuse for using the same way in your code. Leave the clojure.core namespace alone and use more sensible names.

In let, never shadow the `clojure.core` stuff. It's quite common when a map has a key `:name`, and you shadow the `name` function:

~~~clojure
(def user {:name "Ivan"})

(let [{:keys [name]} user]
  (println name))
~~~

The same applies to `key`, `val`, `namespace` and similar functions. One day the field would change to `:full-name` but you would forget to fix the underlying code as it compiles with no errors. It will provide something weird or crash:

~~~clojure
(let [{:keys [full-name]} user]
  (println name))
;; #function[clojure.core/name]
~~~

As a result, to destructure the name of the entity, do it manually:

~~~clojure
(let [{user-name :name} user]
  (println name))
~~~

## Lines and indentation

Most of the time, use two spaces for indentation in your code. If you use Emacs and Cider, there is nothing to worry about: everything is held by the standard settings. Just press RET and TAB and the code is aligned automatically:

~~~clojure
(defn some-function [a b]
  (with-some-macro {:foo 42}
    (let [x (+ a b)]
      (dotimes [_ 99]
        (println "hello")))))
~~~

When splitting the arguments on multiple lines, keep the full indentation like this:

~~~clojure
(calling-a-func-with-args "arg one"
                          {:some {:nested "map"}}
                          some-variable
                          [1 2 3]
                          true)
~~~

but not this:

~~~clojure
(calling-a-func-with-args "arg one"
  {:some {:nested "map"}}
  some-variable
  [1 2 3]
  true)
~~~

Empty lines in code are crucial. Too often, the code is hard to read just because the whole logic collapses into a huge dump of text. Empty lines play the same role that paragraphs do in text. Imagine a book without a single paragraph: an endless monolithic feed of characters impossible to read. Paragraphs give your brain a moment to take a breath before proceed to the next thought. Empty lines in code act the same: they give your readers some rest before moving to the next step.

Use empty lines to split logical parts. For example, you have a long `let` binding and then you compute something. Add an empty line between the binding vector and the body:

~~~clojure
(defn some-long-function [a b c d]
  (let [calc-this
        (some-vast-function a c)

        calc-that
        (some-another-function c d)

        users
        (some-massive-query-to-db calc-that)

        events
        (some-http-request ...)]
                                  ;; <-
    (for [user users]
      ...)))
~~~

When a function has multiple forms, again, separate them with an empty line. Otherwise, they're quite difficult to read. Compare these two forms:

~~~clojure
(defn error!
  ([message]
   (throw (ex-info message {})))
  ([message data]
   (throw (ex-info message data)))
  ([message data cause]
   (throw (ex-info message data cause))))

;; vs

(defn error!

  ([message]
   (throw (ex-info message {})))

  ([message data]
   (throw (ex-info message data)))

  ([message data cause]
   (throw (ex-info message data cause))))
~~~

Adding a line after the arguments makes them clearer especially when there are a docstring, pre- and post- checks, meta and so on. All of that must be separated from the body:

~~~clojure
(defn some-complex-func
  "This function does this and that..."
  [users limit some-arg]
  {:pre [(seq users) (int? limit)]
   :post [(map? %)]}
                     ;; <-
  (let [events
        (get-events ...)]
    ...))
~~~

The same applies to `for`, `doseq` and other forms. Adding just one extra line makes them much more readable:

~~~clojure
(doseq [item items
        :let [{:keys [id title]} item]
        :when (some? id)]
                           ;; <-
  (process-item ...))
~~~

Separate top-level def and defn forms with two empty lines. That really helps one's eyes to navigate through them.

~~~clojure
(defn create-user []
  ...)
                     ;; 1
                     ;; 2
(defn udpate-user []
  ...)
                     ;; 1
                     ;; 2
(defn get-user []
  ...)
~~~


But inside a definition, use only a single line, not two:

~~~clojure
(defn some-func [arg1 arg2]
  (let [x 1
        y 2]
                  ;; that's
                  ;; too much space
    (println ...)))
~~~


Empty lines look odd in GitHub's web interface because of CSS. The distance between the lines is higher than most desktop editors have by default. Be aware of this: code that looks good in web might look bad in the editor.

## Maps

Some developers align maps this way:

~~~clojure
{:name    "John Smith"
 :active? true
 :email   "john@test.com"}
~~~

Editors do that manually, for example, Emacs fixes the indentation within the `M-x clojure-align` command.

Personally, I see a problem with this approach. Too often, when a map consists of short keys, suddenly here comes a long one and vice versa: a map with long keys obtains a short one. Now if you align an updated map you'll get:

~~~clojure
{:name             "John Smith"
 :id               9
 :active?          true
 :email            "john@test.com"
 :number-of-orders 232}
~~~

These gaps look ugly to me. What I usually prefer is to reorder a map manually. I put short keys on top of it and long ones below.

~~~clojure
{:id 9
 :name "John Smith"
 :email "john@test.com"
 :active? true
 :number-of-orders 232}
~~~

Another trick is to keep the longest keys apart from normal ones, then align each group:

~~~clojure
{:id      9
 :name    "John Smith"
 :email   "john@test.com"
 :active? true

 :another-long-field {...}
 :number-of-orders   232}
~~~

## Let

The `let` macro is special in Clojure. It's the most used form in general. Most often, a function consists of a single `let` form where you prepare something and then compose a final result.

What is important about `let`, the way you format it really affects the whole codebase. Thus, I've come up with some rules about `let` which I consider highly important.

`Let` suffers from the same problem we mentioned about maps. If you don't alight key-value pairs, they become hard to read:

~~~clojure
(let [id (:id item)
      accounts-to-delete (jdbc/query db ["select * from accounts where something" 42])
      profiles (rest/get-pending-profiles api "/api/v1/profiles/")]
  ...)
~~~

But if you align them, ugly gaps appear:

~~~clojure
(let [id                 (:id item)
      accounts-to-delete (jdbc/query db ["select * from accounts where something" 42])
      profiles           (rest/get-pending-profiles api "/api/v1/profiles/")]
  ...)
~~~

Both writings are difficult to read. Thus, put the value on the next line after the name and separate pairs with an empty line:

~~~clojure
(let [id
      (:id item)

      accounts-to-delete
      (jdbc/query db ["select * from accounts where something" 42])

      profiles
      (rest/get-pending-profiles api "/api/v1/profiles/")]

  (process-all-of-that id
                       accounts-to-delete
                       profiles))
~~~

This syntax, although looks strange at first glance, proves the best traits through time. It's clear and easy to read as the items are separated. It's free from gaps. It grows down but not to the right. It's always easy to extend it.

## Case, cond

The same syntax applies to the `case` and `cond` forms. Since they accept key-value pairs, put the second item under the first and separate with an empty line:

~~~clojure
(cond
  (check-this? ...)
  (process-that ...)

  (now-check-that? ...)
  (let [a 1]
    (with-transaction [tx db]
      (jdbc/execute tx ...)))

  (one-more-case? ...)
  (something ...)

  :else
  (default-case ...))
~~~

Now compare it to the standard way of writing: which is easier to read?

~~~clojure
(cond
  (check-this? ...) (process-that ...)
  (now-check-that? ...) (let [a 1]
                          (with-transaction [tx db]
                            (jdbc/execute tx ...)))
  (one-more-case? ...) (something ...)
  :else (default-case ...))
~~~

## Macros indentation

When writing a macro, keep in mind whether it is mostly used with the first threading operator (`->`) or not. Sometimes, it affects the styling/indent parameter. For example, you made a then macro to pipe a value through a set of forms. This macro takes a previous form, a binding symbol and an arbitrary body:

~~~clojure
(defmacro then
  [value [bind] & body]
  `(let [~bind ~value]
     ~@body))
~~~

A quick example:

~~~clojure
(-> 1
    (then [x] (inc x))
    (then [x] (* x x))
    (then [x] (println x) x)) => 4
~~~

Since the macro takes two parameters, most likely you specify `{:style/indent 2}` as follows:

~~~clojure
(defmacro then
  {:style/indent 2}
  ...)
~~~

It works correctly when the macro is called without any other macro:

~~~clojure
(then 1 [x]
  (inc x))
~~~

But with `->`, the indentation fails because the macro gets one parameter:

~~~clojure
(-> 1
    (then [x]
        (inc x))
    (then [x]
        (* x x)))
~~~

What you need to do is to set `{:style/indent 1}` for the macro. With this change, the automatic indentation looks correct:

~~~clojure
(-> 1
    (then [x]
      (inc x))
    (then [x]
      (* x x)))
~~~

## Java interop

Try to keep Java interop in a separate namespace. When there is too much interop, the code becomes noisy and "Javish". Provide clean and Clojure-friendly API to cooperate with Java stuff. For example, a dedicated namespace for codecs (Base64 encode, decode), cryptography (ordinary hashes and Hash-HMAC), dates and so on.

~~~clojure
(ns project.codec
  (:import
   java.text.Normalizer
   java.text.Normalizer$Form
   java.util.Base64))

(defn b64-decode ^bytes [^bytes input]
  (.decode (Base64/getDecoder) input))

(defn b64-encode ^bytes [^bytes input]
  (.encode (Base64/getEncoder) input))

(defn normalize-nfc [^String string]
  (Normalizer/normalize string Normalizer$Form/NFC))
~~~

Then:

~~~clojure
(ns project.core
  (:require
   [project.codec :as codec]))

(codec/b64-decode (some-bytes ...))
~~~

One day you can improve this namespace by introducing ClojureScript support on top of other underlying classes.

## Java-like classes

Everyone who ever worked on vast Clojure projects is familiar with something called "map hell". This is when a function accepts three or four maps and you have no idea what is inside them. Although tests and REPL might help, still it's a challenge to get on with such code. Maps, maps are everywhere (buzz-lighter.jpeg).

If you're tired of maps, try the Java approach: classes. Conseal maps in a `deftype` instance and provide a protocol to access their fields. In one project I had three maps which completed each other and acted like a source of truth for something. It was really a mess to get one piece of data from map1, then fetch the second piece from map2 and so on. Instead, I made an interface:

~~~clojure
(defprotocol IStorage
  (get-this-field [this])
  (get-another-field [this])
  (get-item-by-idx [this idx]))
~~~

then a type that holds the maps and implements the interface:

~~~clojure
(deftype Storage
  [map1 map2 map3]

  IStorage

  (get-this-field [this]
    (let [chunk1 (get-in map1 [...])
          chunk2 (get-in map2 [:foo (:id chunk1)])
          chunk3 (get-something-from map3)
          ...]
      {:some "result"}))

  (get-item-by-idx [this idx]
    {:id (get-in map1 some-path)
     :title (get-in map2 another-path)}))
~~~

A final step would be to provide a constructor function:

~~~clojure
(defn make-storage [map1 map2 map3]
  (new Storage map1 map2 map3))
~~~

Now that you have all of that, operate on the instance of `Storage` but not the raw maps:

~~~clojure
(let [storage
      (storage/make-storage m1 m2 m3)

      this-field
      (storage/get-this-field storage)

      item
      (storage/get-item-by-idx storage 9)]

  ...)
~~~

Moreover, add the `^Storage` type hint to the argument so everyone knows it's an instance of a class with its own API but not an ordinary Clojure collection. This approach, although looks slightly foreign, really pays off in vast projects.

## Reuse Java classes

Java VM brings plenty of things that have been developed for years. Not using them in favour of writing your own code is usually a bad idea. Often, I found some code written just because a programmer was not aware of existing JVM functionality. Most likely this code is poorly tested and doesn't take into account plenty of corner cases. Here are some examples:

- manual URL parsing and building;
- manual URL encoding and decoding;
- poor IO based on `spit` and `slurp` functions rather than input/output streams and readers/writers;
- lack of knowledge of built-in Java collections (ArrayList, Stack);
- poor processing of byte arrays;

Even if Java is not a language you came from, it's definitely worth investing your time in it just to write better Clojure code.

## Systems and Components

To manage a global state with various data sources, cron jobs, background tasks and so on use systems and components. Pick one of three good, well-known libraries: Component, Integrant, or Mount. The first two will be a good option. Mount is a bit questionable as it relies on global variables which brings some difficulties to testing; still, it's better than inventing your own way of state management.

Once you picked a certain library, stick with it to the end. What I mean here, I often see code like this:

~~~clojure
(defn -main [& args]
  (let [config (read-config ...)]
    (initiate-sytem config)
    (start-system)

    (initiate-cronjobs)
    (initiate-something-else)
    (etc ...))))
~~~

Here, a programmer initiates and starts a system, but then he or she also starts some additional background stuff like cronjobs or similar. It doesn't have to be like this. A cronjob must be a component that spawns a scheduler and knows how to stop it. There is no way to reuse the `-main` function in tests because you have no control over the background tasks.

Instead, when everything is held by a system, that's extremely easy to tune it and write tests for it. For example, you just remove the cronjob component from the system as it's useless for testing.

~~~clojure
(defn fixture-system [t]
  (initiate-sytem config {:drop [:cronjob]})
  (start-system)
  (t)
  (stop-system))
~~~

When building a system, be as much declarative as you ever can be. I often see programmers compose a system manually like this:

~~~clojure
(defn init-system [config]
  (component/system-map
   :database (db/make-database (:db config))
   :redis (redis/make-redis (:redis config))
   :sendmail (component/using (sendmail/sendmail
                               (:sendmail config))
                              [:database :redis])
   ...))
~~~

Instead, declare a map where the key of a component relates to its constructor:

~~~clojure
(def component-map
  {:database db/make-database
   :redis    redis/make-redis
   :sendmail sendmail/sendmail})
~~~

Declare a second map which specifies dependencies:

~~~clojure
(def using-map
  {:sendmail [:database :redis]})
~~~

Now that you have these two, write a function that takes a config map and:

- Travers on the first map passing the corresponding config values into the constructors;
- Travers on the second map to supply initiated components with dependencies.

Wrap the result into the `System` class, then start it, and you're fine.

Having a system being run, never reach its components using `get`, `get-in` or similar. This is a gross violation of the system design. Instead, pass only required components into the functions. For the same reason don't store a system in a global variable. That can only be an excuse when listening for SIGTERM or other signals to safely shut down the system. A variable that holds a system must be private.

## Collections

Don't use long `->` and `->>` threading chains. Having more than 3-4 tears, they become difficult to read especially when the types differs.

~~~clojure
(let [what-is-it?
      (-> 42                  ;; long
          db/get-user-by-id   ;; a map
          :password           ;; a string field
          .getBytes           ;; byte array
          codec/b64-encode    ;; byte array
          String.)])          ;; string
~~~

Instead, split the chain on several intermediate steps to make it easier for debugging.

~~~clojure
(defn encode-password ^String [^String password]
  (-> password
      (.getBytes)
      (codec/b64-encode)
      (String.)))

(let [user
      (db/get-user-by-id 42)

      password
      (:password user)

      password-encoded
      (encode-password password)]

  ...)
~~~

The `->>` macro is mostly used for processing collections because `map`, `filter` and similar functions accept a collection at the second argument. The problem with `->>` is, it's read less easily than `->` so use it only when it's completely clear what's happening.

The `->>` macro becomes a mess when used with vast anonymous predicates:

~~~clojure
(->> entity :info :companies
     (filter #(= some-id (:guid %)))
     first)

(->> (get-in event [:some-field :items])
     (map :amount)
     (filter (every-pred (complement nil?) int?))
     (reduce +))

(->> nodes
     (map #(some-function! entity-id user %))
     (doall)
     (some.ns/process! entity-id)
     (process-data! task-id resource-type end-cursor))
~~~

Reading such code takes a while. Either transform a predicate into a top-level function or declare it in `let`.

The general idea of using `->` and `->>` is: chaining stuff by itself doesn't mean clearness of the code. Most of the times, you've got to split the things for better reading and debugging.

Don't use `as->` macro which is a mixture of `->` and `->>`. Use separate `->` and `->>` forms for that.

Prefer `vec` over `(into [] ...)`. It's faster and shorter:

~~~clojure
(vec
 (for [item items]
   ...))
~~~

Don't use a collection as a function because if a collection is a nil, you'll get NPE. The only good example of this might be a hardcoded set for filtering:

~~~clojure
(filter #{:active :inactive} statuses)
~~~

`Loop` and `reduce` are good places for transient collections which work faster. It's quite simple to turn an immutable version of `loop/recur` into the transient one:

~~~clojure
(reduce
 (fn [acc item]
   (conj acc (:field item)))
 []
 items)
~~~

becomes:


~~~clojure
(persistent!
 (reduce
  (fn [acc! item]
    (conj! acc! (:field item)))
  (transient [])
  items))
~~~

And

~~~clojure
(loop [i 0
       acc []]
  (if (= i limit)
    acc
    (recur (inc 0) (conj acc (get-item)))))
~~~

becomes:

~~~clojure
(loop [i 0
       acc! (transient [])]
  (if (= i limit)
    (persistent! acc!)
    (recur (inc 0) (conj! acc! (get-item)))))
~~~

In general, avoid laziness. Although the idea of lazy computation is great, often it's better to get an exception right now rather than in further computations. Thus, prepend `for` macro with `vec`. `Mapv`, `filterv` are also great as they rely on transient vectors:

~~~clojure
(mapv :user-name coll-users)

(vec (for [item items]
       (get-something ...)))
~~~

Obviously, this is not the case for infinite collections or collections that fetch their data from network. Turning them into vectors would occupy too many resources or saturate bandwidth.

~~~clojure
;; never vec/doall this
(for [file (s3/get-files-seq ...)]
  (process-file file))
~~~

## Clojure.spec

[orchestra]: https://github.com/jeaye/orchestra

Writing function definitions with `fdef` is a good habit. Using the [Orchestra library][orchestra] you can enable instrumentation for the whole project while testing it. The only thing that deserves to be mentioned here is to keep the specs in a separate file. When functions mix with their `fdef`s, the code becomes noisy and difficult to read.

~~~clojure
(s/fdef process-user
  :args (s/cat :user ::user
               :profile ::profile
               :flag? boolean?)
  :ret map?)

(defn process-user [user profile flag?]
  ...)
~~~

## Types & Records

Always provide a constructor for a `deftype` or `defrecord` definition. It must be a function with a docstring that accepts only the required slots and initiates a class. Without a constructor that's unclear how to initiate it, what slots are required and so on.

~~~clojure
(defrecord SomeComponent
    [host port state on-error])

(defn make-component [host port & {:keys [on-error]}]
  (map->SomeComponent {:host host
                       :port port
                       :on-error on-error}))
~~~

When declaring a component with `defrecord`, divide its slots into three groups: initial arguments, the inner state and dependencies. Use comments to split them off:

~~~clojure
(defrecord SomeComponent
    [;; init
     host port on-error

     ;; runtime
     state

     ;; deps
     cache db])
~~~

Without grouping, it's difficult to guess what is what.

## Named sections

Avoid using wide commented sections in your file like the following below:

~~~
;; --------------------------
;; -------- Handlers --------
;; --------------------------
~~~

or

~~~
;; <><><><><><><><><><><><><>
;; <><><><> Routes <><><><><>
;; <><><><><><><><><><><><><>
~~~

They consume space but do nothing. The very presence of such sections proves the file should have been split on two or three. For example, `routing.clj`, `handlers.clj`, `server.clj` and so on. People who put sections will leave the project one day. Other programmers most likely will push the new stuff to the end of a file skipping all that sectioning.

Needless to say, non-ASCII characters like check marks, fat dots and similar are strictly prohibited in sections as they attract too much attention.

## Comments

Comments are fine when used thriftily. Consider a comment as the last resort to deliver your intentions to the reader. In rare cases, the logic is complicated and full of tricks indeed so you have to leave a hint for other developers. It's quite annoying to realize that a previous developer knew something extraordinary about the logic but didn't let you know.

Long comments are another extreme. They rot in time and no one reads them. Don't pollute the code with long explanations of why it made such and such nor what must be done next. Create an issue or a document and dump all your mind there, but not in the code. A link to that document or its short name like "FOO-123" is enough.

## Trailing spaces

Your code must be free from trailing spaces. If you use Emacs, add the following code to the config file:

~~~clojure
(add-hook 'before-save-hook 'delete-trailing-whitespace)
~~~

It drops the trailing spaces every time you save a file.

## Commented code and dev sections

There cannot be an excuse for having commented lines in your code:

~~~clojure
(defn some-function [a b c]
  ...)

;; The old version of that function
;; (defn some-function [a b c d]
;;   ...)
~~~

Whenever you see it, drop it immediately. People who're interested will find it in the git history. The only exception is a dev section at the bottom of a file wrapped with a `comment` macro:

~~~clojure
(comment

  (def -db
    (jdbc/connect "localhost" 5432))

  (def -row
    (jdbc/execute -db "select 1")))
~~~

This code, though never compiled, still can be evaluated in REPL. Having long dev sections is controversial: they become stale in time, they rely on personal credentials and so on. Keep them short.

## Tests

The standard `clojure.test` framework is good enough for all kinds of testing: unit, integration, smoke and so on. There hardly can be a reason to drive the tests with another third-party library. Often, people just don't know how to organize their tests properly. Invest your time in `clojure.test`, especially fixtures.

Name your tests with a `test-` prefix to distinguish them from ordinary functions in the table of contents of a file (when using `imenu` or similar in Emacs):

~~~clojure
(deftest test-user-auth-ok
  ...)

(deftest test-user-auth-fails
  ...)
~~~

The same about fixtures: use the `fix-` or `fixture-` prefix to stress this is a fixture but not a normal function.

~~~clojure
(defn fixture-prepare-db [t]
  (insert-the-data *db*)
  (t)
  (delete-the-data *db*))
~~~

Tests must be placed in a separate directory, usually test in the root of a project. Don't follow the Python approach when each module has a test one in the same directory:

~~~
user.clj
user_test.clj
order.clj
order_test.clj
~~~

Tagging tests is a good practice to optimize the CI pipeline. For example, first you run unit tests that don't require an environment:

~~~bash
lein test :unit
# or
lein test # default selector
~~~

If everything went fine, bootstrap Docker and run integration tests.

~~~
docker-compose up -d
lein test :integration
~~~

Here is how selectors look in code:

~~~clojure
(deftest ^:unit test-some-pure-function
  ...)

(deftest ^:integration test-some-db-logic
  ...)
~~~

A hint: instead of marking each test in a namespace, mark just the namespace so its tags are applied to each test:

~~~clojure
(ns ^:unit project.pure-function-tests
  ...)

(ns ^:integration project.system-test
  ...)
~~~

Avoid using `with-redefs` macro in the tests as most likely it indicates problems. The less you monkey-patch in a test session, the better and more stable your code is. If the code you're testing needs a file, make that file in a fixture. If it needs a database, MQ, Kafka, cache, or whatever — run it in docker-compose and aim the settings to "localhost". If a test reaches some HTTP API, make a fixture that runs a local Ring server that serves JSON from a file.

~~~clojure
(defmacro with-local-http
  [[port verb->path->response] & body]
  `(let [handler#
         (fn [request]
           (get-response-from-mapping))

         server#
         (jetty/run-jetty handler#)]

     (try
       ~@body
       (finally
         (.close server#)))))

(deftest test-some-api
  (with-local-http [8080 {:get {"/v1/users" {...}}}]
    (run-function-that-calls-the-api ...)))
~~~

All of these are much better than a dull usage of `with-redefs`. It gives you only a vision that the tests have passed whereas local services in Docker prove it.

## Core.async

Before introducing `clojure.core.async` to the project, first ensure you've tried simpler solutions like `agent`s, `pmap`, thread pool executors and so on. Bringing async to the scene changes the paradigm of data processing so delay this step while it's possible.

Core.async should never be a part of a library. If the library processes messages, let your consumers decide which bus type to use. Use dependency injection pattern: your code relies on an instance of `IBus` protocol with `send-` and `get-message` abstract methods. Then provide a sub-library which extends the protocol with `core.async`. Some of your clients may use `manifold`, Kafka or whatever they want for the bus rather than `core.async`.

## Amazonica

This library is widely used to reach the AWS cloud. When adding it to the project, always specify directly what subparts of SDK you need. Otherwise, about 100 jars (!) will be downloaded to your machine. It would also happen every time you build the project on CI, so be careful with Amazonica dependencies.

~~~clojure
{:dependencies [[amazonica "0.3.156"
                :exclusions [com.amazonaws/aws-java-sdk
                             com.amazonaws/amazon-kinesis-client
                             com.amazonaws/dynamodb-streams-kinesis-adapter]]
               [com.amazonaws/aws-java-sdk-core "1.11.968"]
               [com.amazonaws/aws-java-sdk-s3 "1.11.968"]]}
~~~

Although the readme file of Amazonica mentions that case, it's placed almost at the end of the file so no one reaches it. That's sad.

## Libraries

Whenever you start a new project, use the standard, well-known libraries. These are Ring, HTTP-kip, JDBC.next, Migratus and so on. Don't invent your own routing, ORM or template system, encryption library or whatever else. If a company develops their own framework, one day they must publish it and let the community decide whether it's useful or not.
