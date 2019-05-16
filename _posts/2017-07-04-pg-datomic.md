---
layout: post
title:  "Migration from Postgres to Datomic"
permalink: /en/pg-to-datomic/
tags: clojure migration postgres datomic database
lang: en
---

[queryfeed]: https://queryfeed.net/

[clojure-true-brave]: http://www.braveclojure.com/

Recently, I migrated my Clojure-driven pet project from PostgreSQL to
Datomic. This is [Queryfeed][queryfeed], a web application to fetch data from
social networks. I've been running it for several years considering it as a
playground for some experiments. Long ago, it was written in Python, then I
ported it to Clojure.

[euler]: https://projecteuler.net/

It was a great experience when I just finished
reading ["Clojure for True and Brave"][clojure-true-brave] book and was full of
desire to apply new knowledge to something practical rather than
solving [Euler problems][euler] in vain.

[cognitect]: https://cognitect.com

[clojure-tv]: https://www.youtube.com/user/ClojureTV

This time, I've made another effort to switch the database backend to
Datomic. Datomic is a modern, fact-driven database developed
in [Cognitect][cognitect] to be used in conjunction with Clojure. It really
differs for classical RDBS such as MySQL or PostgreSQL. For a long time, I've
been thinking whether I should try it. Meanwhile, more and more Clojure/conj
talks have been publishing on [YouTube][clojure-tv]. At my work, we use vast
PostgreSQL database and the code base is tied to close to it. There is no an
option to perform a switch on weekends. So I decided to port my pet project to
Datomic in my spare time.

Surely, before doing this, I googled for a while and was really wondered about
how few information I found on the Internet. There were just three posts that
did not cover the subject in details. So I decided to share my experience
here. Maybe it would help somebody with their migration duties.

Of cause, I cannot guarantee the steps described below will meet your
requirements as well. Each database is different, so it's impossible to develop
a final tool that could handle all the cases. But at least you may borrow some
of those.

## Table of Contents
{:.no_toc}

* dummy
{:toc}

## Introduction

Before we begin, let's talk about what is the reason to switch to Datomic. That
question cannot be answered just in one or two points. Before Datomic, I've been
working with PostgreSQL for several years and reckon it as a great
software. There is no such a task that Postgres cannot deal with. Here are just
some of them:

- streaming replication, smart sharding;
- geo-spatial data, PostGIS;
- full-text search, trigrams;
- JSON(b) data structures;
- typed arrays;
- recursive queries;
- and tons of other benefits.

So if Postgres is really so great, why switching then? In my point of view, it
brings the following benefits into a project:

1. Datomic is simple. In fact, it has only two operations: read (querying) and
   write (transaction).
2. It supports joins as well. Once you have a reference, it can be resolved into
   a nested map. References may be recursive. In PostgreSQL or any other RDBS,
   you always have a plain result with possibly duplicated rows. The ORM logic
   that may deal with parsing raw SQL response might be too complicated to
   understand.
3. Datomic was developed in the same terms as Clojure was. These are simplicity,
   immutability and declarative style. Datomic shares Clojure's values.
4. It accumulates changes through time like Git or any other control version
   system. With Datomic, you may always roll-back in time to get a history of an
   order or collect audit logs.

Let's highlight some general steps we should pass through to complete the
migration. These are:

- dump you Postgres data;
- add Datomic into your project;
- load the data into Datomic;
- rewrite the code that operates on data;
- rewrite your HTML templates;
- update or add unit tests;
- remote JDBC/Postgres from your project;
- setup infrastructure (backups, console, etc)

As you see, it is not as simple as it could be thought even for a small
project. In my case, migrating Queryfeed took about a week working by nights. It
includes:

[docs]: http://docs.datomic.com/

- two days to read the whole [Datomic documentation][docs];
- one day to migrate the data;
- two days to fix the business logic code and templates;
- two days to deploy everything to the server.

