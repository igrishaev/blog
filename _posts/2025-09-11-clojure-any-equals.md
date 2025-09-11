---
layout: post
title: "A fake Clojure Object equals to what you want"
permalink: /clojure-any-equals/
tags: programming clojure tests
telegram_id:
---

Imagine you write a unit test where you compare two maps:

~~~clojure
(is (= {:some {:nested {:id ...}}}
       (get-result)))
~~~

Turns out, this map has a random UUID deep inside so you cannot blindly compare
them with the "equals" function:

~~~clojure
(defn get-result []
  {:some {:nested {:id (random-uuid)}}})

(is (= {:some {:nested {:id ???}}}
       (get-result)))
~~~

This won't work because the nested `:id` field will is random every time.

What to do? Most often, people use libraries for fuzzy matching, DSLs,
etc. Well, a single case still doesn't mean you should drag in another
library. Apparently, it could be solved with a dummy object that equals to any
UUID:

~~~clojure
(def any-uuid
  (reify Object
    (equals [_ other]
      (uuid? other))))

(= any-uuid (random-uuid))
true

(= any-uuid 42)
false
~~~

Now replace the value in your map, and the test will pass:

~~~clojure
(is (= {:some {:nested {:id any-uuid}}}
       (get-result)))
~~~

It works the same for numbers:

~~~clojure
(def any-number
  (reify Object
    (equals [_ other]
      (number? other))))

(= any-number 42)
true

(= any-number -99)
true
~~~

The only caveat is, this dummy object must be the first one in the `=`
function. It does equal to any object on the left but the opposite is false: a
normal UUID doesn't equal to a fake UUID.

For the rest, it short and trivial, and no other libraries are needed.
