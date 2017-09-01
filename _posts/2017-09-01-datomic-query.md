---
layout: post
title:  "Conditional Queries in Datomic"
date: 2017-09-01T20:49:00Z
permalink: /en/datomic-query
categories: clojure datomic sql
lang: en
---

Let's discuss one thing related to Clojure and Datomic that might be a bit
tricky especially if you have never faced it so far.

Imagine you return a list of some entities to the client. It might be a list of
orders, visits or whatever. Usually, when a user is exploring a long list of
something, it's a good idea to let them filter the list by some criteria: price,
date, age etc. On the server side, you need to apply those filters
conditionally. Say, when the `date_from` query string parameter is passed, you
apply it to the query as well or live it untouched otherwise. The same for the
rest of filters.

Things become more tough when filters are applied to foreign entities. For
example, an order references a user, and a user references a department. If the
`department_name` parameter has been passed, you should join all the required
tables and filter a proper field.

Joining all the tables even if no filters were supplied is a wrong
approach. Join operations are expensive and should never be performed in vain. A
toolset that joins tables should take into account which ones has already been
added into a query and never link them twice.

[django]: https://www.djangoproject.com/

Such systems that control the cases mentioned above are names ORMs. They are
pretty complicated and full of ugly code and implicit hacks. But on the top
level they behave quite friendly. Say, in [Django][django] (a major Python
framework) I would perform something like that:

~~~python
query = models.Order.objects.all()
department_name = request.query_string.get("department_name")
if department_name:
    query = query.filter(user__department_name=department_name)
~~~

Looks pretty neat. This automatically joins `users` and `department` tables
under the hood using `inner join` SQL clause and puts `department.name = ?`
condition into `where` section.

[yesql]: https://github.com/krisajenkins/yesql
[honeysql]: https://github.com/jkk/honeysql
[korma]: https://github.com/korma/Korma

People who work with such non-wide spreaded languages as Clojure usually do not
use ORMs. But still, we need to build complicated queries. The community offers
a handful of libraries ([YeSQL][yesql], [HoneySQL][honeysql], [Korma][korma])
where a query is being constructed within data structures: vectors, maps. I've
been always agents that approach. Before getting more experienced with Clojure,
I felt uncomfortable constructing nested vectors like this one:

~~~
[:select [:foo :bar]
 :from :test
 :where [(= :name "test")
         (when age-param
           (> :age age-param))]]
~~~

The reason why I do not appreciate that is I cannot see the final query behind
brackets and colons. It will definitely fail me once I need to express
something like this (a fragment of a query from production):

~~~sql
select
  foo.id,
  array_agg(foo.type_id) filter (where foo is not null) as type_ids,
  foo.name
from foo_table as foo
~~~

Modern libraries say it's easy and fun to express your queries through data
structures, but it is not, really. It becomes a challenge when applying multiple
conditions to a data structure without seeing the final result.

[hugsql]: https://github.com/layerware/hugsql

A good approach might be using a templating system. Say, [HugSQL][hugsql]
library allows to inject Clojure snippets into your SQL query. Those snippets
are surrounded with standard SQL comments so they do not break syntax. There
won't be an error if you copy and paste such a Clojure-instrumented query into
some RDBS administration tool.

Here is an example of declaring such a query:

~~~sql
-- :name get-oreders :?
select o.*
from oreders o
/*~ (when (:department-name params) ~*/
join user u on o.user_id = u.id
join departments d on user.department_id = d.id
where
    d.name = :department-name
/*~ ) ~*/
order by o.created_at;
~~~

Than it compiles into a Clojure function:

~~~clojure
;; no joins, no filters
(get-oreders db)

;; causes joins and filtering
(let [dep-name (-> request :params :department-name)]
  (get-oreders db {:department-name dep-name}))
~~~

[selmer]: https://github.com/yogthos/Selmer

You may go further and include [Selmer][selmer], a template library inspired by
Django. It's aimed at HTML rendering first but still may be used for any kind of
documents including SQL.

As I see it, a good templating system would be enough to generate SQL that fits
your business logic.

Now I'd like to discuss the same problem when using Datomic instead of classical
RDBS solutions. All the tutorials that I have read do not cover a case then you
need to apply several filters to a query. Suddenly, it may really become a
problem. Let's return to our example with orders. Once you don't have any
filters, the query looks simple:

~~~clojure
'[:find (pull ?o [*])
  :in $
  :where [?o :order/number]]
~~~

But if you've got a department name you need to:

1. inject a new parameter into `:in` section;
2. inject additional clauses into `:where` section;
3. prevent joining a user or a department entities twice if you need to filter
   by other department or user field.

As we've seen, once you have a template system, you may render SQL queries as
well. All you need is to write a query, test how does it behaves and then wrap
some parts of it with special conditional tags.

In Datomic, a query is usually a vector of symbols. Moreover, an immutable
one. Thus, you cannot modify a query and adding something in the middle of it
would be difficult. Surely you could wrap a query into an atom or track indexes
where to inject a new item somehow but all of that would be a mess.

[list-vs-map]: http://docs.datomic.com/query.html#list-vs-map

What I propose is using a special kind of a query represented as a map with
`:find`, `:where` and other keys. As
the [Datomic documentation says][list-vs-map], when processing a query, every
vector is turned into a map anyway. If we had a map, it would be easier to
inject new items into it.

[cond->]: https://clojuredocs.org/clojure.core/cond-%3E

To avoid wrapping a map with an atom or redefining it continuously inside `let`
clause, there is a great form named [cond->][cond->]. It is a mix of both
threading macro and `cond` clause. It takes an initial value and a bunch of
predicate/update pairs. If a predicate form evaluates in true, an update form is
fired using the standard threading macro. Thus, an update form should be either
a function or a list where the second argument is missing and will be
substituted with a value from a previous pair.