Regarding to the documentation, I highly recommend you to read it first before
doing anything. Please do not rely on random Stack Overflow snippets. Datomic is
completely different than classical SQL databases, so your long-term Postgres or
MySQL experience won't work.

[kindle-send]: https://chrome.google.com/webstore/detail/send-to-kindle-for-google/cgdjpilhipecahhcilnafpblkieebhea?hl=en

Quick tip here, since it could be difficult to read lots of text from a screen,
I just download any page I wish to read into my Kindle using the official
Amazon [extension for Chrome][kindle-send]. The paper appears on my Kindle in a
minute and I read it.

Once you've finished with the docs, feel free to the next step: dumping your
PostgreSQL data.

## Dump Postgres database

Exporting you data into a set of files won't be so difficult I believe. I may
guess your project has `projectname.db` module that handles the most of database
stuff. It should have `clojure.java.jdbc` module imported and `*db*` or
`db-spec` variables declared. Your goal is for every table you have in the
database, run a query something like `select * from <table_name>` against it and
save the result into a file.

What file format to use depends on your own preferences, but I highly recommend
the standard `EDN` files rather than JSON, CSV or whatever. The main point in
favor of `EDN` is it handles extended data types such as dates and UUIDs. In my
case, every table has at least one date field, for example `created_at` that is
not null and is set with the current time automatically. When using JSON or
YAML, the dates will be just strings so you need to write extra code to restore
a native `java.util.Date` class from a string. So are unique identifiers, UUIDs.

In addition, since `EDN` files represent native Clojure data structures, you
don't need to add `org.clojure/data.json` dependency into your
project. Everything can be made with out-from-the-box functions. The next
snippet dumps all the users from your Postgres database into a `users.edn` file:

~~~clojure

(def *db* {... your JDBC spec map...})

(def query (partial jdbc/query db-spec))

(spit "users.edn" (with-out-str (-> "select * from users" query prn)))

~~~

And that is! With only one line of code, you've just dumped the whole table into
a file. Repeat it several times substituting a name of an `*.edn` file and a
table. If you have many tables, wrap it with a function:

~~~clojure
(defn dump [table]
  (spit (format "%s.edn" table)
    (with-out-str (-> (format "select * from %s" table)
                      query
                      prn))))
~~~

Then run it against a vector of table names but not a set since an order is
important. For example, if you have a user has a foreign key to `orders` table,
it should be loaded first.

To check whether your dump is correct, try to restore it from a file as follows:

~~~clojure
(-> "users.edn" slurp read-string first)
~~~

Again, it is so simple to perform such things in Clojure. Within one line of
code, you have just read the file, restored the Clojure data from it and took
the first map from a list. In REPL, you should see something like:

~~~clojure
{:id 1
 :name "Ivan"
 :email "test@test.com"
 ... other fields
 }
~~~

That means the dump step was done as well.

## Adding Datomic into your project

Here, I won't discuss on that step so long since it is highlighted as well in
the official documentation. Briefly, you need to:

[my-datomic]: https://my.datomic.com/
[lein-gpg]: https://github.com/technomancy/leiningen/blob/master/doc/GPG.md
[peer-lib]: http://docs.datomic.com/integrating-peer-lib.html
[run-transactor]: http://docs.datomic.com/run-transactor.html
[pg-setup]: http://docs.datomic.com/storage.html#sql-database

1. [register][my-datomic] on Datomic site, it is free;
2. set up your [GPG credentials][lein-gpg];
3. add Datomic repository and [the library][peer-lib] into your project;
4. (optional) if you use Postgres-driven backend for
   Datomic, [create a new Postgres][pg-setup] database using SQL scripts from
   `sql` folder. Then [run a transactor][run-transactor].

Below, here is a brief example of my setup:

~~~clojure
;; project.clj
(defproject my-project "0.x.x"
  ...
  :repositories {"my.datomic.com" {:url "https://my.datomic.com/repo"
                                   :creds :gpg}}

  :dependencies [...
                 [com.datomic/datomic-pro "0.9.5561.50"]
                 ...]
  ...)
