---
layout: post
title:  "In-Memory SQLite Database In Clojure"
permalink: /en/clj-sqlite
categories: clojure sqlite jdbc
lang: en
---

Recently, I've been working with a SQLite database using Clojure. In this post,
I'd like to share my experience that I've got from that challenge.

SQLite is a great tool used almost everywhere. Browsers and mobile devices use
it a lot. A SQLite database is represented by a single file that makes it quite
easy to share, backup and distribute. It supports most of the production-level
databases' features like triggers, recursive queries and so on.

In addition, SQLite has a killer feature to be run completely in memory. So,
instead of keeping an atom of nested maps, why not to store some temporary data
into well organized tables?

JDBC has SQLite support as well, you only need to install a driver, the
documentation says. But then, I've got a problem dealing with in-memory
database:

~~~clojure
(def spec
  {:classname   "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname     ":memory:"})

(jdbc/execute! spec "create table users (id integer)")
(jdbc/query spec "select * from users")

> SQLiteException [SQLITE_ERROR] SQL error or missing database
> (no such table: users)  org.sqlite.core.DB.newSQLException (DB.java:909)
~~~

What the... I've just created a table, why cannot you find it? An interesting
note, if I set a proper file name for the `:subname` field, everything works
fine. But I needed a in-memory database, not a file.

After some hours of googling and reading the code I've found the solution.

The thing is, JDBC does not track DB connections by default. Every time you call
for `(jdbc/*)` function, you create a new connection, perform an operation and
close it. For such persistent data storages like Postgres or MySQL that' fine
although not effective (in our project, we use HikariCP to have a pool of open
connections).

But for in-memory SQLite database, closing a connection to it leads to wiping
the data completely out form the RAM. So you need to track the connection in
more precision way. You will create a connection by yourself and close it when
the work is done.

First, let's setup your project:

~~~clojure
:dependencies [...
               [org.xerial/sqlite-jdbc "3.20.0"]
               [org.clojure/java.jdbc "0.7.0"]
               [com.layerware/hugsql "0.4.7"]
               [mount "0.1.11"]
               ...]
~~~

and the database module:

~~~clojure
(ns project.db
  (:require [mount.core :as mount]
            [hugsql.core :as hugsql]
            [clojure.java.jdbc :as jdbc]))
~~~

Declare a database URI as follows:

~~~clojure
(def db-uri "jdbc:sqlite::memory:"
~~~

Our database shares two states: when it's been set up and ready to work and when
it has not. To keep the state, let's use `mount` library:

~~~clojure
(declare db)

(defn on-start []
  (let [spec {:connection-uri db-uri}
        conn (jdbc/get-connection spec)]
    (assoc spec :connection conn)))

(defn on-stop []
  (-> db :connection .close)
  nil)

(mount/defstate
  ^{:on-reload :noop}
  db
  :start (on-start)
  :stop (on-stop))
~~~

Once you call `(mount/start #'db)`, it becomes a map with the following fields:

~~~clojure
{:connection-uri "jdbc:sqlite::memory:"
 :connection <SomeJavaConnectionObject at 0x0...>}
~~~

When any JDBC function or a method accepts that map, it checks for the
`:connection` field. If it's filled, JDBC uses that connection as well. If it's
not, a new connection is issued. In my case, every execute/query call created a
new in-memory database and stopped it right after the call ends. That's why the
second query could not to find `users` table: because it was performed within
another database.

Now with the `db` started, you are welcome to perform all the standard `jdbc`
operations:

~~~clojure
(jdbc/execute! db "create table users (id integer, name text))")
(jdbc/insert! db :users {:id 1 :name "Ivan"})
(jdbc/get-by-id db :users 1) ;; {:id 1 :name "Ivan"}
(jdbc/find-by-keys db :users {:name "Ivan"}) ;; ({:id 1 :name "Ivan"})
~~~

Finally, you stop the db calling `(mount/stop #'db)`. The connection stops, the
data disappears completely.

For more complicated queries with joins, HugSQL library would be a good
choice. Create a file `queries.sql` in your `resources` folder. Say, you want to
write a complex query that filters a result by some values that probably are not
set. Here is an example of what you should put into `queries.sql` file:

~~~sql
-- :name get-user-visits :?
select
    v.mark,
    v.visited_at,
    l.place
from visits v
join locations l on v.location = l.id
where
    v.user = :user_id
    /*~ (when (:fromDate params) */
    and v.visited_at > :fromDate
    /*~ ) ~*/
    /*~ (when (:toDate params) */
    and v.visited_at < :toDate
    /*~ ) ~*/
    /*~ (when (:toDistance params) */
    and l.distance < :toDistance
    /*~ ) ~*/
    /*~ (when (:country params) */
    and l.country = :country
    /*~ ) ~*/
order by
    v.visited_at;
~~~

In your database module, put the following on the top level:

~~~clojure
(hugsql/def-db-fns "queries.sql")
~~~

Now, every SQL template has become a plain Clojure function that takes a
database and a map of additional parameters. To get all the visits in our
application, we do:

~~~clojure
(get-user-visits db {:user_id 1 :fromDate 123456789 :country "SomePlace"})
> ...gives a seq of maps...
~~~

A list of dependencies used in that example:

~~~clojure
[org.xerial/sqlite-jdbc "3.20.0"]
[org.clojure/java.jdbc "0.7.0"]
[mount "0.1.11"]
~~~

Hope this article will help those who've got stuck with SQLite.
