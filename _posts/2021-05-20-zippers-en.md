---
layout: post
title:  "Clojure Zippers"
permalink: /en/clojure-zippers/
tags: clojure programming zippers
lang: en
---

{% include toc.html id="clojure-zippers-en" title="" %}

## Part 1. The Basics of Navigation

*In this article, we will discuss zippers in the Clojure language. These are an unusual way
to work with collections. Using a zipper, you can traverse a data structure arbitrarily and modify its content as well as search in it. A zipper is a powerful abstraction that pays off over time. However, it is not as straightforward as regular tools and requires training to deal with.*

Let's talk about a zipper in simple terms. It is a wrapper that offers a variety of data manipulations. Let's list the main ones:

- moving vertically: down to children or up to a parent;
- moving horizontally: left or right among children;
- traversal of the entire data structure;
- adding, editing and deleting nodes.

<!-- more -->

This is a partial list, and you will see the most interesting solutions more later. Note: these capabilities are available when working with arbitrary data, whether it's a combination of vectors and maps, XML, or a tree. This makes zippers a powerful tool. If you figure out how to handle them, you will boost your skills and open new doors.

The good news is that zippers are available in the base Clojure package. It's better than a third party library that needs to be included. Zippers are easy to add to a project with no fear of license issues or new dependencies.

Clojure zippers harness the power of immutable collections. Technically, a zipper is a collection that stores data and the position of the pointer. Together they are called a location. A step in either direction returns a new location, just like the `assoc` or `update` functions generate new data from old data.

From the current location, you can get a node,  that is, a piece of data that the pointer refers to. Let's clarify their difference to avoid confusing beginners. Location is the source data and the position in it. Moving around the location generates a new location. From the location, you can retrieve a node — the data that is in this area.

Below is an example with the vector `[1 2 3]`. To move to the second item, the **two**, you need to wrap the data in a zipper and execute the `zip/down` and` zip/right` commands. In the first step, we'll get into the vector and find ourselves on element 1. A step to the right will move us to 2. Let's express it in code: include the package with the alias `zip` and traverse the vector.