~~~

Run `lein deps` to download the library. You will be probably prompted to input
your GPG key.

A quick try in REPL:

~~~clojure
(require '[datomic.api :as d])
(def conn (d/connect "datomic:mem://test-db"))
~~~

## Loading the data into Datomic

In this step, we will load the previously dumped data into your Datomic
installation.

First, we need to prepare the schema before loading the data. A schema is a
collection of attributes. Each attribute by itself is a small piece of
information, for example a `:user/name` attribute keeps a string value and
indicates a user's name.

An entity is a set of attributes linked together by system identifier. Thinking
in RDBS terms, an attribute is a DB column whereas an entity is a row of a
table. That really differs Datomic from such schema-less databases as MongoDB for
example. In Mongo, every entity may have any structure you wish even across the
same collection. In Datomic, you cannot write a string value into a number or a
boolean into a date. One note, an entity may own an arbitrary number of
attributes.

For example, in Postgres if you did not set default values for a column and it
is not null, you just cannot skip it when inserting a row. In Datomic, you may
submit as many attributes as you want when performing a transaction. Imagine we
have a user model with ten attributes: a name, email, etc. When creating a user,
I may pass only a name and there won't be an error. So pay attention you submit
all the required attributes.

Datomic schema is represented by native Clojure data structures: maps, keywords
and vectors. That's why they are stored in EDN files as well. A typical initial
schema for fresh Datomic installation may look as follows:

~~~clojure
[
 ;; Enums
 {:db/ident :user.gender/male}
 {:db/ident :user.gender/female}

 {:db/ident :user.source/twitter}
 {:db/ident :user.source/facebook}

 ;; Users

 {:db/ident       :user/pg-id
  :db/valueType   :db.type/long
  :db/cardinality :db.cardinality/one
  :db/unique      :db.unique/identity}

 {:db/ident       :user/source
  :db/valueType   :db.type/ref
  :db/cardinality :db.cardinality/one
  :db/isComponent true}

 {:db/ident       :user/source-id
  :db/valueType   :db.type/string
  :db/cardinality :db.cardinality/one}
 ...
]
~~~

The first four ones are special attributes that are proposed as enum values. I
will discuss more on them later.

[schema]: http://docs.datomic.com/schema.html

Again, check for the official documentation that
describes [schema usage][schema].

Now that we prepared a schema, let add some boilerate code in our `db`
namespace:

~~~clojure
(ns project.db
  (:require [clojure.java.io :as io]
            [datomic.api :as d]))

;; in-memory database for test purposes
(def db-uri "datomic:mem://test-db")

;; global Datomic connection wrapped in atom
(def *conn (atom nil))

;; A function to initiate the global state
(defn init-db []
  (d/create-database db-uri)
  (reset! *conn (d/connect db-uri)))

;; reads an EDN file located in `resources` folder
(defn read-edn
  [filename]
  (-> filename
      io/resource
      slurp
      read-string))

;; reads and loads a schema from EDN file
(defn load-schema
  [filename]
  @(d/transact @*conn (read-edn filename)))
~~~

I hope the comments highlight the meaning of the code as well. I just declared a
database URL, a global connection, a function to connect to the DB and two
helper functions.

The first function rust reads a `EDN` file and returns a data structure. Since
our files a stored in resources folder, there is a `io/resource` wrapper here in
the threading chain.

The second function also read a file but also performs a Datomic transaction
passing data as a schema.

The `db-uri` variable is represented with URL-like string. Currently, we use
in-memory storage for test purposes. I really doubt you can load the data
directly to SQL-driven storage without errors so let's just practice for a
while. Later, when the import step will be ready, we will just switch `db-uri`
variable to production-ready URL.

With the code above, we are ready to load the schema. I put my initial schema
into a file `resources/schema/0001-init.edn` so I may load it as follows:

~~~clojure
(init-db)
(load-schema "schema/0001-init.edn")
~~~

Now that we have a schema, let's load the previously saved Postgres data. We
need to add more boilerate code. Unfortunately, there cannot be a common
function that may map your Postgres fields into Datomic attributes. The
functions to convert your data might look a bit ugly, but they are
one-time-purpose only so please don't mind.

For each EDN file that contains data of a specific table, we should:

1. read a proper file, get a list of maps;
2. convert each PostgreSQL map into Datomic map;
4. perform Datomic transaction passing a vector of Datomic maps.

Below, here is an example of my `pg-user-to-datomic` function that accepts a
Postgres-driven map and turns it into a set of Datomic attributes:

~~~clojure
(defn pg-user-to-datomic
  [{:keys [email
           first_name
           timezone
           source_url
           locale
           name
           access_token
           access_secret
           source
           token
           status
           id
           access_expires
           last_name
           gender
           source_id
           is_subscribed
           created_at]}]

  {:user/pg-id id
   :user/email (or email "")
   :user/first-name (or first_name "")
   :user/timezone (or timezone "")
   :user/source-url (URI. source_url)
   :user/locale (or locale "")
   :user/name (or name "")
   :user/access-token (or access_token "")
   :user/access-secret (or access_secret "")

   :user/source (case source
                       "facebook" :user.source/facebook
                       "twitter" :user.source/twitter)

   :user/source-id source_id

   :user/token (UUID/fromString token)
   :user/status (case status
                  "normal" :user.status/normal
                  "pro" :user.status/pro)

   :user/access-expires (or access_expires 0)
   :user/last-name (or last_name "")
   :user/gender (case gender
                  "male" :user.gender/male
                  "female" :user.gender/female)

   :user/is-subscribed (or is_subscribed false)
   :user/created-at (or created_at (Date.))})
~~~

Yes, it looks ugly a bit annoying, but you have to write something like this for
every table your have.

Here is the code to load a table into Datomic:

~~~clojure
(->> "users.edn" slurp read-string (map pg-user-to-datomic) transact!)
~~~

Before we go further, let's discuss some important notes on importing the data.

### Avoid nils

Datomic does not support nil values for attributes. When you do not have a value
for an attribute, you should either skip it or pass an empty value: a zero, an
empty string, etc. That's why the most of expressions have `(or "")` at the end
of threading macro.

### Shrink your tables

Migrating to the new datastore backend is a good chance to refactor your
schema. For those who has spent years working with relational database it is not
a secret that typical SQL applications suffer from lots of tables. In SQL, it is
not enough to keep just "entities" tables: users, orders, etc. Often, you need
to associate a product with colors, a blog post with tags or a user with
permissions. That leads to `product_colors`, `post_tags` and other bridge
tables. You join them in a query to "go through" from a user to their orders,
for example.

Datomic is free from bridge tables. It supports reference attributes that are
linked to any other entity. In addition, each attribute may carry multiple
values. For example, if we want to link a blog post with a set of tags, we'd
rather declare the following schema:

~~~clojure
[
 ;; Tag

 {:db/ident       :tag/name
  :db/valueType   :db.type/string
  :db/cardinality :db.cardinality/one
  :db/unique      :db.unique/identity}

 ;; Post

 {:db/ident       :post/title
  :db/valueType   :db.type/string
  :db/cardinality :db.cardinality/one}

 {:db/ident       :post/text
  :db/valueType   :db.type/string
  :db/cardinality :db.cardinality/one}

 {:db/ident       :post/tags
  :db/valueType   :db.type/ref
  :db/cardinality :db.cardinality/many}
]
~~~

In Postgres, you will need `post_tags` bridge table with `post_id` and `tag_id`
foreign keys. In datomic, you simply pass a vector of IDs in `:post/tags` field
when creating a post.

Migrating to Datomic is a great chance to get rid of those tables.

### Use enums

Both Postgres and Datomic provide support of enum types. A enum type is a set of
values. An instance of enum type may have only one of those values.

In Postgres, I use enum types a lot. They are fast, reliable and provide strong
consistency of you data. For example, if you have an order with possible "new",
"pending" and "paid" states, please don't use `varchar` type for that. Somehow
you may write something wrong there, for example mix up the register or make a
misprint. So you'd better to declare the schema as follows:

~~~sql
create type order_state as enum (
  'order_state/new',
  'order_state/pending',
  'order_state/paid'
);

create table orders (
  id serial primary key,
  state order_state not null default 'order_state/new'::order_state,
  ...
);
~~~

Now you cannot submit an unknown state for an order.

Although Postgres enums are great, JDBC library makes our life a bit more
difficult by forcing us to wrap enum values into `PGObject` when querying or
inserting data. For example, to submit a new state for an order, you cannot just
pass a string `"order_state/paid"`. You'll get an error saying you are trying to
submit a string for `order_state` type column. So you have to wrap your string
into a special object:

~~~clojure
(defn get-pg-obj [type value]
  (doto (PGobject.)
    (.setType type)
    (.setValue value)))

(def get-order-state
  (partial get-pg-obj "order_state"))

;; now, composing parameters for a query
{:order_id 42
 :state (get-order-state "order_state/paid")}
~~~

Another disadvantage here is inconsistency between select and insert
queries. When you just read the data, you get the enum value as a string. But
when you pass a enum as a parameter, you still need to wrap it with
PGObject. That is a bit annoying.

Datomic also has nice support of enums. There is no a special syntax for
them. Enums are special attributes that do not have values but only
names. Above, I have already highlighted them:

~~~clojure
[
 {:db/ident :user.gender/male}
 {:db/ident :user.gender/female}

 {:db/ident :user.source/twitter}
 {:db/ident :user.source/facebook}
]
~~~

Later, you may reference a enum value passing just a keyword
`:user.source/twitter`. It's quite simple, fast and keeps your database
consistent.

### JSON data

Personally, I try to avoid using JSON in Postgres as long as it is
possible. Adding JSON fields everywhere turns your Postgres installation into
MongoDB. It becomes quite easy to make a mistake or corrupt the data and fall
into a situation when one half or your JSON data has a particular key and the
rest half does not.

[ipn]: https://developer.paypal.com/docs/classic/products/instant-payment-notification/

Sometimes you really need to keep JSON in your DB. A good example might
be [Paypal Instant Notifications][ipn]. These are HTTP requests that Paypal
sends to your server when a customer buys something. IPN's body keeps about 30
fields and its structure may vary depending on transaction type. Splitting that
data into separate fields and storing all of them across separate columns will
be a mess. A solution will be to fetch only the most sensible ones (date, email,
sum, order number) and write the rest data into a `jsonb` column. Then, once you
need to fetch any additional information from an IPN, for example a tax sum, you
may query it as well:

~~~sql
select
  data->'tax_sum'::numeric as tax
from
  ipn
where
  order_number = '123456';
~~~

In Datomic, there is no JSON type for attributes. I'm not sure I made a proper
decision, but I just put those JSON data into a text attribute. Sure, where is
no a way to access separate fields in a datalog query or apply roles to
them. But at least I can restore the data when selecting a single entity:

~~~clojure
;; local handler to parse JSON with keywords in keys
(defn parse-json [value]
  (json/parse-string v true))

(defn get-last-ipn [user-id]
  (let [query '[:find [(pull ?ipn [*]) ...]
                :in $ ?user
                :where
                [?ipn :ipn/user ?user]]

        result (d/q query (d/db @*conn) user-id)]

    (when (not-empty result)
      (let [item (last (sort-by :ipn/emitted-at result))]
        (update item :ipn/data parse-json)))))
