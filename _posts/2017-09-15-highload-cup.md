---
layout: post
title:  "Clojure in Highload Cup"
permalink: /en/highload-cup
tags: highload mail.ru clojure
lang: en
---

[highloadcup]: https://highloadcup.ru/

1st September was the day when the [Highload Cup][highloadcup] competition has
finished. I'm proud of I took participation in it. The Cup was a quite
interesting event full of drive, enthusiasm and night coding. Although I haven't
taken any prize, I've got lots of fun and new knowledge that I've partially
shared with you in my previous publications.

In that post, I'm going to highlight some technical details unmentioned before.

A minute of vanity: I'm a single developer who used Clojure/Datomic stack to
implement a solution. And by the way a single member from my hometown Voronezh
(my colleagues who live here argued on Cup passionately but still without a
result).

The task was easy only at first glance. For non-Russian speakers, let me retell
it quickly. You've got three entities: a user, a location and a visit. They
represent, respectively, a physical person, a place in the world and a fact that
somebody visited a place and put mark for it. Say, the last year John Doe
visited Siberia and was so excited that he put 5 stars.

Your task is to ship a Docker container that carries a REST server. The server
should implement basic CRUD operations on users, locations and visits. All the
data pass in and out using JSON.

In addition to CRUD, there are two aggregate APIs. The first one is to return
all the places visited by specific user. The second one is to return an average
mark assigned to specific location by all the users who have ever visited
it. Both APIs accept optional query string arguments to filter the results by
foreign entities. Say, `fromAge` parameter stands for we need to consider only
those people who are alder than that number, `distanceTo` limits those locations
with distances less than the passed value and so on.

Once you've built a Docker container, you submit it to the central server where
it is shot with a special software. It considers lots of such facts as proper
response codes, incorrect data filtering, response time and so on. Than it
calculates your rank. The less is the rank, the better your position is.

Sounds simple, but I spent a week trying to implement fast Clojure
solution. TL/DR: finally, the C++ guys have come and taken the top of the rank
table. Some of them wrote their own HTTP server. But still, it was quite fun to
compete with them.

[repo]: https://github.com/igrishaev/highloadcup
[datomic]: http://www.datomic.com/
[spec]: https://clojure.org/guides/spec

As the Cup has finished, you are welcome to [review my code][repo] (it was
private before due to Cup rules). The final version uses Clojure
1.9, [Datomic][datomic] free edition and [clojure.spec][spec] to validate
incoming data. There were some experiments with SQLite database kept in memory
but at the end I finished with Datomic (more on that below).

So here are some technical details that I wanted to discuss.

### Reading ZIP on the fly

According to the Cup's rules, when your application starts, it finds the input
data in `/tmp/data` directory. There is a single zip archive with JSON files
inside. The Docker container is mount with read-only file system, so you cannot
unzip it using standard Unix tools. Instead, you should read the data directly
using streams.

Thanks to Java, it ships `java.util.zip` package with all we need
inside. Surprisingly, I ended up with quite short code to read the file:

~~~clojure
(defn read-zip [path]
 (let [zip (java.util.zip.ZipFile. path)
    entries (-> zip .entries enumeration-seq)]
  (for [e entries]
   (.getInputStream zip e))))
~~~

It accepts path to a zip file and returns a lazy sequence of input streams. Each
stream might be read into a data structure with a function:

~~~clojure
(defn read-stream [stream]
 (json/parse-stream (io/reader stream) true))
~~~

[cheshire]: https://github.com/dakrone/cheshire

, where `json` is an alias to the [Cheshire][cheshire] library included as
`[cheshire.core :as json]` at the top of the namespace.

### The data backend

Since the beginning it was obvious to keep the data in memory but not on the
disk. I was thinking on whether I should use in-memory SQLite backend or use
Datomic within in-memory storage. After all, I've tried both options and ended
up with Datomic finally.

[clj-sqlite]: http://grishaev.me/en/clj-sqlite
[hugsql]: https://www.hugsql.org/

With SQLite, I've got only one trouble when connecting to the database. I
described the problem in details in my previous
post ["In-Memory SQLite Database In Clojure"][clj-sqlite]. For the rest, it
worked fine. I used [HugSQL][hugsql] to compose queries like this:

~~~sql
-- :name get-location-avg :? :1
select
  avg(v.mark) as avg
from visits v
/*~ (when (or (:fromAge params) (:toAge params) (:gender params)) */
join users u on v.user = u.id
/*~ ) ~*/
where
  v.location = :location_id
  /*~ (when (:fromDate params) */
  and v.visited_at > :fromDate
  /*~ ) ~*/
  /*~ (when (:toDate params) */
  and v.visited_at < :toDate
  /*~ ) ~*/
  /*~ (when (:fromAge params) */
  and u.birth_date < :fromAge
  /*~ ) ~*/
  /*~ (when (:toAge params) */
  and u.birth_date > :toAge
  /*~ ) ~*/
  /*~ (when (:gender params) */
  and u.gender = :gender
  /*~ ) ~*/
~~~

Then I switched to Datomic backend. I was wondering whether it would be slower
than good old SQLite. The results were in favor of Datomic: it was about 1.5
times faster when returning responses.

For in-memory backend, you do not need a registered version or a license
key. Just add `[com.datomic/datomic-free "0.9.5561.54"]` into dependencies list
and I've done. Then pass something like `"datomic:mem://highloadcup"` when
connecting to the database.

It was a good decision to create common functions for CRUD operations
(create-user, update-user, etc). In fact, I had only three general functions to
create, update and read for something, and the entity-specific functions became
just partials on them.