~~~clojure
(require '[clojure.zip :as zip])

(-> [1 2 3]
    zip/vector-zip
    zip/down
    zip/right
    zip/node)
;; 2
~~~

Chaining these functions will return 2 as expected. The last action — `zip/node` — outputs the value (a node) from the current location. If we remove `zip/node`, we'll get a location that corresponds to 2. It looks like this:

~~~clojure
(-> [1 2 3]
    zip/vector-zip
    zip/down
    zip/right)

;; [2 {:l [1], :pnodes [[1 2 3]], :ppath nil, :r (3)}]
~~~

Maybe you have some questions: how do we know the path to the 2 when it could have been elsewhere in the vector? What happens if we go outside the collection?
You'll find the answers to these questions below. For now, if something is not clear to you, do not
panic: we'll clarify more than once everything happening here.

So, the zipper suggests navigating through the data. Despite its power, it doesn't know how to do
this for a specific collection, so you need to teach it. In addition to data, a zipper requires answers to two questions:

- Is the current element a branch? This is the name of the element from which
you can get other ones.

- If it's a branch, how do you fetch children from it?

That's all a zipper needs to know to navigate. Note, for changing the zipper itself, you need
to know the answer to one more question — how to attach children to a branch. However, we are only looking at navigation, so the third question can wait.

Technically, functions give the answers to the first and second questions. The first one takes a node and returns `true` or `false`. If it returns `true`, the zipper calls the second function. It takes the same node but should return a sequence of child nodes or `nil` if they don't exist. In code, these functions are called `branch?` and `children`.

To get a zipper, you need to tell it input data and the two functions just described. As long as we only read a zipper, the third function can be `nil`. The zippers locate in the `clojure.zip` package. Include it into namespace:

~~~clojure
(ns my.project
  (:require [clojure.zip :as zip]))
~~~

[zip-src]: https://github.com/clojure/clojure/blob/master/src/clj/clojure/zip.clj

Explore the source code for this module in your leisure time. It is only 280 lines long!

The `zip/zipper` function creates a zipper from source data and functions. This is the module's main point, its building blocks. For common cases, the module offers some predefined zippers that only expect data. `Vector-zip` for nested vectors is a good example. Here is its code without the third parameter:

~~~clojure
(defn vector-zip
  [root]
  (zipper vector?
          seq
          ...
          root))
~~~

We replaced it with three dots. The third parameter is a function that attaches child nodes to the branch on change (ignore it for now). If you pass the vector `[1 2 3]` to `vector-zip`, the following happens:

The zipper will wrap the vector and expose a pointer to it. From the starting position, you can only traverse down, because at the top a zipper has no parent (up) and neighbors (left and right). When navigating **down**, the zipper first checks if the branch is the current node. That triggers the expression `(vector? [1 2 3])` that gets evaluated to `true`. In this case, the zipper will execute `(seq [1 2 3])` to get children. They will be the sequence `(1 2 3)`. Once the children are found, the zipper will set the pointer to the leftmost child — 1.

Let's show this in the diagram. Start position, a pointer is on the source vector:

{: .asciichart}
~~~
                ┌───────┐
                │  nil  │
                └───────┘
                    ▲
                    │
 ┌───────┐    ┏━━━━━━━━━━━┓    ┌───────┐
 │  nil  │◀───┃  [1 2 3]  ┃───▶│  nil  │
 └───────┘    ┗━━━━━━━━━━━┛    └───────┘
                    │
                    ▼
                ┌───────┐
                │   1   │
                └───────┘
~~~

Step down, the pointer is at 1:

{: .asciichart}
~~~
                ┌───────┐
                │[1 2 3]│
                └───────┘
                    ▲
                    │
 ┌───────┐    ┏━━━━━━━━━━━┓    ┌───────┐
 │  nil  │◀───┃     1     ┃───▶│   2   │
 └───────┘    ┗━━━━━━━━━━━┛    └───────┘
                    │
                    ▼
                ┌───────┐
                │  nil  │
                └───────┘
~~~

Step to the right, the pointer on 2:

{: .asciichart}
~~~
                ┌───────┐
                │[1 2 3]│
                └───────┘
                    ▲
                    │
 ┌───────┐    ┏━━━━━━━━━━━┓    ┌───────┐
 │  nil  │◀───┃     2     ┃───▶│   3   │
 └───────┘    ┗━━━━━━━━━━━┛    └───────┘
                    │
                    ▼
                ┌───────┐
                │  nil  │
                └───────┘
~~~

So, we are on 2 and can move horizontally. A step to the right will move us to 3,  to the left — to 1. In the code, it looks like this:

~~~clojure
(def loc2
  (-> [1 2 3]
      zip/vector-zip
      zip/down
      zip/right))

(-> loc2 zip/node)
;; 2

(-> loc2 zip/right zip/node)
;; 3

(-> loc2 zip/left zip/node)
;; 1
~~~

When trying to move down, the zipper will execute the `(vector? 2)` predicate. The result
will be `false`, which means that the current element is not a branch and no downward movement is allowed.

Remember the following as you traverse. Each step creates a new location without changing the old one. If you save any particular location in a variable, subsequent calls to `zip/right,` `zip/down`, and others will not change it in any way. Above, we have declared the `loc2` variable,
which points to 2. You can use it to get the source vector.

~~~clojure
(-> loc2 zip/up zip/node)
;; [1 2 3]
~~~

If you move along manually, chances are good that you will go outside the collection. A step to nowhere will return `nil` instead of a location:

~~~clojure
(-> [1 2 3]
    zip/vector-zip
    zip/down
    zip/left)
nil
~~~

This is a signal that you are on the wrong route. The bad news is that you cannot go back from `nil`. `Nil` signifies an empty location, and there is no reference to the previous step in it. The `zip/up`, `zip/right` and other functions also return `nil` for an empty location. If you iterate in a cycle and do not take this into account, you’ll just end up spinning your wheels.

~~~clojure
(-> [1 2 3]
zip/vector-zip
zip/down
zip/left
zip/left
zip/left
zip/left)
~~~

The `zip/down` function is an exception: if you try to descend from `nil`, you'll get a `NullPointerException` error. This is a slight defect that probably will be fixed on day.

~~~clojure
(-> [1 2 3]
zip/vector-zip
zip/down
zip/left
zip/down)

;; Execution error (NullPointerException)...
~~~

Let's take a look at a more complex vector. One of its children is another vector — `[1 [2 3] 4]`. To move the pointer to **3**, make the steps `down`, `right`, `down`, and `right`. Let's store a location in a variable:

~~~clojure
(def loc3
  (-> [1 [2 3] 4]
      zip/vector-zip
      zip/down
      zip/right
      zip/down
      zip/right))

(zip/node loc3)
3
~~~

The pictures below show what happens at each step. Starting position:

{: .asciichart}
~~~
                ┌───────┐
                │  nil  │
                └───────┘
                    ▲
                    │
 ┌───────┐    ┏━━━━━━━━━━━┓    ┌───────┐
 │  nil  │◀───┃[1 [2 3] 4]┃───▶│  nil  │
 └───────┘    ┗━━━━━━━━━━━┛    └───────┘
                    │
                    ▼
                ┌───────┐
                │   1   │
                └───────┘
~~~

Step down:

{: .asciichart}
~~~
              ┌───────────┐
              │[1 [2 3] 4]│
              └───────────┘
                    ▲
                    │
 ┌───────┐    ┏━━━━━━━━━━━┓    ┌───────┐
 │  nil  │◀───┃     1     ┃───▶│ [2 3] │
 └───────┘    ┗━━━━━━━━━━━┛    └───────┘
                    │
                    ▼
                ┌───────┐
                │  nil  │
                └───────┘
~~~

To the right:

{: .asciichart}
~~~
              ┌───────────┐
              │[1 [2 3] 4]│
              └───────────┘
                    ▲
                    │
 ┌───────┐    ┏━━━━━━━━━━━┓    ┌───────┐
 │   1   │◀───┃   [2 3]   ┃───▶│   4   │
 └───────┘    ┗━━━━━━━━━━━┛    └───────┘
                    │
                    ▼
                ┌───────┐
                │   2   │
                └───────┘
~~~

Down:

{: .asciichart}
~~~
              ┌───────────┐
              │   [2 3]   │
              └───────────┘
                    ▲
                    │
 ┌───────┐    ┏━━━━━━━━━━━┓    ┌───────┐
 │   1   │◀───┃     2     ┃───▶│   3   │
 └───────┘    ┗━━━━━━━━━━━┛    └───────┘
                    │
                    ▼
                ┌───────┐
                │  nil  │
                └───────┘
~~~

To the right. We are at our goal:

{: .asciichart}
~~~
              ┌───────────┐
              │   [2 3]   │
              └───────────┘
                    ▲
                    │
 ┌───────┐    ┏━━━━━━━━━━━┓    ┌───────┐
 │   2   │◀───┃     3     ┃───▶│  nil  │
 └───────┘    ┗━━━━━━━━━━━┛    └───────┘
                    │
                    ▼
                ┌───────┐
                │  nil  │
                └───────┘
~~~

To move to **4** from the current position, you first need to go up. The pointer will move to vector `[2 3]`. Now we are among the children of the original vector and can move horizontally. Let's take a step to the right and find ourselves at number **4**.

Here the same actions are shown graphically. The current location (i.e., 3):

{: .asciichart}
~~~
              ┌───────────┐
              │   [2 3]   │
              └───────────┘
                    ▲
                    │
 ┌───────┐    ┏━━━━━━━━━━━┓    ┌───────┐
 │   2   │◀───┃     3     ┃───▶│  nil  │
 └───────┘    ┗━━━━━━━━━━━┛    └───────┘
                    │
                    ▼
                ┌───────┐
                │  nil  │
                └───────┘
~~~

Step up:

{: .asciichart}
~~~
              ┌───────────┐
              │[1 [2 3] 4]│
              └───────────┘
                    ▲
                    │
 ┌───────┐    ┏━━━━━━━━━━━┓    ┌───────┐
 │   1   │◀───┃   [2 3]   ┃───▶│   4   │
 └───────┘    ┗━━━━━━━━━━━┛    └───────┘
                    │
                    ▼
                ┌───────┐
                │   2   │
                └───────┘
~~~

Step to the right:

{: .asciichart}
~~~
              ┌───────────┐
              │[1 [2 3] 4]│
              └───────────┘
                    ▲
                    │
 ┌───────┐    ┏━━━━━━━━━━━┓    ┌───────┐
 │ [2 3] │◀───┃     4     ┃───▶│  nil  │
 └───────┘    ┗━━━━━━━━━━━┛    └───────┘
                    │
                    ▼
                ┌───────┐
                │  nil  │
                └───────┘
~~~

The original vector can be of any nesting. As an exercise, replace 3 with another vector and go down into it.

What does happen if you pass something other than a vector to `vector-zip`?  For example, it might be a string, nil, or a number. Before traversing, the zipper checks to see if the node is a branch and if it has child nodes. For `vector-zip`, it checks the data with the `vector?` function, which returns `nil` for all non-vector values. As a result, we get a location from where we can't step anywhere: neither down nor laterally. This dead end must be avoided.

~~~clojure
(-> "test"
    zip/vector-zip
    zip/down)
nil
~~~

The `clojure.zip` module also offers other built-in zippers. The `xml-zip` is especially interesting for navigating XML trees. We'll discuss it separately when you get to know the other zipper features.


## Part 2. Automatic navigation

We figured out how to navigate through the collection. However, you might wonder how the path
goes? How do you know in advance in which direction to go?

The main message of this section is: **Manual navigation through data makes no sense.**
If you know the path beforehand, you don't need a zipper.

For data whose structure you know in advance, Clojure offers an easier way to work with. For example, if we know for sure that the input data structure is a vector, and its second element is another vector, we'll use `get-in`:

~~~clojure
(def data [1 [2 3] 4])

(get-in data [1 1])
;; 3
~~~

The same goes for other data types. It doesn't matter what combination lists and maps make.  If the structure is known in advance, the data you need can be easily reached with a `get-in` or threading macro. In this case, zippers will only complicate the code.

~~~clojure
(def data {:users [{:name "Ivan"}]})

(-> data :users first :name)
;; "Ivan"
~~~

What is the advantage of zippers? Their strengths are manifested in situations where `get-in` can't work. It's about data with an *unknown* structure. Let's say there is an arbitrary vector as input, and you need to find a string in it. For example, it might be at the first nesting level, or at the third, and so on. Another example is an XML document. The required tag can be located anywhere in it, but you need to find it somehow. In short, the ideal situation for a zipper is a fuzzy data structure that we're only guessing about.

Together, the functions `zip/up`, `zip/down`, and others form the universal function — `zip/next`. It
moves the pointer so that sooner or later we'll traverse the entire structure. When traversing, repetitions are excluded: we'll visit each place only once. Here is an example with a vector:

~~~clojure
(def vzip (zip/vector-zip [1 [2 3] 4]))

(-> vzip zip/node)
;; [1 [2 3] 4]

(-> vzip zip/next zip/node)
;; 1

(-> vzip zip/next zip/next zip/node)
;; [2 3]

(-> vzip zip/next zip/next zip/next zip/node)
;; 2
~~~

We don't know how many times to call  `zip/next`, so let's resort to a ploy. The `iterate` function takes the `f` function and an `x` value. It returns a sequence where the first element is `x`, and each next is an `f(x)` from the previous one. For a zipper, we get the initial location, then `zip/next` from it, then `zip/next` from the previous movement, and so on.

Below, the variable `loc-seq` is the location chain of the source zipper. To get the nodes, we take the first six elements (the number we take randomly) and call `zip/node` for each.

~~~clojure
(def loc-seq (iterate zip/next vzip))

(->> loc-seq
     (take 6)
     (map zip/node))

;; ([1 [2 3] 4]
;;   1
;;   [2 3]
;;   2
;;   3
;;   4)
~~~

`Iterate` returns a *lazy* and *infinite* sequence. Both characteristics are important. Laziness means that the next shift (i.e., calling `zip/next`) will not happen until you reach an element in the chain. Infinity means that `zip/next` is called an unlimited number of times. We need a flag to indicate that we need to stop calling `zip/next`, otherwise the stream of locations will never end.

In addition, at some point, `zip/next` stops moving the pointer. Take, for example, the hundredth and thousandth elements of an iteration. Their node will be the initial vector:

~~~clojure
(-> loc-seq (nth 100) zip/node)
;; [1 [2 3] 4]

(-> loc-seq (nth 1000) zip/node)
;; [1 [2 3] 4]
~~~

The reason lies in how the zipper traversal works. The `zip/next` function acts like a ring. When it reaches the initial location, the loop ends. In this case, the location will get a completion flag, and the next calling `zip/next` will return the same location. You can check a flag presence with the `zip/end?` function:

~~~clojure
(def loc-end
  (-> [1 2 3]
      zip/vector-zip
      zip/next
      zip/next
      zip/next
      zip/next))

loc-end
;; [[1 2 3] :end]

(zip/end? loc-end)
~~~

To create the finite chain of locations, we'll keep moving the pointer until we get the last location. Together, this gives the following function:

~~~clojure
(defn iter-zip [zipper]
  (->> zipper
       (iterate zip/next)
       (take-while (complement zip/end?))))
~~~

This function returns all locations in the data structure. Recall that a location stores a node (a data element) that we can get using `zip/node`. The example below shows how to convert locations into data:

~~~clojure
(->> [1 [2 3] 4]
     zip/vector-zip
     iter-zip
     (map zip/node))

;; ([1 [2 3] 4]
;;  1
;;  [2 3]
;;  2
;;  3
;;  4)
~~~

Now we have a chain of locations. Let's write a search. Suppose you want to check if the vector
contains the `:error` keyword. First, let's write a predicate for a location to know whether its node is equal to this value.

~~~clojure
(defn loc-error? [loc]
  (-> loc zip/node (= :error)))
~~~

Well, let's check if there is one in the chain of locations that matches our predicate.
To do this, call `some`:

~~~clojure
(def data [1 [2 3 [:test [:foo :error]]] 4])

(some loc-error?
      (-> data zip/vector-zip iter-zip))

;; true
~~~

Note that due to laziness, we are not scanning the entire tree. If the required node appears in the middle, `iter-zip` ends the iteration and stops making calls, and further `zip/next` calls won't happen.

It's useful to know that `zip/next` traverses a tree in depth-first order. As it moves, it tends to go down or to the right, but up only when steps in these directions return `nil`. As we'll see later, sometimes the traversal order is important. There're tasks where we have to traverse in breadth-first order. There're no other default options for traversal in `clojure.zip`, but we can easily write
our own. We'll look at a task that requires traversal in breadth later.

The built-in `vector-zip` zipper is for nested vectors. But nested maps are much more common. Let's write a zipper to traverse such data:

~~~clojure
(def map-data
  {:foo 1
   :bar 2
   :baz {:test "hello"
         :word {:nested true}}})
~~~

Let's take the familiar vector-zip as a basis. These zippers are similar, the only difference is the collection type they work with. Let's think about how to define functions that answer the questions. The map is a branch whose children are `MapEntry` elements. This type represents a key-value pair. If the value is a map, we get a chain of nested `MapEntry` from it and so on.

To warm up, let's write a predicate for checking the `MapEntry` type:

~~~clojure
(def entry?
  (partial instance? clojure.lang.MapEntry))
~~~

The `map-zip` zipper looks like this:

~~~clojure
(defn map-zip [mapping]
  (zip/zipper
   (some-fn entry? map?)
   (fn [x]
     (cond
       (map? x) (seq x)

       (and (entry? x)
            (-> x val map?))
       (-> x val seq)))
   nil
   mapping))
~~~

Let's discuss the main points. The `(some-fn ...)` composition returns `true` if one of the predicate-parameters works positively. In other words, we consider only the map or its entry (key-value pair) as a branch.

In the second function, which looks for children, we have to check some conditions. If the current value is a map, we return a sequence of map entries using the `seq` function. If we are already in `MapEntry`, then check if the value is a nested map. If it is, we should get its children with the same `seq` function.

When traversing the tree, we'll get all the key-value pairs. If the value is a nested dictionary,
we'll fall into it when traversing. Here is an example:

~~~clojure
(->> {:foo 42
      :bar {:baz 11
            :user/name "Ivan"}}
     map-zip
     iter-zip
     rest
     (map zip/node))

;; ([:foo 42]
;;  [:bar {:baz 11, :user/name "Ivan"}]
;;  [:baz 11]
;;  [:user/name "Ivan"])
~~~

Notice the `rest` function after `iter-zip`. We skipped the first location that contains the original data. Since they are already known, their printing makes no sense.

Using our `map-zip`, we can check if the map contains the `:error` key with the `:auth` value. Each of these keywords can be anywhere, both in keys and in values at any level. However, we are interested in their combination. To do this, let's write a predicate:

~~~clojure
(defn loc-err-auth? [loc]
  (-> loc zip/node (= [:error :auth])))
~~~

Let's make sure that there is no such pair in the first dictionary, even if the values appear separately:

~~~clojure
(->> {:response {:error :expired
                 :auth :failed}}
     map-zip
     iter-zip
     (some loc-err-auth?))

;; nil
~~~

We'll find this pair, even if it is deeply nested:

~~~clojure
(def data
  {:response {:info {:message "Auth error"
                     :error :auth
                     :code 1005}}})

(->> data
     map-zip
     iter-zip
     (some loc-err-auth?))

;; true
~~~

Below are a few tasks for independent work.

**1.** The `map-zip` zipper ignores the situation where the map key is another map.
For example:

{% raw %}
~~~clojure
{{:alg "MD5" :salt "***"} "deprecated"
{:alg "SHA2" :salt "****"} "deprecated"
{:alg "HMAC-SHA256" :key "xxx"} "ok"}
~~~
{% endraw %}

Such collections, although rarely, are used sometimes. Modify `map-zip` so that it checks not only the value of `MapEntry` but also the key.

**2.** In practice, we work with a combination of vectors and maps. Write a universal zipper that takes into account both the map and the vector when traversing.


## Part 3. XML zippers

The power of zippers is fully revealed when working with XML. Unlike other formats, it is specified recursively. For example, JSON, YAML, and other formats offer data types (numbers, strings, collections) with different syntax and structure. In XML, wherever we are, the current node always consists of three components: tag, attributes, and content. Content is a set of strings or other nodes. Here's a recursive pseudocode notation:

~~~
XML = [Tag, Attrs, [String|XML]]
~~~

To make sure the XML is homogeneous, consider an abstract file with vendor items:

~~~xml
<?xml version="1.0" encoding="UTF-8"?>
<catalog>
  <organization name="re-Store">
    <product type="iphone">iPhone 11 Pro</product>
    <product type="iphone">iPhone SE</product>
  </organization>
  <organization name="DNS">
    <product type="tablet">iPad 3</product>
    <product type="notebook">Macbook Pro</product>
  </organization>
</catalog>
~~~

At the top of the XML is the `catalog` node. It's just a grouping tag; we need it because there can't be multiple tags at the top. The `catalog` children are organizations. The `name` attribute of the organization contains its name. Products are under the organization. A product is a node with a `product` tag and a description of the product type. Instead of children, it has text content — its description. It's impossible to go down below a product.

Clojure offers an XML parser that returns a structure similar to the `[Tag, Attrs, Content]` schema above. Each node becomes a map with the keys :tag, :attrs, and `:content`. The `:content` key stores a vector where an element is either a string or a nested map.

We put the XML data with products in the `resources/products.xml` file. Let's write a function to parse a file into an XML zipper. Add module imports:

~~~clojure
(:require
 [clojure.java.io :as io]
 [clojure.xml :as xml])
~~~

Both come with Clojure and therefore do not require dependencies. To get the zipper, we pass the `path` parameter through a series of functions:

~~~clojure
(defn ->xml-zipper [path]
  (-> path
      io/resource
      io/file
      xml/parse
      zip/xml-zip))
~~~

The `xml/parse` function should return a nested structure consisting of maps with keys `: tag`,
`:attrs`, and `:content`. Note that text content such as a product name, is also a vector with one string. This achieves the homogeneity of each node.

This is what we should get after calling `xml/parse`:

~~~clojure
{:tag :catalog
 :attrs nil
 :content
 [{:tag :organization
   :attrs {:name "re-Store"}
   :content
   [{:tag :product
     :attrs {:type "iphone"}
     :content ["iPhone 11 Pro"]}
    {:tag :product :attrs {:type "iphone"} :content ["iPhone SE"]}]}
  {:tag :organization
   :attrs {:name "DNS"}
   :content
   [{:tag :product :attrs {:type "tablet"} :content ["iPad 3"]}
    {:tag :product
     :attrs {:type "notebook"}
     :content ["Macbook Pro"]}]}]}
~~~

The call of `(->xml-zipper "products.xml")` creates the initial location of the XML zipper from the data above. First, let's take a look at the definition of `xml-zip` to understand how it works. Here we present code excerpts:

~~~clojure
(defn xml-zip
  [root]
  (zipper (complement string?)
          (comp seq :content)
          ...
          root))
~~~

As you might guess, the children of the node are its `:content`, additionally wrapped in `seq`. A string can't have children, so `(complement string?)` means — search for children only in non-string nodes.

Look at how we would find all products from a given XML. First, let's get a lazy iteration over its zipper. Recall that at each step we get not a map with  `:tag` and other fields, but a zip location with a pointer to it. It remains only to filter out the locations which nodes contain the product tag. To do this let's write a predicate:

~~~clojure
(defn loc-product? [loc]
  (-> loc zip/node :tag (= :product)))
~~~

And let's write a transforming selection:

~~~clojure
(->> "products.xml"
     ->xml-zipper
     iter-zip
     (filter loc-product?)
     (map loc->product))

;; ("iPhone 11 Pro" "iPhone SE" "iPad 3" "Macbook Pro")
~~~

At first glance, there is nothing special here. The XML structure is known in advance, so we can do it without zipper. Let's select catalog children and get organizations, then we'll get organizations' children (i.e., goods). Here's this simple code:

~~~clojure
(def xml-data
  (-> "products.xml"
      io/resource
      io/file
      xml/parse))

(def orgs
  (:content xml-data))

(def products
  (mapcat :content orgs))

(def product-names
  (mapcat :content products))
~~~

To make the code more concise, you can remove the intermediate variables and narrow it down to one form:

~~~clojure
(->> "products.xml"
     io/resource
     io/file
     xml/parse
     :content
     (mapcat :content)
     (mapcat :content))

;; ("iPhone 11 Pro" "iPhone SE" "iPad 3" "Macbook Pro")
~~~

In practice, the structure of XML always changes. Suppose a super-large dealer breaks down products by branch. In this case, the XML looks like this (a snippet):

~~~xml
<organization name="DNS">
  <branch name="Office 1">
    <product type="tablet">iPad 3</product>
    <product type="notebook">Macbook Pro</product>
  </branch>
  <branch name="Office 2">
    <product type="tablet">iPad 4</product>
    <product type="phone">Samsung A6+</product>
  </branch>
</organization>
~~~

The above code that selected data only by level won't work anymore. If we run it against the new XML, we'll get a branch node along with the products:

~~~clojure
("iPhone 11 Pro"
 "iPhone SE"
 {:tag :product, :attrs {:type "tablet"}, :content ["iPad 3"]} ...)
~~~

If we used a zipper, it would return **only** products, including those from the branch:

~~~clojure
(->> "products-branch.xml"
     ->xml-zipper
     iter-zip
     (filter loc-product?)
     (map loc->product))

("iPhone 11 Pro" "iPhone SE" "iPad 3" "Macbook Pro" "iPad 4" "Samsung A6+")
~~~

Obviously, it's beneficial to use code that works with both XML rather than maintaining a separate version for a large dealer. In the latter case, you have to store the flag, which supplier is normal and which is large, and promptly update it.

However, this example doesn't cover the full capacity of the zippers. The `xml-seq` function from the core Clojure module also provides XML traversal. The function returns a lazy sequence of XML nodes in the same form (a map with `:tag`, `:attr`, and `:content`). `Xml-seq` is a special case of the more abstract `tree-seq` function. The latter is similar to a zipper in that it takes similar functions to determine if a node can be a branch and how to get its children. As you can see from the code, the `xml-seq` and `xml-zip` definitions are similar:

~~~clojure
(defn xml-seq
  [root]
  (tree-seq
    (complement string?)
    (comp seq :content)
    root))
~~~

The difference between a zipper and `tree-seq` is that when iterating, the zipper returns a location — a more abstract and more informative element. Instead, `tree-seq` produces unwrapped elements during iteration. For ordinary searches, `tree-seq` is even preferable, since it doesn't generate unnecessary abstractions. The selection of goods, taking into account branches, looks like this:

~~~clojure
(defn node-product? [node]
  (some-> node :tag (= :product)))

(->> "products-branch.xml"
     io/resource
     io/file
     xml/parse
     xml-seq
     (filter node-product?)
     (mapcat :content))

("iPhone 11 Pro" "iPhone SE" "iPad 3" "Macbook Pro" "iPad 4" "Samsung A6+")
~~~

To get back to zippers, let's pick a problem where `tree-seq` loses its benefits. Manual search can be such a task.


## Part 4. XML search

Let's say we need to select the stores that sell iPhones from an XML with products. Note: this is the first time we've touched on the relationship between nodes. That's important! It's easy to select the data individually. Shops are locations that have the `organization` tag. iPhones are locations that have a node with the `product` tag and the `type="tablet"` attribute. But how to find a relationship between them?

The previous time, we decomposed the XML into a sequence using `xml-seq`. The problem is that the function returns a collection of nodes with no relationship, which prevents us from solving our task. Let's show this with an example: First, let's get a chain of nodes:

~~~clojure
(def xml-nodes
  (->> "products-branch.xml"
     io/resource
     io/file
     xml/parse
     xml-seq))
~~~

Let's say the product we want is in one of the elements. For example, we'll find an iPhone in the third (second from zero) node:

~~~clojure
(-> xml-nodes (nth 2))
;; {:tag :product :attrs {:type "iphone"} :content ["iPhone 11 Pro"]}
~~~

However, it is difficult to find out which store it is from. You can guess that the store is to the left of
the product, because when traversing the tree, it precedes the product. This becomes clear if you print the node tags:

~~~clojure
(->> xml-nodes (mapv :tag) (remove nil?) (run! print))
;; :catalog :organization :product :product :organization ...
~~~

This is a more or less correct assumption, but you shouldn’t rely on it too much because the result depends on the XML traversal order. In addition, solving the problem becomes more complicated. When traversing, you need not only to select the desired products but also to move back in search of a store. Then you will have to move forward again, skipping the found product, otherwise, you'll find yourself in an infinite loop. This approach is stateful and works well in imperative languages but not in Clojure.

This is where a zipper comes in. A location, which it returns at each step, remembers its position in the structure. This means that we can navigate from the location to the required place using the functions `zip/up`, `zip/right`, and others, which we discussed in the first part. In this case, the use of manual navigation is reasonable.

Let's go back to XML with a simple catalog-organization-products structure. Let's refresh it in memory.

~~~xml
<?xml version="1.0" encoding="UTF-8"?>
<catalog>
  <organization name="re-Store">
    <product type="iphone">iPhone 11 Pro</product>
    <product type="iphone">iPhone SE</product>
  </organization>
  <organization name="DNS">
    <product type="tablet">iPad 3</product>
    <product type="notebook">Macbook Pro</product>
  </organization>
</catalog>
~~~

First of all, let's find iPhones-locations and write the predicate for the iPhone:

~~~clojure
(defn loc-iphone? [loc]
  (let [node (zip/node loc)]
    (and (-> node :tag (= :product))
         (-> node :attrs :type (= "iphone")))))
~~~

Get locations with iPhones:

~~~clojure
(def loc-iphones
  (->> "products.xml"
       ->xml-zipper
       iter-zip
       (filter loc-iphone?)))

(count loc-iphones)
2
~~~

Now, to find an organization by the product, just go up one level using `zip/up`. This is true because the organization is the parent of the product:

~~~clojure
(def loc-orgs
  (->> loc-iphones
       (map zip/up)
       (map (comp :attrs zip/node))))

({:name "re-Store"} {:name "re-Store"})
~~~

For each iPhone, we should get the organization that sells it. We got duplicates because both iPhones are sold in the re:Store shop. To make the result unique, wrap it in `set`.

~~~clojure
{% raw %}
(set loc-orgs)
#{{:name "re-Store"}}
{% endraw %}
~~~

This is the answer to the question: iPhones can be bought at re:Store. If you add an iPhone to the DNS organization, the latter also appears in `loc-orgs`.

Let's solve the same problem for XML with branches. Now we can't call `zip/up` on a product to get the organization, because in some cases we'll get a branch and it will take one more step up. In order not to guess how many steps to take up, let's write the function `loc->org`. It'll step up until we find the required tag:

~~~clojure
(defn loc-org? [loc]
  (-> loc zip/node :tag (= :organization)))

(defn loc->org [loc]
  (->> loc
       (iterate zip/up)
       (find-first loc-org?)))
~~~

The `find-first` utility function finds the first collection element that matches the predicate. We'll use this function more than once.

~~~clojure
(defn find-first [pred coll]
  (some (fn [x]
          (when (pred x)
            x))
        coll))
~~~

To shorten the code, we won't declare the variables `loc-iphones` and others. Let's express the search in one form:

~~~clojure
(->> "products-branch.xml"
     ->xml-zipper
     iter-zip
     (filter loc-iphone?)
     (map loc->org)
     (map (comp :attrs zip/node))
     (set))
~~~

In the new solution, we have replaced `zip/up` with a function of a more complex climbing algorithm. Otherwise, nothing has changed.

Notice how convenient XML is for searching and navigating. If we store data in JSON, it is a combination of lists and dictionaries, and the versions with and without branches are different.

Here are products without branch stores:

~~~json
[{"name": "re-Store",
  "products": [{"type": "iphone", "name": "iPhone 11 Pro"},
               {"type": "iphone", "name": "iPhone SE"}]},
 {"name": "DNS",
  "products": [{"type": "tablet", "name": "iPad 3"},
               {"type": "notebook", "name": "Macbook Pro"}]}]
~~~

Here are products with them:

~~~json
[{"name": "re-Store",
  "products": [{"type": "iphone", "name": "iPhone 11 Pro"},
               {"type": "iphone", "name": "iPhone SE"}]},
 {"name": "DNS",
  "branches": [{"name": "Office 1",
                "products": [{"type": "tablet", "name": "iPad 3"},
                             {"type": "notebook", "name": "Macbook Pro"}]},
               {"name": "Office 2",
                "products": [{"type": "tablet", "name": "iPad 3"},
                             {"type": "notebook", "name": "Macbook Pro"}]}]}]
~~~

It goes without saying that traversing these structures requires different code.
In the case of XML, its structure is homogeneous: adding a branch only changes the depth of goods nesting, but the traversal rules remain unchanged.

Let's complicate the problem requirements: there're bundles of products among individual ones. A bundle product can't be purchased separately. For example, screen cleaning wipes  are usually sold with the device. They ask us to find a store where a wipe is sold separately.

Here is an example:

~~~xml
<?xml version="1.0" encoding="UTF-8"?>
<catalog>
  <organization name="re-Store">
    <product type="fiber">VIP Fiber Plus</product>
    <product type="iphone">iPhone 11 Pro</product>
  </organization>
  <organization name="DNS">
    <branch name="Office 2">
      <bundle>
        <product type="fiber">Premium iFiber</product>
        <product type="iphone">iPhone 11 Pro</product>
      </bundle>
    </branch>
  </organization>
</catalog>
~~~

As an exercise let's find all the wipes. Among them will be both individual products and a set.

~~~clojure
(defn loc-fiber? [loc]
  (some-> loc zip/node :attrs :type (= "fiber")))

(->> "products-bundle.xml"
     ->xml-zipper
     iter-zip
     (filter loc-fiber?)
     (map (comp first :content zip/node)))

("VIP Fiber Plus" "Premium iFiber")
~~~

Let's start solving the problem. First, we find all the wipes as we did above. Then we cut off those that are included in the bundle. In terms of a zipper, this means that this location's parent doesn't have the `:bundle` tag. After that, we move on from the rest wipes to stores.

The `loc-in-bundle?` predicate checks if a location is included in the bundle:

~~~clojure
(defn loc-in-bundle? [loc]
  (some-> loc zip/up zip/node :tag (= :bundle)))
~~~

The final solution:

~~~clojure
(->> "products-bundle.xml"
     ->xml-zipper
     iter-zip
     (filter loc-fiber?)
     (remove loc-in-bundle?)
     (map loc->org)
     (map (comp :attrs zip/node))
     (set))

{% raw %}
#{{:name "re-Store"}}
{% endraw %}
~~~

The DNS store wasn't included in the result because it sells wipes in a bundle only.

New complication: we want to buy an iPhone, *but only in a bundle* with a wipe. Which store should you direct a buyer to?

Solution: First, look for all iPhones. Select only those that present in a bundle. Next, we are looking for a wipe among the neighbors of the iPhone. If you find it, go up to the store from the iPhone or the wipe. Most of the functions required for this search are ready: these are predicates for checking a bundle, product type, and other small things. But we have not yet considered how to get the neighbors of the location.

The functions `zip/lefts` and `zip/rights` return the nodes to the left and right of the current location. If we `concat` them, we get all the neighbors (also called peers):

~~~clojure
(defn node-neighbors [loc]
  (concat (zip/lefts loc)
          (zip/rights loc)))
~~~

Note: These are nodes, not locations. Let's make a quick check with a vector:

~~~clojure
(-> [1 2 3]
    zip/vector-zip
    zip/down
    zip/right ;; node 2
    node-neighbors)

;; (1 3)
~~~

The zipper is designed in such a way that getting the right and left nodes is easier than moving the location to the left or right. Therefore, when looking for neighbors, it is better to work with nodes (values) rather than locations.

Let's add functions to check if there is a wipe that is adjacent to the location:

~~~clojure
(defn node-fiber? [node]
  (some-> node :attrs :type (= "fiber")))

(defn with-fiber? [loc]
  (let [nodes (node-neighbors loc)]
    (find-first node-fiber? nodes)))
~~~

Here's the final expression:

~~~clojure
(->> "products-bundle.xml"
     ->xml-zipper
     iter-zip
     (filter loc-iphone?)
     (filter loc-in-bundle?)
     (filter with-fiber?)
     (map loc->org)
     (map (comp :name :attrs zip/node))
     (set))

;; #{"DNS"}
~~~

As a result, we get the DNS store, because it sells the bundles including an iPhone and a wipe. Both of these products are available in re:Store, but separately.
It doesn't suit us. If we replace a wipe with a headset in the bundle, we'll get no store.

Finally, we can add new constraints. For example, from the found stores, select those that are located within a radius of 300 meters from the customer. To do this, we need the store locations on the map and a function checking if a point is inside a circle. You can choose only open stores or those that offer delivery. Let's write these features into the attributes of organizations and add selection functions.

Our XML zipper has become like a database. It provides answers to complex queries, and at the same time, the code grows slower than the semantic load. Because of its regular structure, XML is highly traversable, and zippers further enhance this property. Pay attention to the convenient transitions and relationships between nodes. Imagine the effort it took to split the data into tables and build SQL queries with many JOINs.

Of course, compared to a true database, XML has a drawback: it has no indexes and only a linear search works in it, not a binary tree one. Besides, in our approach, all data is in memory. It won't work well for very large documents with millions of records, but we don't care about that yet.


## Part 5. Editing

So far, we've ignored another zipper possibility. During the traversal, you can not only parse but also change locations. In broad terms, all CRUD (Create, Read, Update, Delete) operations familiar from web development are available to us. Below we'll discuss how they work in zippers.

As you remember, a zipper accepts a third function — `make-node`. Until now, we've passed `nil` to it. We didn't use it because we only read the data. The zipper will call the function when we ask to return the data with the changes made to the locations. The function takes two parameters: a branch and children. Its task is to relate them in the way it is customary in a tree.

For simple collections like a vector, the function is simple. It only wraps the children in `vec` to get a vector from the sequence. In `vector-zip`, the function is a little more complex because it takes metadata into account. Here is the entire code of this zipper.

~~~clojure
(defn vector-zip
  [root]
  (zipper vector?
          seq
          (fn [node children]
            (with-meta (vec children) (meta node)))
          root))
~~~

You see that the new vector (form `(vec children)`) copies the metadata of the old vector (variable `node`). If you supplement the original with `assoc` or `conj`, the metadata is preserved. In the case of `vector-zip`, we are building a new vector,  so we wrap it in `with-meta`. If we remove `with-meta`, the output will be a vector with no metadata, which may affect the program logic.

The XML zipper has a slightly different build: the children are in the `:content` field.

~~~clojure
(fn [node children]
  (assoc node :content (and children (apply vector children))))
~~~

For our zipper `map-zip` that we developed at the beginning, the build function would look like `assoc` or `into` with a collection of `MapEntry` pairs.

The zipper implicitly calls this function if it finds modified nodes. The functions `zip/edit`,  `zip/replace`, and others are used to modify. Before looking at them, let's discuss exactly how the modification occurs inside zippers.

The changes are specific because they affect locations, not the source data. After you have worked with a location, it is marked with the `:changed?` flag. It is a signal to data re-building using the `zip/root` function, which we will discuss later.

Let's look at an example with the vector `[1 2 3]`. Move to 2 and double it using the `zip/edit` function. It takes a location, a function, and residual arguments.
You are familiar with this approach from topics about atoms (`swap!`) and collections (`update`). By analogy with them, a location will receive a new value, which the function calculated based on the previous one.

Here's the location before changes:

~~~clojure
(-> [1 2 3]
    zip/vector-zip
    zip/down
    zip/right)

[2 {:l [1] :pnodes [[1 2 3]] :ppath nil :r (3)}]
~~~

Now, it's the location after the changes: Pay attention to the `:changed?` key:

~~~clojure
(def loc-2
  (-> [1 2 3]
      zip/vector-zip
      zip/down
      zip/right
      (zip/edit * 2)))

[4 {:l [1] :pnodes [[1 2 3]] :ppath nil :r (3)
    :changed? true}]
~~~

Next, we would like to get the modified vector `[1 4 3]`. Let's do it manually:

~~~clojure
(-> loc-2
    zip/up
    zip/node)

;; [1 4 3]
~~~

The `zip/root` function accepts the location with changes and does the same. Its algorithm looks like this:

- ascend to the root location;
- return a node.

To get the result in one pass, add `zip/root` to the end of the threading macro:

~~~clojure
(-> [1 2 3]
    zip/vector-zip
    zip/down
    zip/right
    (zip/edit * 2)
    zip/root)

;; [1 4 3]
~~~

The `zip/up` function, which we called either manually or implicitly in `zip/root`, does the bulk of the work. When going up, it checks if the location has been changed, and if so, rebuilds it with `make-node`. Here's a snippet of its code:

~~~clojure
(defn up
  [loc]
  (let [[node {... changed? :changed? :as path}] loc]
    (when pnodes
      (let [pnode (peek pnodes)]
        (with-meta (if changed?
                     [(make-node loc pnode (concat l ...))
                      (and ppath (assoc ...))]
                     [pnode ppath])
                   (meta loc))))))
~~~

### Multiple change

When changing one location, problems usually don't arise. However, we rarely modify a single location. In practice, we do it in a batch depending on some conditions.

Previously, we decomposed the zipper into a sequence of locations using `iter-zip`, and then passed it through a series of `map`, `filter`, and other functions. This method isn't suitable when editing. For example, we selected the second item from the `zip-iter` result and modified it:

~~~clojure
(def loc-seq
  (-> [1 2 3]
      zip/vector-zip
      iter-zip))

(-> loc-seq (nth 2) (zip/edit * 2))

;; [4 {:l [1] :pnodes [[1 2 3]] :ppath nil :r (3)
;;    :changed? true}]
~~~

Zippers themselves are immutable, and any action will return a new location. At the same time, the `zip-iter` function is designed so that each next location is obtained from the previous one. Calling `zip/edit` on one of the elements will not affect subsequent ones. If we go up from the last location, we get the vector unchanged, even if we have edited some locations in the middle before.

~~~clojure
(-> loc-seq last zip/up zip/node)
;; [1 2 3]
~~~

The following patterns are used when editing zippers.

**One element changes.** In this case, we iterate through the zipper until we meet the required location in the chain. Then we change it and call `zip/root`.

**Many elements change.** With `loop` and `zip/next` we manually iterate through the zipper. In this case, the specified function either changes the location or leaves it intact. The `recur` form gets `zip/next` from the function result. So if there were changes, `zip/next` will work with the new location, not the previous one.

The following functions can change locations:

- `zip/replace` is a literal replacement of the current node with another;
- `zip/edit` is a more flexible node replacement. Similar to `update` and `swap!`
it takes a function and additional arguments. The current node is the first argument
of the function. The result will replace the location content;
- `zip/remove` deletes a location and moves the pointer to the parent.

Functions for inserting neighbors or children:

- `zip/insert-left` adds a neighbor to the left of the current location;
- `zip/insert-right` adds a neighbor to the right;
- `zip/insert-child` adds a child to the beginning of the current location;
- `zip/append-child` adds a child to the end.

Neighbors and children differ in hierarchy. The neighbor appears on the same level as the location, and the child appears below. In the center of the diagram is the location with the vector `[2 3]`. Its neighbors are numbers 1 and 4, and its children are 2 and 3.

{: .asciichart}
~~~


                ┌─────────────┐
                │ [1 [2 3] 4] │
                └─────────────┘
                       ▲
                       │
    ┌───────┐    ┏━━━━━━━━━━━┓    ┌───────┐
    │   1   │◀───┃   [2 3]   ┃───▶│   4   │
    └───────┘    ┗━━━━━━━━━━━┛    └───────┘
                       │
                 ┌─────┴─────┐
                 ▼           ▼
             ┌───────┐   ┌───────┐
             │   2   │   │   3   │
             └───────┘   └───────┘

~~~

Let's look at these functions with simple examples. Suppose there is the key `:error` deep in the nested vectors. You need to change this to `:ok`. First, let's add a predicate for the search:

~~~clojure
(defn loc-error? [loc]
  (some-> loc zip/node (= :error)))
~~~

Now, we'll find the location, fix it and go up to the root:

~~~clojure
(def data [1 2 [3 4 [5 :error]]])

(def loc-error
  (->> data
       zip/vector-zip
       iter-zip
       (find-first loc-error?)))

(-> loc-error
    (zip/replace :ok)
    zip/root)

;; [1 2 [3 4 [5 :ok]]]
~~~

Another example: change all `nil` items to `0` in the nested vector to make the math safe. This time there may be more than one location, so a traversal through the `loop` is required. At each step, we check if the location matches the condition, and if so, we pass the `zip/next` call from the modified version to `recur`:

~~~clojure
(def data [1 2 [5 nil 2 [3 nil]] nil 1])

(loop [loc (zip/vector-zip data)]
  (if (zip/end? loc)
    (zip/node loc)
    (if (-> loc zip/node nil?)
      (recur (zip/next (zip/replace loc 0)))
      (recur (zip/next loc)))))

;; [1 2 [5 0 2 [3 0]] 0 1]
~~~

Do the same, but replace all negative numbers modulo. First, let's declare the `abs` function:

~~~clojure
(defn abs [num]
  (if (neg? num)
    (- num)
    num))
~~~

The traversal is similar to the previous one, but now instead of `zip/replace`, we call
`zip/edit`. It updates the content of the location, based on the previous value:

~~~clojure
(def data [-1 2 [5 -2 2 [-3 2]] -1 5])

(loop [loc (zip/vector-zip data)]
  (if (zip/end? loc)
    (zip/node loc)
    (if (and (-> loc zip/node number?)
             (-> loc zip/node neg?))
      (recur (zip/next (zip/edit loc abs)))
      (recur (zip/next loc)))))
~~~

In both cases, the loop logic is simple. If this is the final location, return its node. Recall that the final location is the initial location when you've returned to it after a series of `zip/next` calls. Otherwise, if the location contains a negative number, we change the content with `zip/edit`. From the changed location, we traverse to the next one. The key point: on the penultimate line, the call
`zip/next` takes the result of `zip/edit`, not the initial location. That is, changes in it will be passed on to the next step.

The examples above allow you to see patterns — repetitive techniques. Let's put them in separate functions so as not to waste attention on them in the future.

**Search for a location by predicate.**  It takes an initial location and predicate, and starts iteration. It returns the first location that matches the predicate:

~~~clojure
(defn find-loc [loc loc-pred]
  (->> loc
       iter-zip
       (find-first loc-pred)))
~~~

**Run locations with changes.** It iterates locations using `zip/next` and `loop/recur`. When moving to the next step, it wraps the location into a function. The function should either change the location or return it unchanged. This is a generic version of `loop` we wrote above.

~~~clojure
(defn alter-loc [loc loc-fn]
  (loop [loc loc]
    (if (zip/end? loc)
      loc
      (-> loc loc-fn zip/next recur))))
~~~

Let's rewrite the example with the new functions. Find in the vector a location which node is 2.

~~~clojure
(defn loc-2? [loc]
  (-> loc zip/node (= 2)))

(def loc-2
  (-> [1 2 3]
      zip/vector-zip
      (find-loc loc-2?)))
~~~

Let's double it and go to the final vector:

~~~clojure
(-> loc-2 (zip/edit * 2) zip/root)
;; [1 4 2]
~~~

Let's change the negative numbers modulo. To do this, we'll create the `loc-abs` function. If the node has a negative number, we'll return the corrected location, otherwise, the original one:

~~~clojure
(defn loc-abs [loc]
  (if (and (-> loc zip/node number?)
           (-> loc zip/node neg?))
    (zip/edit loc abs)
    loc))
~~~

Pass it to `alter-loc`:

~~~clojure
(-> [-1 2 [5 -2 2 [-3 2]] -1 5]
    zip/vector-zip
    (alter-loc loc-abs)
    zip/node)

;; [1 2 [5 2 2 [3 2]] 1 5]
~~~

### Prices in XML

Let's move on to more realistic examples with XML and products. Prepare the next file:
`products-price.xml`:

~~~xml
<?xml version="1.0" encoding="UTF-8"?>
<catalog>
  <organization name="re-Store">
    <product type="fiber" price="8.99">VIP Fiber Plus</product>
    <product type="iphone" price="899.99">iPhone 11 Pro</product>
  </organization>
  <organization name="DNS">
    <branch name="Office 2">
      <bundle>
        <product type="fiber" price="9.99">Premium iFiber</product>
        <product type="iphone" price="999.99">iPhone 11 Pro</product>
      </bundle>
    </branch>
  </organization>
</catalog>
~~~

Note that products now have prices — a characteristic that changes frequently.

As you might remember, in terms of Clojure, XML is nested dictionaries with keys.
`:tag`, `:attrs` и `:content`. But after the changes, we would like to see it in its usual, textual form. We need the opposite action: converting XML from the data structure to text. To do this, import the built-in `clojure.xml` module. Its `emit` function prints XML.

Often, `emit` is wrapped in `with-out-str` (a macro to intercept printing to a string). In the examples below, we'll output the XML in the console. Since `emit` doesn't support indentation, we will add it manually for clarity.

**The first task** is to make a 10 percent discount on all iPhones. We have almost all abstractions ready,
so let's write the solution from top to bottom:

~~~clojure
(require '[clojure.xml :as xml])

(-> "products-price.xml"
    ->xml-zipper
    (alter-loc alter-iphone-price)
    zip/node
    xml/emit)
~~~

These five lines are enough for our task. It remains to write the `alter-iphone-price` function. We need the function to take an iPhone location and return it, but with a different `price` attribute. A location of a different type will remain unchanged. Let's describe the function:

~~~clojure
(defn alter-iphone-price [loc]
  (if (loc-iphone? loc)
    (zip/edit loc alter-attr-price 0.9)
    loc))
~~~

The `loc-iphone?` predicate checks if the location holds an iPhone. We've already written it in our
previous lessons:

~~~clojure
(defn loc-iphone? [loc]
  (let [node (zip/node loc)]
    (and (-> node :tag (= :product))
         (-> node :attrs :type (= "iphone")))))
~~~

The `alter-attr-price` function takes a node (i.e., location content) and must change its attribute. The second function argument is the factor by which the current price should be multiplied. The slight difficulty is that attributes in XML are strings. To perform multiplication, you need to convert a string to a number, multiply it by a factor, and then convert the result, rounded to two digits, back to a string. All together gives us this function:

~~~clojure
(defn alter-attr-price [node ratio]
  (update-in node [:attrs :price]
             (fn [price]
               (->> price
                    read-string
                    (* ratio)
                    (format "%.2f")))))
~~~

Quick check of the function:

~~~clojure
(alter-attr-price {:attrs {:price "10"}} 1.1)
;; {:attrs {:price "11.00"}}
~~~

After running the whole chain, we should get XML:

~~~xml
<?xml version="1.0" encoding="UTF-8"?>
<catalog>
  <organization name="re-Store">
    <product price="8.99" type="fiber">VIP Fiber Plus</product>
    <product price="809.99" type="iphone">iPhone 11 Pro</product>
  </organization>
  <organization name="DNS">
    <branch name="Office 2">
      <bundle>
        <product price="9.99" type="fiber">Premium iFiber</product>
        <product price="899.99" type="iphone">iPhone 11 Pro</product>
      </bundle>
    </branch>
  </organization>
</catalog>
~~~

As a result, the price of iPhones changed by 10 percent, while the rest of the products remained
the same.

**More difficult task**: add a new product — a headset — to all bundles. Again, let's describe the solution from top to bottom:

~~~clojure
(-> "products-price.xml"
    ->xml-zipper
    (alter-loc add-to-bundle)
    zip/node
    xml/emit)
~~~

The solution differs from the previous one only in the `add-to-bundle` functions. Its logic is as follows: if the current location is a bundle, add a child to it, and if not, just return the location.

~~~clojure
(defn add-to-bundle [loc]
  (if (loc-bundle? loc)
    (zip/append-child loc node-headset)
    loc))
~~~

Checking whether it's a bundle or not:

~~~clojure
(defn loc-bundle? [loc]
  (some-> loc zip/node :tag (= :bundle)))
~~~

The `zip/append-child` function appends the value to the end of the location's children. In our case, it's the `node-headset` node, which we put into a constant:

~~~clojure
(def node-headset
  {:tag :product
   :attrs {:type "headset"
           :price "199.99"}
   :content ["AirPods Pro"]})
~~~

Here's the final XML where a new product has been added into the bundles:

~~~xml
<?xml version="1.0" encoding="UTF-8"?>
<catalog>
  <organization name="re-Store">
    <product price="8.99" type="fiber">VIP Fiber Plus</product>
    <product price="899.99" type="iphone">iPhone 11 Pro</product>
  </organization>
  <organization name="DNS">
    <branch name="Office 2">
      <bundle>
        <product price="9.99" type="fiber">Premium iFiber</product>
        <product price="999.99" type="iphone">iPhone 11 Pro</product>
        <product price="199.99" type="headset">AirPods Pro</product>
      </bundle>
    </branch>
  </organization>
</catalog>
~~~

**The third task** is to do away with all bundles. We decided that it was not profitable to sell items in bundles. All `<bundle>` tags are removed from XML, but their products must go to organizations.

And for the third time, the solution differs only in the function:

~~~clojure
(-> "products-price.xml"
    ->xml-zipper
    (alter-loc disband-bundle)
    zip/node
    xml/emit)
~~~

Let's describe the `disband-bundle` algorithm. If the current node is a bundle, we save its children (products) to a variable to not lose them. Then we delete the bundle, which will return the parent of the deleted location. In our case, it's an organization. We return it with the products attached.

~~~clojure
(defn disband-bundle [loc]
  (if (loc-bundle? loc)
    (let [products (zip/children loc)
          loc-org (zip/remove loc)]
      (append-childs loc-org products))
    loc))
~~~

The `append-childs` function is our wrapper over the built-in `zip/append-child`. The latter attaches only one element, which is inconvenient. To join a list, let's write a helper function:

~~~clojure
(defn append-childs [loc items]
  (reduce (fn [loc item]
            (zip/append-child loc item))
          loc
          items))
~~~

Here's the final XML with no bundles, but with the same products:

~~~xml
<?xml version="1.0" encoding="UTF-8"?>
<catalog>
  <organization name="re-Store">
    <product price="8.99" type="fiber">VIP Fiber Plus</product>
    <product price="899.99" type="iphone">iPhone 11 Pro</product>
  </organization>
  <organization name="DNS">
    <branch name="Office 2">
      <product price="9.99" type="fiber">Premium iFiber</product>
      <product price="999.99" type="iphone">iPhone 11 Pro</product>
    </branch>
  </organization>
</catalog>
~~~

We hope these examples are enough for you to understand how to edit zippers. Note that it took a little code: for each task, we wrote, on average, three functions. Another advantage is that the code is stateless. All functions are pure, and their call doesn't affect the data. Should an exception pop up somewhere in the middle of editing, the XML tree won't be half-changed.

## Part 6. Virtual Trees. Currency Exchange

We hope that the theory and examples were enough to start experimenting with zippers. We bring to your attention an unusual example.

So far, the second function we passed to a zipper returned children from a branch. For a vector we used `seq`, for XML — a more complex combination `(comp seq :content)`. Both options depend on the parent node, and if there are no children, the functions return `nil`.

But what happens if the function returns a constant set of children:

~~~clojure
(fn [_]
  (seq [1 2 3]))
~~~

How will such a zipper behave? Let's write it:

~~~clojure
(def zip-123
  (zip/zipper any?
              (constantly (seq [1 2 3]))
              nil
              1))
~~~

Due to the fact that each element has three children, the zipper will become infinite. Traversing it with `iter-zip` doesn't work. `Zip/next` will plunge deeper and deeper into the zipper but never reach its end.

For fun, let's take a few steps on the new zipper. Let's go down and to the right. We will find ourselves on 2 in the middle of the vector `[1 2 3]`:

~~~clojure
(def loc-2
  (-> zip-123
      zip/down
      zip/right))

(zip/node loc-2)
;; 2
~~~

Let's see our position on the diagram. A step to the left will move us on 1, a step to the right — on 3:

{: .asciichart}
~~~
              ┌───────────┐
              │     1     │
              └───────────┘
                    ▲
                    │
 ┌───────┐    ┏━━━━━━━━━━━┓    ┌───────┐
 │   1   │◀───┃     2     ┃───▶│   3   │
 └───────┘    ┗━━━━━━━━━━━┛    └───────┘
                    │
                    ▼
                ┌───────┐
                │[1 2 3]│
                └───────┘
~~~

Stepping down we fall into the next vector `[1 2 3]` and so on. Let's go down and to the right five more times, and still end up in 2:

~~~clojure
(def down-right (comp zip/right zip/down))

(-> loc-2
    down-right
    down-right
    down-right
    down-right
    down-right
    zip/node)
;; 2
~~~

The zipper can be called virtual because the data we travel through doesn't really exist — they appear on the fly.

What the use of this zipper is yet to be seen. However, it confirms the important thesis that you can get child nodes in the process of traversing the tree. This does not violate the zipper rules and provides new opportunities.

However, the explicitly specified vector `[1 2 3]` doesn't expose them. If the children are known in advance, there is no need for a zipper, since the collection can be traversed in an easier way. A suitable case is when children depend on some external factors. For example, both functions `branch?` and `children` rely on other collections and data. This is also a traversal, but according to different rules.

Let's look at the following problem. A bank exchanges currencies, for example, dollars for euros,
rubles for lira, and so on. For brevity, let's designate them in pairs: `(usd, eur)` and `(rub, lir)`. The exchange works in one direction. To exchange euros for dollars or lira for rubles, the bank must have separate rules `(eur, usd)` and `(lir, rub)`.

The client contacts the bank to exchange the currency `X` for `Y`. If there is a pair `(X, Y)` in the exchange rules, there's no problem. But if there is no such pair, the bank must build a chain of exchanges. For example, a client wants to exchange dollars for lira, but the bank doesn't have the direct pair `(usd, lir)`. However, there are pairs `(usd, eur)` and `(eur, lir)`. In this case, the client will be offered the exchange `usd -> eur -> lir`.

Write a program that accepts exchange rules, as well as input and output currencies. You have to find the exchange chains. The shorter the chain, the better. If multiple chains of the same length are possible, return all of them so the client can choose. Consider the option when there are no solutions and provide an adequate response  to this case, so as not to go into an eternal loop and not take up all the computer's resources.

Let's describe the input data in terms of Clojure. Each rule will be a vector of two keywords — which currency is exchanged for which one. The vector of rules will be called `rules`. In addition to the rules, we takes the parameters `from` and `to` — these indicate which currency to change from and to which one.

~~~clojure
;; rules
[[:usd :rub] [:rub :eur] [:eur :lir]]

:usd ;; from
:rub ;; to
~~~

The output should be a set of chains from `from` to `to` or `nil`. For the case above, the chain from dollar to euro looks like this:

~~~clojure
[:usd :rub :eur]
~~~

All together gives the function `exchanges`, which body we have to fill:

~~~clojure
(defn exchanges [rules from to]
  ...)
~~~

First, let's write some tests. They will help us warm up, and at the same time we'll understand the problem better. The first test is a simple exchange, there is a rule for it:

~~~clojure
(deftest test-simple
  (is (= [[:usd :rub]]
         (exchanges [[:usd :rub]] :usd :rub))))
~~~

A reverse exchange is impossible unless there is a reverse rule:

~~~clojure
(deftest test-reverse-err
  (is (nil? (exchanges [[:rub :usd]] :usd :rub))))
~~~

Here's a case where the exchange chain doesn't exist:

~~~clojure
(deftest test-no-solution
  (is (nil? (exchanges [[:rub :usd] [:lir :eur]] :usd :eur))))
~~~

The most important scenario is multiple exchange. You can get from dollars to rubles in two ways -- with euros or lira in the middle:

~~~clojure
 (deftest test-two-ways
  (is (= [[:usd :eur :rub]
          [:usd :lir :rub]]
         (exchanges [[:usd :eur]
                     [:eur :rub]
                     [:usd :lir]
                     [:lir :rub]] :usd :rub))))
~~~

Another test checks if we only return the shortest chains. An exchange with four currencies (in this case, `[: usd: yen: eur: rub]`) is not included in the result:

~~~clojure
(deftest test-short-ways-only
  (is (= [[:usd :eur :rub]
          [:usd :lir :rub]]
         (exchanges [[:usd :eur]
                     [:eur :rub]
                     [:usd :lir]
                     [:lir :rub]
                     [:usd :yen]
                     [:yen :eur]] :usd :rub))))
~~~

In terms of competitive programming, we can say that the problem offers separate edges of the graph. It's required to check whether it's possible to construct a continuous route from the vertex A to B from the edges. But since we're solving the problem with zippers, we won't use the terms "graph" and "edges". We don't guarantee that the solution will be optimal — perhaps the graph algorithm will do better. However, we hope that the example will further reveal the power of zippers.

As you remember, zippers are used to traverse trees, which is included in the problem statement. Let's say the `from` currency, which we want to exchange, is at the root node of the tree. Let it be a dollar. Obviously, children of this currency are all those that can be exchanged for the dollar. To do this, select the second element from each pair, where the first element is `:usd`:

~~~clojure
(def rules
  [[:usd :rub]
   [:usd :lir]
   [:rub :eur]
   [:rub :yen]
   [:eur :lir]
   [:lir :tug]])

(def from :usd)

(def usd-children
  (for [[v1 v2] rules
        :when (= v1 from)]
    v2))

;; (:rub :lir)
~~~

In our case, the dollar children are the ruble and the lira. Let's draw an imaginary tree and mark the levels:

{: .asciichart}
~~~
                  ┌───────┐
     1            │  usd  │
                  └───────┘
                      │
          ┌───────┐   │   ┌───────┐
     2    │  rub  │◀──┴──▶│  lir  │
          └───────┘       └───────┘
~~~

For each currency of the second level, we'll find child nodes according to the same rule. For convenience, let's write the `get-children` function:

~~~clojure
(defn get-children [value]
  (for [[v1 v2] rules
        :when (= v1 value)]
    v2))

(get-children :rub)
;; (:eur :yen)
~~~

The new tree:

{: .asciichart}
~~~
                      ┌───────┐
    1                 │  usd  │
                      └───────┘
                          │
              ┌───────┐   │   ┌───────┐
    2         │  rub  │◀──┴──▶│  lir  │
              └───────┘       └───────┘
                  │               │
       ┌───────┐  │  ┌───────┐    │  ┌───────┐
    3  │  eur  │◀─┴─▶│  yen  │    └─▶│  tug  │
       └───────┘     └───────┘       └───────┘
~~~

Note: it's exactly the virtual tree that we talked about recently. We don't have this tree in advance, it appears in the process. The `make-children` function is closed on the original exchange pairs. This is an example of traversing a data structure that we get on the fly from other data.

The structure of the currency tree is known and can be traversed. The question is, how deep should we traverse it? Apparently, we should stop as soon as we meet a location which node is equal to the `to` currency. Let it be yen. That is, we've connected `from` and `to` using other currencies. Let's show the solution on the diagram:

{: .asciichart}
~~~
                      ┌───────┐
    1                 │  usd  │
                      └───────┘
                          │
              ┌───────┐   │   ┌ ─ ─ ─ ┐
    2         │  rub  │◀──┘
              └───────┘       └ ─ ─ ─ ┘
                  │
       ┌ ─ ─ ─ ┐  │  ┌───────┐       ┌ ─ ─ ─ ┐
    3             └─▶│  yen  │
       └ ─ ─ ─ ┘     └───────┘       └ ─ ─ ─ ┘

~~~

To get the exchange chain, we pass the `to` location to the `zip/path` function. It should return the vector of all the location's parents, excluding itself. So, the path to the location and its node form an exchange chain.

We'll write the code based on this reasoning. Let's prepare a zipper:

~~~clojure
(def zip-val
  (zip/zipper keyword?      ;; is it currency?
              get-children  ;; what can it be exchanged for?
              nil
              from))        ;; original currency
~~~

Look for a location with the target currency in the zipper:

~~~clojure
(defn loc-to? [loc]
  (-> loc zip/node (= to)))

(def loc-to
  (->> zip-val
       iter-zip
       (find-first loc-to?)))
~~~

If it's found, we get an exchange chain from it. To do this, add the `to` value to the path:

~~~clojure
(conj (zip/path loc-to) (zip/node loc-to))

;; [:usd :rub :yen]
~~~

We have solved the main problem. But there're drawbacks: for any data, we receive only one chain, even if there are several of them. To fix this, let's search not only for the first location with the `to` currency, but all of them using `filter`.

Let's expand the initial data:

~~~clojure
(def rules
  [[:usd :rub]
   [:usd :lir]
   [:rub :eur]
   [:lir :yen]
   [:rub :yen]
   [:eur :lir]
   [:lir :tug]])

(def from :usd)
(def to :yen)
~~~

and find chains. To do this, replace `find-first` with `filter`, which should return all elements matching the predicate.

~~~clojure
(def locs-to
  (->> zip-val
       iter-zip
       (filter loc-to?)))
~~~

For each location found, let's build a path:

~~~clojure
(for [loc locs-to]
  (conj (zip/path loc) (zip/node loc)))

([:usd :rub :eur :lir :yen]
 [:usd :rub :yen]
 [:usd :lir :yen])
~~~

Now we've found chains of any length, which may be redundant. According to the problem statement, we reject an exchange of four operations if we find it with two. Let's write a function
that returns the shortest lists from the result above. It groups exchanges by length, finds the shortest one, and selects it from a map.

~~~clojure
(defn get-shortest-chains
  [chains]
  (when (seq chains)
    (let [count->chains (group-by count chains)
          min-count (apply min (keys count->chains))]
      (get count->chains min-count))))
~~~

For the last result, we get two vectors with three currencies in each. The last test `test-short-ways-only`, where long chains are discarded, covers this case:

~~~clojure
[[:usd :rub :yen] [:usd :lir :yen]]
~~~

Build the `exchanges` function from the code snippets. Make sure all tests pass. Add more cases to them.

It seems that the problem has been solved, but you can improve the solution. The fact is that with certain input data, the tree might become infinite. The program will either go into an infinite loop
or, with a limited number of steps, won't find a solution. Try to guess what might be causing this and how to  fix it. In the next section, you will find the answer to these questions.


## Part 7. Breadth-First Traversal. Improved Currency Exchange

Previously, we worked with the currency tree to find the exchange chain. We solved the problem, but mentioned that in special cases the tree can turn out to be infinite. How is this possible? Let's remember how `zip/next` traverses the tree.

The algorithm is called `depth-first`. With this traversal, the code first walks down and only then to the side (in our case, to the right). This is easy to see if you decompose the data into parts using
a zipper:

~~~clojure
(->> [1 [2 [3] 4] 5]
     zip/vector-zip
     iter-zip
     (map zip/node)
     (map println))

;; 1
;; [2 [3] 4]
;; 2
;; [3]
;; 3
;; 4
;; 5
~~~

The number `3` preceding `4` means the zipper goes deep first (inside the vector `[3]`) and only then to the right.

Even more interesting is the case with a naive virtual tree, where each node has children `[1 2 3]`. When traversing such a tree, the zipper will tend downward, each time descending into the next vector `[1 2 3]` and stopping at 1. Let's show this in the diagram:

~~~clojure
(def zip-123
  (zip/zipper any?
              (constantly (seq [1 2 3]))
              nil
              1))
~~~

{: .asciichart}
~~~
                       ┌───────┐
                       │[1 2 3]│
                       └───────┘
                           │
               ┌───────┐   │
               │[1 2 3]│◀──┘
               └───────┘
                   │
        ┌───────┐  │
        │[1 2 3]│◀─┘
        └───────┘
            │
            │
    ...   ◀─┘

~~~

Since there is no condition in our zipper to stop the production of child nodes, their nesting is unlimited. The `iter-zip` function returns an infinite chain of locations, each containing **1**. It doesn't matter how much "1" we take from it — a hundred or a thousand — we get the same number of "1".

~~~clojure
(->> zip-123
     iter-zip
     (take 10)
     (map zip/node))

;; (1 1 1 1 1 1 1 1 1 1)
~~~

Now let's get back to currency exchange. Suppose a bank changes rubles for dollars, dollars for euros, and euros for rubles. Let's express it in code:

~~~clojure
(def rules
  [[:rub :usd]
   [:usd :eur]
   [:eur :rub]])
~~~

As you can see, we have a vicious circle:

{: .asciichart}
~~~
             ┌───────┐
        ┌───▶│  rub  │────┐
        │    └───────┘    │
        │                 ▼
    ┌───────┐         ┌───────┐
    │  eur  │◀────────│  usd  │
    └───────┘         └───────┘
~~~

The previous solution ignores the cyclical nature of the rules, this is its drawback. Suppose a client wants to exchange rubles for lira. Let's start building a tree from the ruble. Here's the beginning of the chain:

{: .asciichart}
~~~
                       ┌───────┐
                       │  rub  │
                       └───────┘
                           │
               ┌───────┐   │
               │  usd  │◀──┘
               └───────┘
                   │
        ┌───────┐  │
        │  eur  │◀─┘
        └───────┘
            │
 ┌───────┐  │
 │  rub  │◀─┘
 └───────┘
~~~

So we came to the ruble again. For it, we get the dollar again, for the dollar the euro, then the ruble. If we continue to iterate, we'll dive into this chain endlessly.

Logic dictates that you need to stop going deep if the next currency is equal to the initial one. Simply put, a `:rub` element that is not at the root node can't have children. However, in the `branch?` and `make-children` functions, we don't know where the element is located in the tree. They get values, not locations. We could fix this with a state, such as an atom, that would hold the list of the currencies that we traversed.

Another option is to check how many times we are referring to the `from` currency to find children. If this is the first call, then we're at the top of the tree (i.e., at the root node) Let's find the children
and change the atom on which the `children` function is closed. If not for the first time (atom
changed), we came across a cyclical case, and there are no children for it.

Both options have the right to exist, but for now, we want to do without state and mutable means.

If you examine the tree again, it becomes clear that the problem lies in the traversal order. Since we strive in depth, there is a high probability of falling into a wormhole from which we cannot get out. We might be lucky if we successfully stepped into the branch with the solution (on the left), and the infinite branch (on the right) remained untouched:

{: .asciichart}
~~~
                 ┌───────┐
                 │  rub  │
                 └───────┘
                     │
         ┌───────┐   │   ┌───────┐
         │  yen  │◀──┴──▶│  usd  │
         └───────┘       └───────┘
             │               │
 ┏━━━━━━━┓   │               │   ┌───────┐
 ┃  lir  ┃◀──┘               └──▶│  eur  │
 ┗━━━━━━━┛                       └───────┘
                                     │
                                     │   ┌───────┐
                                     └──▶│  rub  │
                                         └───────┘
                                             │
                                             │
                                             └──▶  ...

~~~

However, you cannot rely on luck when solving problems.

Now, let the zipper traverse the location not in depth, but in breadth and to the right. With this order, we are not threatened by infinite branch. We won't try to exhaustively traverse an infinite branch if it occurs in the tree.
Instead, we go down the levels of the tree and read all the elements of each level. Even if one of them originated from an endless branch, this doesn't prevent you from exploring the rest of the elements. The figure below shows that horizontal traversal helps you get to the solution. In this case, the vertical traversal would go to infinity because both branches are cyclical.

{: .asciichart}
~~~


                               ┌───────┐
                           ┌───│  rub  │
                           │   └───────┘
                           ▼
                       ┌───────┐       ┌───────┐
                       │  yen  │──────▶│  usd  │
                       └───────┘       └───────┘
                                           │
                  ┌────────────────────────┘
                  ▼
              ┏━━━━━━━┓                         ┌───────┐
              ┃  lir  ┃────────────────────────▶│  eur  │
              ┗━━━━━━━┛                         └───────┘
                                                    │
           ┌────────────────────────────────────────┘
           ▼
       ┌───────┐     ┌───────┐           ┌───────┐     ┌───────┐
       │  rub  │────▶│  tug  │──────────▶│  yen  │────▶│  rub  │
       └───────┘     └───────┘           └───────┘     └───────┘
           │                                               │
           │                                               │
    ...  ◀─┘                                               └──▶  ...


~~~

The problem is that the `clojure.zip` module offers only depth-first order of traversal with `zip/next`. There's no other algorithm. We'll write our own function to traverse the zipper "in layers", as shown in the figure:

{: .asciichart}
~~~
                            ┌───────┐
  1                         │   1   │
                            └───────┘
                                │
              ┌───────┐         │         ┌───────┐
  2           │   2   │◀────────┴────────▶│   3   │
              └───────┘                   └───────┘
                  │                           │
      ┌───────┐   │   ┌───────┐   ┌───────┐   │   ┌───────┐
  3   │   4   │◀──┴──▶│   5   │   │   6   │◀──┴──▶│   7   │
      └───────┘       └───────┘   └───────┘       └───────┘
~~~

We'll get the following layers:

~~~clojure
[1]
[2 3]
[4 5 6 7]
~~~

In this case, each element is not a primitive, but a location. This means that the element remembers its position in the tree, you can move from it to other elements, get its path, and so on.

First, we need a function that will return the child locations of the original one. Its logic is simple: if it's possible to go down from the location, we move to the right until we reach emptiness.

~~~clojure
(defn loc-children [loc]
  (when-let [loc-child (zip/down loc)]
    (->> loc-child
         (iterate zip/right)
         (take-while some?))))
~~~

Note that this function isn't the same as `zip/children`. The latter returns values, not locations, and we need locations exactly. Compare expressions:

~~~clojure
(-> [1 2 3]
    zip/vector-zip
    zip/children)

(1 2 3)
~~~

and

~~~clojure
(-> [1 2 3]
    zip/vector-zip
    loc-children)

([1 {:l [] :pnodes [[1 2 3]] :ppath nil :r (2 3)}]
 [2 {:l [1] :pnodes [[1 2 3]] :ppath nil :r (3)}]
 [3 {:l [1 2] :pnodes [[1 2 3]] :ppath nil :r nil}])
~~~

In the second case, we got the locations, while `zip/children` simply accessed the find children function passing to the zipper.

Suppose, for some location, `loc-children` returned a list of its children. To go down one level, you need to find their children and combine the result. The easiest way to do this is to use the following expression:

~~~clojure
(mapcat loc-children locs)
~~~

where `locs` is a list of locations of the current level. If we pass the result of `mapcat` to` locs` parameter, we'll move on even further. We'll do this until we get an empty sequence. All together gives us the `loc-layers` function:

~~~clojure
(defn loc-layers [loc]
  (->> [loc]
       (iterate (fn [locs]
                  (mapcat loc-children locs)))
       (take-while seq)))
~~~

It takes the root location from where to start iterating over the layers. We set the first layer explicitly as a vector of one location. Then its children follow, then children of the children and so on. We'll only stop when getting an empty layer. Quick check:

~~~clojure
(def data [[[[1]]] 2 [[[3]]] 3])

(let [layers (-> data
                 zip/vector-zip
                 loc-layers)]
  (for [layer layers]
    (->> layer
         (map zip/node)
         println)))

;; ([[[[1]]] 2 [[[3]]] 3])
;; ([[[1]]] 2 [[[3]]] 3)
;; ([[1]] [[3]])
;; ([1] [3])
;; (1 3)
~~~

To get a chain where the elements go from left to right, we concatenate the layers using `concat`. This function is not needed for solving the problem, but it can be useful:

~~~clojure
(defn loc-seq-layers [loc]
  (apply concat (loc-layers loc)))
~~~

Let's go back to currency exchange. Let's select the exchange rules so that they contain cyclical dependencies. The zipper remains the same: it builds the exchange tree using the local `get-children` function, which is closed on the rules.

~~~clojure
(def rules2
  [[:rub :usd]
   [:usd :eur]
   [:eur :rub]

   [:rub :lir]
   [:lir :eur]
   [:eur :din]
   [:din :tug]])
~~~

The style of working with this zipper will change. Now we iterate through it using not `zip/next` but our `loc-layers`. At each step, we should get exchange layers. We have to find the locations, which node is equal to the final currency, in the next layer. As soon as we have found at least one, the problem is solved. It remains only to calculate the path to them.

~~~clojure
(defn exchange2 [rules from to]

  (letfn [(get-children [value]
            (seq (for [[v1 v2] rules
                       :when (= v1 value)]
                   v2)))

          (loc-to? [loc]
            (-> loc zip/node (= to)))

          (find-locs-to [layer]
            (seq (filter loc-to? layer)))

          (->exchange [loc]
            (conj (zip/path loc) (zip/node loc)))]

    (let [zipper (zip/zipper keyword?
                             get-children
                             nil
                             from)]

      (->> zipper
           loc-layers
           (some find-locs-to)
           (map ->exchange)))))
~~~

As you may have noticed, now there is no need to compare the lengths of the chains: if the locations belong to the same level, the number of steps to them is the same. According to the problem statement, we are interested in the shortest exchange options. For example, if one chain was found on the third level, and there are three chains on the fourth, the latter are not interesting to us -- we
complete the traversal on the third layer.

Here are examples of exchanges regarding the rules specified in `rules2`:

~~~clojure
(exchange2 rules2 :rub :eur)
([:rub :usd :eur] [:rub :lir :eur])

(exchange2 rules2 :rub :tug)
([:rub :usd :eur :din :tug] [:rub :lir :eur :din :tug])

(exchange2 rules2 :lir :din)
([:lir :eur :din])
~~~

The solution is still not perfect. If we specify a pair of currencies for which there is no chain, we'll get an infinite loop. To stop it, limit the number of layers to some reasonable number, such as five. From a financial point of view, currency exchange  with no restrictions is likely to be detrimental, and therefore meaningless. Technically, we need to add the form `(take N)` right after `loc-layers`:

~~~clojure
(->> zipper
     loc-layers
     (take 5)
     (some find-locs-to)
     (map ->exchange))
~~~

Now, we get an empty result for an invalid pair:

~~~clojure
(exchange2 rules2 :tug :yen)
()
~~~

The task can be improved further. For example, you can calculate costs and transaction fees for each chain. To do this, add the exchange rate and fee to the `[:from: to]` vector. Depending on whether we represent a client or a bank, we'll look for the most optimal or the most expensive exchanges. Please, come up with your own variations for this problem. At this point we'll finish with currencies and move on.

In this chapter, we've discussed how the traversal order affects the solution to the problem. Breadth-first and depth-first traversal ordering applies to different cases. This is important for infinite trees, when the algorithm can loop while traversing. There is no breadth-width traversal in the `clojure.zip` package, but you can easily write a function to divide the zipper into layers. You may find `loc-layers` useful in other cases involving graphs and vertices.


## Part 8. Summary

Finally, let's take a look at other zipper features that you might find useful.

### HTML

The previous examples show that zippers work fine with XML. By the way, you can apply them to HTML as well. Strictly speaking, the syntax of the formats is different: some HTML elements like `<br>` or `<img>` don't have closing tags. Parsers that take these features into account can solve the problem. As a result, we get an HTML tree that can be traversed as in the examples above.

[hickory]:https://github.com/davidsantiago/hickory
[jsoup]:https://jsoup.org/

The [Hickory][hickory] library offers an HTML markup parser. The parsing is based on the Java library [JSoup][jsoup], which builds a tree of elements. Hickory contains a function to rebuild a Java tree into Clojure-like one and get a zipper. Add a dependency to the project:

~~~clojure
[hickory "0.7.1"]
~~~

and run the example:

~~~clojure
(ns zipper-manual.core
  (:require
   [hickory.core :as h]
   [hickory.zip :as hz]
   [clojure.zip :as zip]))

(def html (-> "https://grishaev.me/"
              java.net.URL.
              slurp))

(def doc-src (h/parse html))
(def doc-clj (h/as-hiccup doc-src))
(def doc-zip (hz/hiccup-zip doc-clj))
~~~

How are these conversions performed? A website layout is loaded into the `html` variable as a string. The `doc-src` variable contains a tree obtained from HTML. It's an object of the `Document` class from the `org.jsoup.nodes` package. For Clojure, it's a black box: to work with it, it needs to read the documentation for the `Document` class.

The `as-hiccup` function converts the document into a set of nested vectors which look like this:

~~~clojure
[:tag {:attr "value"} & [...]],
~~~

The tag comes first, then the attribute dictionary, followed by any number of the same vectors or strings. This is the standard HTML representation in Clojure, and many libraries use the same format.

The `hiccup-zip` function returns the zipper for that structure. It can do everything that we've practiced earlier, for example:

- remove unwanted tags like `<script>`, `<iframe>`;
- leave these tags, but secure their attributes;
- leave dangerous tags only if their source points to trusted sites;
- look for items of interest to us.

Here's how to find all the images on a webpage:

~~~clojure
(defn loc-img? [loc]
  (some-> loc zip/node first (= :img)))

(defn loc->src [loc]
  (some-> loc zip/node second :src))

(->> doc-zip
     iter-zip
     (filter loc-img?)
     (map loc->src))

("/assets/static/photo-round-small.png" ...)
~~~

The first function checks if the location points to a node with the `<img>` tag, the second
extracts the `src` attribute from it. The third form returns a list of links to images.

Based on these actions, you can build HTML filtering, especially if an HTML markup comes from a source you don't trust. Another scenario is to find a suitable image for a social media cover in HTML. To do this, you need to select all images, estimate their width and height, and select the largest in area (if the `width` and `height` attributes are filled in).

Hickory considers typical cases and offers selectors for searching by tag and attribute. It isn't even necessary to cast the JSoup tree to a zipper to do this. However, in rare cases, you need to find tags with complex relationships, as in the product and bundle example (either only in the bundle or strictly outside it). These problems fit zippers very well.

### Data and Serialization

Another plus of zippers is that they are represented by data — a combination of lists and maps. This means that you can write the current zipper in EDN or JSON. When reading, we get the old data structure and continue traversing from where we left off. This is the difference between Clojure and object languages, where, in the general case, you cannot write an object to a file without some effort.

When restoring a zipper, remember about its metadata. The functions `branch?`, `children`, and `make-node` that we passed to the constructor are stored in the zipper metadata. This is done to separate data from actions on it. Let's check the zipper metadata we got from HTML:

~~~clojure
(meta doc-zip)

#:zip{:branch? #function[clojure.core/sequential?]
      :children #function[hickory.zip/children]
      :make-node #function[hickory.zip/make]}
~~~

Let's write functions for resetting and reading EDN:

~~~clojure
(defn edn-save [data path]
  (spit path (pr-str data)))

(defn edn-load [path]
  (-> path slurp edn/read-string))
~~~

Let's say we've made some iterations on a zipper and saved it:

~~~clojure
(-> doc-zip
    zip/next
    zip/next
    zip/next
    (edn-save "zipper.edn"))
~~~

If we read the EDN and pass the result to `zip/next`, we'll get an error. The function will call `branch?` and `children` from the metadata that has not been saved, resulting in an exception. To make a zipper from a file work, add metadata to it. You can either move it into a variable in advance or declare it manually.

~~~clojure
(def zip-meta (meta doc-zip))

;; or

(def zip-meta
  #:zip{:branch? sequential?
        :children #'hickory.zip/children
        :make-node #'hickory.zip/make})
~~~

In the second case, we had to specify the `children` and `make-node` functions as variables (instances of the `Var` class) because they are private. The read zipper will be in the same state as at the time of saving.

~~~clojure
(def doc-zip-new
  (-> "zipper.edn"
      edn-load
      (with-meta zip-meta)))

(-> doc-zip-new zip/node first)
:head
~~~

Storing the zipper in long-term memory brings new possibilities. For example, traversal of certain data takes time, and the program can perform the task in chunks, keeping the intermediate result. This is how complex business scenarios work. If a customer refuses the services of the company, you must delete their records in the database, files, links to them in documents and much more. This process can be thought of as a set of steps. At each step, the code reads a zipper as EDN from the database and adds metadata. Then it shifts the zipper one `zip/next`, performs the task, and updates the record in the database with the new version of the zipper. Once you've reached the initial node (`zip/end?` returns `true`), you mark the record in the database as resolved.

### Other Uses

The example with the currency exchange shows how to find a solution to the problem by brute force search. Whether you're looking for the optimal chain of steps, maximum cost, or a traversal route, zippers might help you. It is easy to check if they are suitable for solving your problem. The zipper implies that you have a value and several others based on it, they in turn have their values and so on. If the condition works, you are one step away from building the tree and traversing it.

Let's say, according to the exchange table, the dollar (current value) can be exchanged for the euro and the ruble (child values). From point A (current) you can drive to points B and C (children). In HTML, one tag can include other tags. In all three cases, you can use a zipper. You only need to define the functions `branch?` (if an element can have children) and `children` (how to find them specifically).

### Third-party Libraries

[data.zip]:https://github.com/clojure/data.zip/

The `clojure.zip` module offers enough navigation functions. Nevertheless, throughout this chapter, we had to write a few functions ourselves. The library **[data.zip]** contains various add-ons for zippers, including the same ones as we wrote. Perhaps the library will shorten your utility code.

### Summary

Zippers are means for navigating the data structure. A zipper offers movement in four directions: down, up, left, and right. An element in the center is called a node.

A zipper can navigate a wide variety of structures. It needs to know only two things: whether the current element is a branch of a tree, and if so, how to find the children. To do this, the zipper takes the `branch?` and `children` functions, which are later stored in metadata.

Usually, children are found from the parent node, but in some cases we get them dynamically. For example, to find out which currencies can be exchanged for the current one, you can refer to the exchange map. To do this, the `children` function has to see the map as a global variable or a closure.

The current zipper element is called a location. It stores not only the value, but also the data for going in all directions, as well as the path. These qualities set zippers apart from `tree-seq` and analogs that decompose a tree into a chain not including a path to an element. Some tasks consist precisely of finding the right path.

The zipper offers functions for editing and deleting the current node. Editing can be based on the current value (`zip/edit`) or the new one (`zip/replace`).

By default, zipper traversal is depth-first. When moving to the end, the location will receive a mark that the cycle has been completed. Use the `zip/end?` function as a sign of ending an iteration. In our examples, we wrote the `zip-iter` function that does exactly one traversal.

Breadth-first traversal is required for some tasks. This can happen when one of the tree branches is potentially infinite. For breadth-first traversal, we wrote our own  functions that don't come with the Clojure.zip.

Zippers are useful for working with XML, finding solutions, and filtering HTML. Try to figure them out to solve such problems in a short and elegant way.