~~~

### Foreign keys

In RDBS, a typical table has auto-incremental `id` field that marks a unique
number of that row. When you need to refer to another table, an order or a
user's profile, you declare a foreign key that just keeps a value for those
id. Since they are auto-generated, you should never bother on their real values,
but only consistency.

In Datomic, you do not have possibility to have auto-incremented values. When
you import your data, it's important to handle foreign keys (or references in
terms of Datomic) properly. During the import, we populate `:<entity>/pg-id`
field that holds the legacy Postgres value. Once you import a table with foreign
keys, you may resolve a reference as follows:

~~~clojure
{... ;; other order fields
 :order/user [:user/pg-id user_id]}
~~~

A reference attribute may be represented as vector of two where the first value
is an attribute name and the second is its value.

For new entities created in production after migration to Datomic, you do not
need to submit `.../pg-id` value. You may either delete it (retract) once the
migration process has been finished or just keep it in the database as an
indicator that marks legacy data.

## Update the code

This step would be the most boring, I believe. You need to scan the whole
project and fix those fragments where you access the data from the database.

Since it is a good practice to prepend attributes with a namespace, the most
common change would be attribute renaming I believe:

~~~clojure
;; before
(println (:name user))

;; after
(println (:user/name user))
~~~

You will face less problems by organizing special functions that wraps the
underlying logic. A good example might be to add `get-user-by-id`,
`get-orders-by-user-id` and so on.