Having that, I could quickly switch from SQLite-powered backed to Datomic.

[datomic-query]: http://grishaev.me/en/datomic-query

The only think I've got stuck on was applying optional filters to a query. That
became a reason to write ["Conditional Queries in Datomic"][datomic-query]
article.

[db-master]: https://github.com/igrishaev/highloadcup/blob/master/src/highloadcup/db.clj
[db-sqlite]: https://github.com/igrishaev/highloadcup/blob/sqlte/src/highloadcup/db.clj

You may examine Datomic database backend in [master branch][db-master] whereas
SQLite version lives in a [self-titled branch][db-sqlite].

### JSON validation

A system that tests you server tends to send incorrect data. If you accept it
without returning `400 Bad Request` status you will get penalty score. So the
validation is a major part of our application.

[schema]: https://github.com/plumatic/schema/

Before, I used [Schema][schema] module for that purpose. I know it well
including some of its shadowed parts. But having Clojure 1.9 on board was a
great chance to try [clojure.spec][spec] that is still in alpha but works great.

After some REPL experiments, I ended up with my own `highloadcup.spec` namespace
that carried wrappers around the original spec. One of them is `validate`
function that does the following:

1. validates the data against a spec;
2. coerces string numbers into integers when needed;
3. returns `nil` when the data is invalid.

Its code is

~~~clojure
(ns highloadcup.spec
 (:require [clojure.spec.alpha :as s]))

(def invalid :clojure.spec.alpha/invalid)

(defn validate [spec value]
 (let [result (s/conform spec value)]
  (when-not (= result invalid)
   result)))
~~~

Pay attention it's a good practice to declare `invalid` constant at the
top. Once the library becomes stable, its namespace will get rid of "alpha".

Another point, spec was designed to be used with full-qualified keys. But in my
case, all the keys were without namespaces. That's normal for non-Clojure
applications. Declare your specs as usual, but once you compose a map of them,
pass `:opt-un` parameter (stands for "unqualified"):

~~~clojure
(def enum-gender #{"m" "f"})

(s/def :user/id int?)
(s/def :user/email string?)
(s/def :user/first_name string?)
(s/def :user/last_name string?)
(s/def :user/gender enum-gender)
(s/def :user/birth_date int?)

(s/def :user/create
 (s/keys :req-un [:user/id
                  :user/email
                  :user/first_name
                  :user/last_name
                  :user/gender
                  :user/birth_date]))
~~~

This is a spec for creating a user where every field is required. For updating a
user, there is a similar spec with all the fields optional:

~~~clojure
(s/def :user/update
 (s/keys :opt-un [:user/email
                  :user/first_name
                  :user/last_name
                  :user/gender
                  :user/birth_date]))
~~~

When applying query string filters, they are all plain strings even when
represent numbers. Turning them to the proper type is also knowing as
coercion. To coerce a string value during validation, use `conformer`:

~~~clojure
(defn x-integer? [x]
 (if (integer? x)
  x
  (if (string? x)
   (try
    (Integer/parseInt x)
    (catch Exception e
     invalid))
   invalid)))

(def ->int (s/conformer x-integer?))

(s/def :opt.visits/fromDate ->int)
(s/def :opt.visits/toDate ->int)
(s/def :opt.visits/country string?)
(s/def :opt.visits/toDistance ->int)

(s/def :opt.visits/params
 (s/keys :opt-un [:opt.visits/fromDate
          :opt.visits/toDate
          :opt.visits/country
          :opt.visits/toDistance]))
~~~

Now, then you validate parameters taken from the Ring request against
`:opt.visits/params` spec, all the numbers represented with strings will be
turned into integers as well.

### Docker

Let's talk a bit about building a Docker container. I don't see any reason to
compile uberjar inside Docker. It's Java so that it is "compiled once, works
everywhere" (usually I'm sceptical on that point, but not now). All you need is
to copy an uberjar file into container and setup CMD properly.

[docker-java]: https://hub.docker.com/_/java/
[docker-frolvlad]: https://hub.docker.com/r/frolvlad/alpine-oraclejdk8/

Do not use the official [Java template][docker-java]. Under the hood, it ships
OpenJDK that is 1.5 times slower than OracleJDK, unfortunately. So I had to
inherit my image from [Vlad Frolov's one][docker-frolvlad]. I know that's
illegal to distribute Java runtime as a part of your application. But the
difference in score was more important these days.

[Dockerfile]: https://github.com/igrishaev/highloadcup/blob/master/Dockerfile

JVM flags also could help to tweak performance. The web pages I've found
googling for "java docker" said that Java has troubles detecting heap size when
running in Docker. So at least `"-Xmx"` and `"-Xms"` should be specified. Next
two, `"-da"` and `"-dsa"` reduce all the assert statements. See the rest of
flags in my [Dockerfile][Dockerfile].

[Makefile]: https://github.com/igrishaev/highloadcup/blob/master/Makefile

All the project duties (lein, docker, files) should be automated with good
old [Make][Makefile] utility. Ideally, the default target should build the
entire project from scratch.

### Acknowledgments

[mailru]: https://mail.ru/

I want to thank [Mail.ru][mailru] team who were responsible for handling that
Cup. They've done really huge amount of work. I thought they didn't sleep at
all. During the three weeks term, they've been answering all the questions in
chat, fixing the infrastructure, solving bugs, writing wiki and adapting the
official website.

Thank you guys, your are real professionals!

**PS** to my Russian readers: you are welcome to share that post with your
foreign colleagues. Next time, let them join Highload Cup too!