What's the most interesting about `cond->` is unlike `cond` or `case` forms, its
branches are evaluated continuously. Each update form takes a value that a
previous form has produced. In other terms, an initial value goes through
multiple updates without being saved in some temporary variable.

In example below, I've got a data set that consists from `user`, `location` and
`visit` entities. Both `user` and `location` are simple ones and store just
dates, strings and so on. A `visit` is a bit more complex. It means that a user
has visited a location and assigned a mark to it. Therefore, a visit references
a user and a location entities as well.

The goal is to get an average mark for a specific location. In addition, such a
value might be filtered by user's age, gender or location's country name. Those
parameters come from a query string and could be either totally skipped, passed
partially or completely. I've got a function that accepts a location id and a
map of optional parameters:

~~~clojure
(defn location-avg
  [location-id {:keys [fromDate
                       toDate
                       fromAge
                       toAge
                       gender]}]

~~~

The initial Datomic query:

~~~clojure
(def query-initial
  '{:find [(avg ?mark) .]
    :with [?v]
    :in [$ ?location]
    :args []
    :where [[?v :location ?location]
            [?v :mark ?mark]]})
~~~

Now, here is a long pipeline with comments:

~~~clojure
(cond-> query-initial

  ;; First, add two initial arguments: database instance and location reference.
  ;; This form will always be evaluated.
  true
  (update :args conj
          (get-db)                    ;; returns the DB instance
          [:location/id location-id]) ;; location reference

  ;; If either from- or to- date were passed, join the `visit` entity
  ;; and bind its `visited_at` attribute to the `?visited-at` variable.
  (or fromDate toDate)
  (update :where conj
          '[?v :visited_at ?visited-at])

  ;; If the `fromDate` filter was passed, do the following:
  ;; 1. add a parameter placeholder into the query;
  ;; 2. add an actual value to the arguments;
  ;; 3. add a proper condition against `?visited-at` variable
  ;; (remember, it was bound above).
  fromDate
  (->
   (update :in conj '?fromDate)
   (update :args conj fromDate)
   (update :where conj
           '[(> ?visited-at ?fromDate)]))

  ;; Do the same steps for the `toDate` filter,
  ;; but the condition slightly differs (< instead of >).
  toDate
  (->
   (update :in conj '?toDate)
   (update :args conj toDate)
   (update :where conj
           '[(< ?visited-at ?toDate)]))

  ;; To filter by user's fields, we bind a user reference
  ;; to the `?user` variable:
  (or fromAge toAge gender)
  (update :where conj
          '[?v :user ?user])

  ;; If from/to age filters we passed, bind user's age
  ;; to the `?birth-date` variable.
  (or fromAge toAge)
  (update :where conj
          '[?user :birth_date ?birth-date])

  ;; Then add placeholders, arguments and where clauses
  ;; for specific filters: fromAge, if passed...
  fromAge
  (->
   (update :in conj '?fromAge)
   (update :args conj (age-to-ts fromAge))
   (update :where conj
           '[(< ?birth-date ?fromAge)]))

  ;; ...and the same for toAge.
  toAge
  (->
   (update :in conj '?toAge)
   (update :args conj (age-to-ts toAge))
   (update :where conj
           '[(> ?birth-date ?toAge)]))

  ;; To filter by gender, bind user's gender to a variable
  ;; and add a clause:
  gender
  (->
   (update :in conj '?gender)
   (update :args conj gender)
   (update :where conj
           '[?user :gender ?gender]))

  ;; The final step is to remap a query (see below).
  true
  remap-query)
~~~

Remapping a query is important because the initial data is a bit wrong. The
proper structure for a map query looks as follows:

~~~clojure
{:query <query-map>
 :args [db location_id fromDate ...]}
~~~

In my case, I believe it's simpler to keep one-level map rather than deal with
two levels (`:query` first, then `:args`). It would force me to use `update-in`
instead if just `update` and write more code. Here is the `remap-query`
function:

~~~clojure
(defn remap-query
  [{args :args :as m}]
  {:query (dissoc m :args)
   :args args})
~~~

Finally, let's check our results. If somebody passes all the filters, the query
will look like:

~~~clojure
{:query
 {:find [(avg ?mark) .],
  :with [?v],
  :in [$ ?location ?fromDate ?toDate ?fromAge ?toAge ?gender],
  :where
  [[?v :location ?location]
   [?v :mark ?mark]
   [?v :visited_at ?visited-at]
   [(> ?visited-at ?fromDate)]
   [(< ?visited-at ?toDate)]
   [?v :user ?user]
   [?user :birth_date ?birth-date]
   [(< ?birth-date ?fromAge)]
   [(> ?birth-date ?toAge)]
   [?user :gender ?gender]]},
 :args [<db-object> [:location/id 42] 1504210734 1504280734 20 30 "m"]}
~~~

[query]: http://docs.datomic.com/clojure/#datomic.api/query

Now you pass it into [datomic.api/query][query] function that accepts a map-like
query. In my case, the result is something like: `4.18525`.

As you have seen, composing complicated queries with Datomic might a bit tricky
due do immutability and differences between string templates and data
structures. But still, Clojure provides rich set of tools for processing
collections. In my case, the standard `cond->` has made all the
pipeline. Neither atoms nor other tricks to track the state were required. There
is a common rule: once you've got stuck with a data structure, keep yourself
from inventing "smart" ways to deal with it. There is probably a built-in macro
in `clojure.core` for that.