[hugsql]: https://github.com/layerware/hugsql
[yesql]: https://github.com/krisajenkins/yesql

If you use [HugSQL][hugsql] or [YeSQL][yesql] Clojure libraries than you already
have such functions created dynamically from `*.sql` files. That is quite better
than having naked SQL everywhere. Porting such a project to Datomic will be much
easier.


## HTML templates

Another dull step that cannot be automated is to scan your Selmer templates (if
you have them in your project, of course) and to update those fragments where
you touch entities' attributes. For example:

~~~HTML
{% raw %}
;; before
<p>{{ user.first_name}} {{ user.last_name}}</p>

;; after
<p>{{ user.user/first-name}} {{ user.user/last-name}}</p>
{% endraw %}
~~~

You may access nested entities as well. Imagine a user has a reference to their
social profile:

~~~html
{% raw %}
<p>{{ user.user/profile.profile/name }}</p> ;; "twitter", "facebook" etc
{% endraw %}
~~~

Datomic encourages us to use enums which values are just keywords. Sometimes,
you need to implement `case...then` pattern in your Selmer template and render
any content depending on enum value. This may be a bit tricky since Selmer does
not support keyword literals. In the example above, a user has `:user/source`
attribute that references a enum with possible values `:user.source/twitter` or
`:user.source/facebook`. Here is how I figured out switching on them:

~~~html
{% raw %}
{% ifequal request.user.user/source.db/ident|str ":user.source/twitter" %}
  <a href="https://twitter.com/{{ user.user/username }}">Twitter page</a>
{% endifequal %}
{% ifequal request.user.user/source.db/ident|str ":user.source/facebook" %}
  <a href="{{ user.user/profile-url }}">Facebook page</a>
{% endifequal %}
{% endraw %}
~~~

In the example above, we have to turn a keyword into a string using `|str`
filter to compare both values as strings.

To find all the Selmer variables or operators in Selmer, just grep your
templates folder by `{% raw %}{{{% endraw %}` or `{% raw %}{%{% endraw %}`
literals.

## Remove JDBC/Postgres

Now that your project is Datomic-powered and does not need JDBC drivers anymore,
you may either remove them from the project or at least decrease them to the
`dev` dependencies needed only for development purposes.

Scan you project grepping it with `jdbc`, `postgres` terms to find those
namespaces that still use legacy DB backend. Remove any that still present. Open
your root `project.clj` file, remove `jdbc` and `postgresql` packages from
`:dependencies` vector. Ensure you may run and build the application and unit
tests as well.

## Update unit test

Datomic is a great tool in those aspect you may use in-memory backend when
running tests. That makes them pass quite faster and without needing setting up
Postgres installation on you machine.

[luminus]: http://www.luminusweb.net/

I believe your project is able to detect whether it is in `dev`, `test` or `prod`
mode. If it's not, take a look at [Luminus framework][luminus]. It's done quite
well in that meaning. For each type of environment, you specify its own database
URL. For test, it will be in-memory storage.

Using the standard `clojure.test` namespace, you wrap each test with a fixture
that does the following steps:

1. creates a new database in memory and connects to it;
2. runs all the schemas against it (migrations);
3. populates it with predefined test data (users, orders etc; also know as
   "fixtures");
4. runs the test itself
5. drops the database and closes and disconnects from it.

These steps should be run for each test. In that case, we can guarantee what
every test has its own environment and does not depend on other tests. It's a
good practice when a test accepts a fresh installation not being touched by
previous tests.

Some preparation steps are:

~~~clojure
(ns your-project.test.users
  (:require [clojure.test :refer :all]
            [your-project.db :as db]))

(defn migrate []
  "Loads all the migrations"
  (doseq [file ["schema/0001-init.edn"
                "schema/0002-user-updated.edn"]]
    (db/load-schema file))

(defn load-fixtures []
  "Loads all the fixtures"
  (db/load-schema "fixtures/test-data.edn"))

(defn test-fixture [f]
  (db/init) ;; this function reads the config,
            ;; creates the DB and populates
            ;; the global Datomic connection

  (migrate)
  (load-fixtures)
  (f)         ;; the test is run here
  (db/delete) ;; deletes a database
  (db/stop))  ;; stops the connection

(use-fixtures
  :each
  test-fixture)

~~~

Now you may write your tests as well:

~~~clojure

(deftest user-may-login
  ...)

(deftest user-proceed-checkout
  ...)
~~~

For every test, you will have a database running with all the migrations and
test data loaded.

If you still do not have any tests in your project, I urge you to add them
soon. Without tests, you cannot be sure you do not break anything when changing
the code.

## Infrastructure (final touches)

In the final section, I will highlight several important points that relate to
the server management.

### Setting up production Postgres-driven backend

Running in-memory Datomic database is fun since it really costs nothing. In
production, you would better set up more reliable backend. Datomic supports
Postgres storage system out from the box. To prepare the database, run the
following SQL scripts:

~~~bash
sudo su postgres # switch to postgres user
cd /path/to/datomic/bin/sql
psql < postgres-user.sql
psql < postgres-db.sql
psql datomic < postgres-table.sql
~~~

The scripts above create a user `datomic` with the password `datomic`, then the
database `datomic` with the owner `datomic`. The last script creates a special
table to keep Datomic blocks.

Please do not forget to change the standard `datomic` password to something more
complicated.

### Running the transactor

[transactor]: http://docs.datomic.com/run-transactor.html

The following page describes how to [run a transactor][transactor] needed by
peer library when you use non-memory data storage. I'm not going to retell it
here. Instead, I will share a bit of config to run it automatically using the
standard `init.d` Linux daemon.

Create a file named `datomic.conf` in your `my-project/conf` directory. Put a
symlink to `/etc/init.d/` folder that references that file. Add the following
lines into it:

~~~
description "Datomic transactor"

start on runlevel startup
stop on runlevel shutdown

respawn

setuid <your user here>
setgid <your group here>

chdir /path/to/datomic

script
    exec bin/transactor sql-project.properties
end script
~~~

There, `/path/to/datomic` is a directory where unzipped Datomic installation is
located. `sql-project.properties` is a transactor configuration file where you
should specify your Datomic key sent to your email.

Now that you have put a symlink, try the following commands:

~~~bash
sudo start datomic

status datomic
# datomic start/running, process 5281

sudo stop datomic
~~~

### Console

Most of RDBS have UI applications to manage the data. Datomic comes with
built-in console that is run as web application. Within those console, you can
examine the schema, perform queries and transactions.

The following template runs a console:

~~~bash
/path/to/datomic/bin/console -p 8088 <some-alias> <datomic-url-without-db>
~~~

In my example, the command is:

~~~bash

$(DATOMIC_HOME)/bin/console -p 8888 datomic \
"datomic:sql://?jdbc:postgresql://localhost:5432/datomic?user=xxxxx&password=xxxxx"
~~~

Opening a browser at `http://your-domain:8888/browser` will show you a
dashboard.

Some security issues may be mentioned here. The console does not support any
login/password authentication, so it is quite unsafe to run the console on
production server as-is. Implement at least some of the following steps:

1. Proxy the console with Nginx. It must not be reachable by itself.
2. Limit access by a list of IPs. These may be your office or your home only.
3. There should be only secure SSL connection allowed, no plain HTTP. Let's
   encrypt would be a great choice (see my [recent post](/en/letsencrypt)).
4. Add basic/digest authentication to your Nginx config.

To run a console as a service, create another `console.conf` file in
`/etc/init.d/` directory. Use the `datomic.conf` file as template. Substitute
the primary command with those one shown above. Now you can run the console only
when you really need it:

~~~bash
sudo start console
~~~

### Backups

[aws-s3]: https://aws.amazon.com/s3

Making backups regularly is highly important. Datomic installation carries a
special utility to take care of it. You won't need to make your backups manually
by running `pgdump` against Postgres backend. Datomic provides a high-level
backing up algorithm that performs in several threads. In addition, it
supports [AWS S3][aws-s3] service as a destination point.

A typical backup command looks as follows:

~~~shell
/path/to/datomic/bin/datomic -Xmx4g -Xms4g backup-db <datomic-url> <destination>
~~~

To access AWS servers, you need to export both `AWS_ACCESS_KEY_ID` and
`AWS_SECRET_KEY` variables first or prepend a command with them. In my case, the
full command looks something like:

~~~shell
AWS_ACCESS_KEY_ID=xxxxxx AWS_SECRET_KEY=xxxxxxx \
/path/to/datomic/bin/datomic -Xmx4g -Xms4g backup-db \
datomic:sql://xxxxxxxx?jdbc:postgresql://localhost:5432/datomic?user=xxxxxx&password=xxxxxxx" \
s3://secret-bucket/datomic/2017/07/04
~~~

The date part in the end is substituted automatically using `$(shell date
+\%Y/\%m/\%d)` expression in Makefile or the following in bash:

~~~bash
date_path=`date +\%Y/\%m/\%d` # 2017/07/04
~~~

Add that command into your crontab file to make backups regularly.

### Backups as a way to deploy the data

The good news are backup's structure does not depend on the backend type. No
matter you dump in-memory storage or Postgres cluster, the backup can be
restored everywhere as well. It gives us possibility to migrate the data on our
local machine, make a backup and then restore it into production database.

Once you finished migrating you data, launch the backup command described
above. The backup should go to S3. On the server, run the `restore` command:

~~~bash
AWS_ACCESS_KEY_ID=xxxxx AWS_SECRET_KEY=xxxxx
/path/to/datomic/bin/datomic -Xmx4g -Xms4g restore-db \
s3://secret-bucket/datomic/2017/07/04 \
"datomic:sql://xxxxxxx?jdbc:postgresql://localhost:5432/datomic?user=xxxx&password=xxxxx"
~~~

When everything is done without mistakes, the server will catch the new data.

## Conclusion

After spending about a week on moving from Postgres to Datomic I can say it
really worths it. Although Datomic does not support most of the Postgres smart
features like geo-spatial data or JSON structures, it is much closer to Clojue
after all. Since it was made by the same authors, Datomic looks like as a
continuation of Clojure. And that is a huge benefit that may overweight
disadvantages mentioned above.

Surfing the Internet, I found the next links that may also be helpful:

- [A Minor Victory with Datomic](http://jkk.github.io/minor-victory-datomic)
- [Datomic Setup](http://mcramm.com/post/datomic-setup/)
- [Using Datomic? Here’s How to Move from PostgreSQL to DynamoDB Local](https://blog.clubhouse.io/using-datomic-heres-how-to-move-from-postgresql-to-dynamodb-local-79ad32dae97f)

I hope you enjoyed reading this material. You are welcome to share your thoughts
in the commentary section.
