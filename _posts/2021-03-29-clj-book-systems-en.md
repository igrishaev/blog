---
layout: post
title:  "Systems in Clojure"
permalink: /en/clj-book-systems/
tags: clojure book programming systems
lang: en
---

{% include toc-clj-book-en.md %}

{% include toc.html id="clj-book-systems-en" title="In This Chapter" %}

*In this chapter, we will talk about systems, i.e., collections of interrelated components. We'll look at how large projects are assembled of small parts as well as how to overcome complexity and make all the parts work as one.*

The concept of a system is related to the configuration we discussed recently. The configuration answers the question of how to get the parameters, and the system knows how to use them.

Systems emerged when the demand for long-running applications arose. We don't require such things from scripts and utilities as they have a short runtime and a state that does not last long. When scripts and utilities finish their work, resources get released, so there is no point in monitoring them.

Things are different for server applications: they work all the time, and therefore, are designed differently than scripts. The application consists of components that run in the background. Each component performs its specific task. At startup, the app enables components in the correct order and builds connections between them.

<!-- more -->

## More Details on Systems

A component is a stateful object. It is affected by the "on" and "off" operations. As a rule, to enable a component means to open the resource, and to disable -- means to close it.

Typical application components are server, database, and cache. In order not to open a connection for every database request, you need a connection pool. We should not create it manually and pass it into JDBC functions. There should be a component that, when enabled, opens the pool and stores it. Such a component offers special methods for consumers to work with a database. Internally, the methods use an open pool.

At first glance, the scheme resembles OOP and encapsulation. But don't jump to conclusions: components in Clojure work differently. Below we will look at how objects differ from components.

## Dependencies

The main point of the system is component dependencies. A server, database, and cache are independent of each other. These are the base components of the system on which others, of a higher level, rely. Suppose a background thread reads the database and sends emails. It will be wrong if the component opens new connections to the database and email. Instead, it takes the enabled components and works with them like with a black box.

A system starts and stops components in the correct order. If component A depends on B and C, then by the time A starts up, the other two should be enabled. On completion, the B and C components must not be shut down as long as A is running because this will disrupt its operation. The system builds a dependency graph between components. The graph is traversed so as to satisfy all participants.

It should be easy to add a new component to a system. Ideally, the system is a combination of maps and lists.  The boot code runs through them and enables the components. Extending the system means adding a new node to the tree.

Once the system is aware of the dependencies, a subset of it can be launched. Let's say you need to debug an email handler that depends on a database and SMTP server. You don't need the web server and cache in this case, and starting the entire system is redundant. Advanced systems offer a feature with "run this component and its dependencies" semantics.

## Advantages

At first glance, it seems that the system only overcomplicates things. It will require a new library, team conventions, and refactoring. However, the inconvenience pays off over time.

The system puts the project in order. As the codebase grows, it becomes important that the components of a project are in the same style. If you follow this path, the service components will go to the libraries, and only the logic will remain in the project. It's easier to start a project with a tried-and-tested base of internal components.

Systems are useful at all stages, especially when testing. Tests run a system where some components work differently. For example, an SMS sender writes messages to a file or an atom. The authorization component reads the verification code from these sources. The approach does not guarantee complete reliability, but it will execute tests in isolation, without resorting to third-party services. We'll go into more detail about the problem of isolation in the chapter on tests.

# Preparing for the Overview

We've talked a little about systems in the chapter on mutable data. The method uses `alter-var-root` and global variables. The idea is to move the component into a module and provide the `start!` and `stop!` functions, which shift the module state. Starting the system comes down to calling them in the correct order.

That is an amateur solution because the system is not aware of the dependencies between the components. It is fragile, operates in manual mode, and each change requires verification.

Clojure offers several libraries for systems. We'll take a look at Mount, Component, and Integrant. These libraries differ in their approach to describing components and dependencies. So we will look at the problem from different angles.

We deliberately arranged the libraries in this order. Mount is the simplest of them, so let's start with it as an introduction to the topic. Component has become an industry standard. Let's pay more attention to it and therefore put it in the middle. Integrant completes our overview. We'll discuss it as an alternative to the Component library, which you will be familiar with by then.

Our system is similar to what you might come across in practice. It consists of a web server, a database, and a worker (a background task that updates records in the database). We added the worker specifically to help you learn how to work with dependencies. To better understand the system, let's draw its topology \fig{fig:chart-system}.

{:.code_chart}
~~~
           ┌────────────┐
           │   Config   │┼──────────────────┐
           └────────────┘                   │
                  ┼                         │
          ┌───────┴──────┐                  │
          │              │                  │
   ┌────────────┐  ┌────────────┐    ┌────────────┐
   │   Server   │  │  Database  │┼───│   Worker   │
   └────────────┘  └────────────┘    └────────────┘
~~~

Arrows indicate relationships between components. The expression A&rarr;B means "A depends on B." In our block diagram, all components are configuration dependent. The background worker additionally needs the database. We will work on this system until the end of the chapter.

## Database and Worker

We've mentioned previously that opening a connection for every request is suboptimal. In real projects, they work with the base through a pool. It is a stateful entity, so it is can be on and off as well.

Databases like SQLite and H2 store data in memory. This is convenient for a quick start, but does not reflect the realities of production, which is what we are aiming for in this book. For in-memory databases, connection pools are not used because the data is in memory, rather than in the network. We will work with the PostgreSQL relational database and the HikariCP pool.

The background process (aka worker) supplements the records in the database with information from the network. Let's say the company conducts an analysis of its website visits. When someone opens the page, the app stores the client's URL and IP address. To build reports by country and city, you need to get geodata by IP from third-party services. This operation is long, so the "in processing" flag is set for the database record and the logic is moved to the background.

## Docker

If you have PostgreSQL installed, create a new database and a table in it. If not, it's time to try Docker. It is a program for running applications from images. Here, an image is a package containing an app and everything needed for running. The running image is called a container.

Containers have several advantages. The application lives in an isolated environment and, therefore, is separated from the main system. Along with the safety problem, this solves the problem of cleanliness -- the container does not leave traces of working unless otherwise indicated.

Docker searches for images in an open repository where programs of different versions and bundles are published. If you specifically need PostgreSQL version 9.3, download the image with this tag. Installing this version on the host system will most likely result in a conflict with an already working database.

Some images can be configured with environment variables or files. At startup, a PostgreSQL image loads all `*.sql` files from the `/docker-entrypoint--initdb.d` folder. If we associate the folder to the local path with migrations, we'll get a ready-to-use database. At the same time, we did not write a single line of code but only specified the settings.

Docker includes the `docker-compose` utility. It runs a container from a config file. By default, the file is named `docker-compose.yaml`. It is a YAML document that specifies running options and the image. We have set up the `postgres` image and its options: the port, file paths, and environment variables.

~~~yaml
version: '2'
services:
  postgres:
    image: postgres
    volumes:
      - ./initdb.d:/docker-entrypoint-initdb.d
    ports:
      - 5432:5432
    environment:
      POSTGRES_DB: book
      POSTGRES_USER: book
      POSTGRES_PASSWORD: book
~~~

The `initdb.d` folder contains SQL files for starting the database. The `01.init.sql` file carries the `requests` table:

~~~sql
drop table if exists requests;
create table requests (
    id            serial primary key,
    path          text not null,
    ip            inet not null,
    is_processed  boolean not null default false,
    zip           text,
    country       text,
    city          text,
    lat           float,
    lon           float
);
~~~

Running `docker-compose up` brings up the PostgreSQL server on port 5432 with the `book` database. This is enough for further work. We will not talk about Docker anymore, as this is a topic for a separate book. You can find all details [on the project website](https://docker.com).

# Mount

The [Mount](https://github.com/tolitius/mount) library describes an entity with two states: starting and stopping. On command, the entity gets "on" and takes the value returned by the startup code. On shutdown, the stop code will work. The entity is like a global variable that changes on command. Mount is simple, so it's perfect for beginners. We will start the practical part with it.

## First Entity

The `defstate` macro defines a new entity. This macro is somewhat similar to `def` because it declares a variable in the current space. The difference is that `defstate` takes startup and shutdown code instead of a value.

Add the Mount dependency to the project. Let's describe the web server component using the macro. We'll put it in the `server.clj` module. The `app` function is a primitive application that returns the 200 status on all requests.

~~~clojure
;; project.clj
:dependencies [... [mount "0.1.16"]]

;; server.clj
(ns book.systems.mount.server
  (:require
   [mount.core :as mount :refer [defstate]]
   [ring.adapter.jetty :refer [run-jetty]]))

(def app (constantly {:status 200 :body "Hello"}))

(defstate server
  :start (run-jetty app {:join? false :port 8080})
  :stop (.stop server))
~~~

So far, we have only declared the state but have not turned anything on. If we execute `server`, we should see the following:

~~~clojure
#DerefableState[{:status :pending, :val nil}]
~~~

Execute `(mount/start)` to start the component. The function runs through the components and turns them on. The `(run-jetty ...)` expression under the `:start` key will return the server running in the background. Once started, your browser will show a greeting at `http://127.0.0.1:8080`. The `server` variable will become the `Server` instance from the `jetty` package:

~~~clojure
(type server)
;; org.eclipse.jetty.server.Server
~~~

To stop the system, execute `(mount/stop)`. Note that in the `(.stop server)` expression, the entity refers to itself. After stopping, `server` will become a value that signifies termination.

~~~clojure
(mount/stop)
(type server)
;; mount.core.NotStartedState
~~~

This is how the system is built. First, we find entities that run throughout the entire program. These are mainly network connections and background tasks. Then we declare a component with a start and stop logic.

## Relationship with Configuration

Above, we made a mistake by explicitly setting the server parameters in the component. We have already discussed why this is a bad thing; parameters must be in configuration. Since we have a system, let's move the config into a component.

The `config` entity is a lightweight configuration loader. For the sake of brevity, we will skip catching errors and other details. The `:start` phase reads the EDN file. Replace `edn/read-string` with Yummy, Aero, or your solution. The component doesn't need the `stop` phase because it is stateless.

~~~clojure
(defstate config
  :start
  (-> "system.config.edn" slurp edn/read-string))
~~~

The `system.config.edn` file contains a map, in which the key is the component name, and the value is parameters. Let's put the server under the `:jetty` group:

~~~clojure
{:jetty {:join? false :port 8088}}
~~~

Let's modify the server so that it depends on the config. Add the `config` component to the `require` list of the module:

~~~clojure
(ns book.systems.mount.server
 (:require
  [book.systems.mount.config :refer [config]]))
~~~

Let's rewrite the server to read the configuration:

~~~clojure
(defstate server
  :start
  (let [{jetty-opt :jetty} config]
    (run-jetty app jetty-opt))
  :stop
  (.stop server))
~~~

The result is a system of two components, where one depends on the other. Make sure that after calling `(mount/start)`, the server is working as expected.

## Databases

Let's prepare a database component. For this, we will need [JDBC](https://github.com/clojure/java.jdbc) and [HikariCP](https://github.com/tomekw/hikari-cp) libraries. The former offers access to relational databases. It is a set of functions that work almost the same for different engines. The following statements will read and write a user to PostgreSQL, MySQL, or Oracle:

~~~clojure
(jdbc/get-by-id db :users 42)
(jdbc/insert! db :users {:name "Ivan" :email "ivan@test.com"})
~~~

For each backend, JDBC builds a request taking into account its peculiarities.

JDBC functions take a JDBC spec as the first parameter. Usually, it is a connection map: the server address and port, the database name, the user, and the password. For each request, JDBC opens a connection, exchanges data, and closes the connection.

The spec may contain the `:datasource` key with a ready data source. In this case, JDBC ignores other keys and works directly with `:datasource`. HikariCP offers a function to build a source with a connection pool. Each time we request a connection from the source, we'll get one of the previously opened.

The source stores the state, so let's move it into the component. First, we prepare the `db.clj` module:

~~~clojure
(ns book.systems.mount.db
  (:require
   [mount.core :as mount :refer [defstate]]
   [hikari-cp.core :as cp]
   [book.systems.mount.config :refer [config]]))

(defstate db
  :start
  (let [{pool-opt :pool} config
        store (cp/make-datasource pool-opt)]
    {:datasource store})
  :stop
  (-> db :datasource cp/close-datasource))
~~~

The start code will return a JDBC spec -- a map with the `:datasource` key, inside which there is a pool. On terminate, the `close-datasource` function closes it along with all opened connections. Let's add the pool settings to the config:

~~~clojure
{:pool {:minimum-idle       10
        :maximum-pool-size  10
        :adapter            "postgresql"
        :username           "book"
        :password           "book"
        :database-name      "book"
        :server-name        "127.0.0.1"
        :port-number        5432}}
~~~

To save space, we will only indicate the basic parameters. These are connection properties (host, port, user, password) and the pool dimension. If desired, you can configure connection timings: idle time, connection time, and others. For a complete list of options, see the project's GitHub page.

Start the system and execute a request:

~~~clojure
(mount/start)
(require '[clojure.java.jdbc :as jdbc])
(jdbc/query db "select 1 as number")
;; ({:number 1})
~~~

If you got the right data, then both the database and the pool are ready for work.

## Background Task

Everything is ready for the last system component. It is a worker that runs on a separate thread. It selects records from the database that wait for processing and supplements them with fields from a third-party service.

The `requests` table stores the page address and IP of a client who has visited it. The `is_processed` flag indicates whether the entry has already been processed. The `city`, `country`, and other fields are `NULL` by default.

The worker cycle consists of the following steps:

-
  once per interval, select an entry with the `NOT is_processed` flag;

-
  request a service that will return geodata by IP;

-
  update a record in a transaction.

Let's express the worker in terms of Mount. The task runs in a separate thread, so we need a thread or future with an infinite loop. In order to stop the process on request, the loop must be conditional, with a flag check at each step. The flag is available both to the worker and to the one who controls it.

In Clojure, this is solved using a future and atom. The atom stores the flag that is a sign of the continuation of the loop. At every step, a future dereferences the atom, and if it is true, executes the task. To complete the future, take two actions. First, set the flag to false using `reset!`. Second, wait until the future becomes realized. In Clojure, this means that the executor has completed the task and responded to the future.

Let's prepare a worker module. You will need a config, database, logging, and HTTP client:

~~~clojure
(ns book.systems.mount.worker
  (:require
   [clojure.java.jdbc :as jdbc]
   [clj-http.client :as client]
   [clojure.tools.logging :as log]
   [book.systems.mount.db :refer [db]]
   [book.systems.mount.config :refer [config]]))
~~~

The worker is a map with the `:flag` and `:task` fields, an atom, and a future. The `:start` phase prepares this map. The `make-task` function does not exist yet, but we believe that it will return a future. In the `:stop` phase, the flag becomes false, and we wait for the future to stop.

~~~clojure
(defstate worker
  :start
  (let [{task-opt :worker} config
        flag (atom true)
        task (make-task flag task-opt)]
    {:flag flag :task task})
  :stop
  (let [{:keys [flag task]} worker]
    (reset! flag false)
    (while (not (realized? task))
      (log/info "Waiting for the task to complete")
      (Thread/sleep 300))))
~~~

The `:start` and `:stop` code should be small. Move technical steps into functions to make the code simpler. If you don't, the start and stop logic will be difficult to understand.

Let's add worker parameters to the EDN file. We need one field -- how many milliseconds to wait between processing records.

~~~clojure
{:worker {:sleep 1000}}
~~~

Let's describe the `make-task` function. It takes an atom with flag and EDN parameters. The function will return a future with a loop:

~~~clojure
(defn make-task
  [flag opt]
  (let [{:keys [sleep]} opt]
    (future
      (while @flag
        (try
          (task-fn)
          (catch Throwable e
            (log/error e))
          (finally
            (Thread/sleep sleep)))))))
~~~

The `task-fn` function executes the business logic of the application (line 7). We wrap it in `try/catch` to prevent the future from crashing. If we caught an exception, we write it to the log. There is a slight delay at the end of the iteration to avoid the flurry of database requests (line 11). If someone sets `flag` false, control will exit from `while` and the future will end.

Now let's describe the `task-fn` function. It reads one record from the database, which is awaiting processing. We are looking for geodata by IP using the `get-ip-info` function. We don't know yet how the search works, but we know that it will return a map with the `city`, `country`, and other fields.

~~~clojure
(defn task-fn []
  (jdbc/with-db-transaction [tx db]
    (when-let [request (first (jdbc/query tx query))]
      (let [{:keys [id ip]} request
            info   (get-ip-info ip)
            fields {:is_processed true
                    :zip (:postal_code info)
                    :country (:country_name info)
                    :city (:city info)
                    :lat (:lat info)
                    :lon (:lng info)}]
        (jdbc/update! tx :requests
                      fields
                      ["id = ?" id])))))
~~~

We moved the request to find a record to the `query` variable. This shortens the `task-fn` code. It is an SQL query with a `FOR UPDATE` statement locking the record to change in other connections.

~~~clojure
(def query
  "SELECT * FROM requests WHERE NOT is_processed
   LIMIT 1 FOR UPDATE;")
~~~

`FOR UPDATE` works only in a transaction, so the function body is wrapped in `(jdbc/with-db-transaction)`. It is a macro within which a transactional connection to the database is available. The `tx` symbol points to it. We're passing `tx`, not `db`, to the JDBC function (lines 3 and 12).

Let's write the `get-ip-info` function. It accesses a service that accepts an IP and returns information about it in JSON. In our case, this is the `iplocation.com` site. In high-performance systems, address databases are deployed locally, so they do not depend on third-party services.

~~~clojure
(defn get-ip-info [ip]
  (:body (client/post "https://iplocation.com"
                      {:form-params {:ip ip}
                       :as :json})))
~~~

If we call `get-ip-info` with the Berlin address, we get the following map:

~~~clojure
(get-ip-info "85.214.132.117")

{:city "Berlin"
 :region "BE"
 :country_code "DE"
 :country_name "Germany"
 :lat 52.5167
 :lng 13.4}
~~~

We have described the last element of the worker, and it is ready to go. Let's add a few records to the database and start the worker. After a while, we'll read these records again.

~~~clojure
(mount/start)

(jdbc/insert! db :requests {:path "/help" :ip "31.148.198.0"})
;; wait for a while

(jdbc/query db "select * from requests")
({:path "/help" :ip "31.148.198.0" :is_processed true
  :city "Pinsk" :zip "225710" :id 1
  :lon 26.0728 :lat 52.1214 :country "Belarus"})
~~~

The address `31.148.198.0` will resolve in Pinsk, the city of Belarus. The system is working properly.

## System Build

The components are ready and working separately; all that remains is to put them together. To do this, we'll write a central module, `core`, which will import all known components. The `(mount/start)` call from it will start the entire system. Let's see why this module is needed.

The `start` and `stop` functions work nothing but on components known to Mount. If you load the worker module, then Mount gets information about `worker`, `db`, and `config`, but not `server`, because no one refers to it in `require`. Therefore, the server module will not load, and the system will not know about this component. But if you import all the modules explicitly, it will solve the problem.

~~~clojure
(ns book.systems.mount.core
  (:require
   ;; other packages...
   [mount.core :as mount]
   book.systems.mount.config
   book.systems.mount.db
   book.systems.mount.server
   book.systems.mount.worker))

(defn start []
  (mount/start))
~~~

You may notice that specifying the `config` and `db` modules is optional. The compiler will load them automatically because `server` and `worker` refer to them. We have specified them for clarity, and we recommend doing the same, especially if you are just getting started with Clojure. The `core` module is a component registry: a mere glance at it is enough to understand how the project works. Indicate all components in it, without exception.

## Dependencies

The main system task is to traverse components in the correct order, taking into account the dependencies. Let's take a look at how Mount handles this.

We don't specify component dependencies when declaring it. `Worker` needs `config` and `db`, but this is not stated anywhere. When we call `(mount/start)`, the system guesses the startup order: `config` &rarr; `db` &rarr; `worker`. If you swap any two elements, it will fail. How does it work?

Mount relies on the Clojure compiler to compute the order. Namespaces depend on each other, like components in a system. The compiler looks for references to other modules in the `ns` body and loads them first. Let's remember what the worker's header looks like:

~~~clojure
(ns book.systems.mount.worker
  (:require
   [book.systems.mount.db :refer [db]]
   [book.systems.mount.config
     :refer [config]]))
~~~

Let's draw a graph of references:

{:.code_chart}
~~~

    ┌──────────────┐
    │ mount.worker │
    └──────────────┘
            │  ┌────────────┐
            ├─┼│  mount.db  │
            │  └────────────┘
            │  ┌───────────────┐
            └─┼│ mount.config  │
               └───────────────┘
~~~

The compiler will load `mount.worker` only after resolving the dependencies. It will start with the database module, which simplified definition is as follows:

~~~clojure
(ns book.systems.mount.db
  (:require
   [book.systems.mount.config :refer [config]]))
~~~

From the compiler's point of view, it looks like this:

{:.code_chart}
~~~
    ┌──────────────┐
    │   mount.db   │
    └──────────────┘
            │  ┌───────────────┐
            └─┼│ mount.config  │
               └───────────────┘
~~~

Before loading the `db`, the compiler takes care of `config`. The latter is independent of other modules and will be loaded first. Then the compiler will return to `db` and load it. After that, it will rise to the `worker` level. The `db` module is ready; `config` is next on the list. We've loaded the configuration in the `db` stage. Clojure doesn't load a module twice, so the compiler skips it. The last step will load `worker`.

We have deduced the order of the namespaces: `config`, `db`, `worker`. Each `defstate` form is executed on the same queue. Therein lies a trick: the `defstate` call increments the internal Mount counter, and the component remembers this number. The `config`, `db`, and `worker` entities will be numbered 1, 2, and 3. Mount walks through components in ascending order to start the system and descending to stop it.

## Internal Structure

Mount stores information about components in private atoms. They are hidden from consumers, but Clojure allows you to reach them. When the components are loaded, run:

~~~clojure
(def _state @@(resolve 'mount.core/meta-state))
~~~

The `_state` variable will contain a component map. The double `@` operator plays the following role. The `resolve` function will return a `Var` object by a symbol. Earlier, we found out that the `Var` object is a container that stores a value. The first `@` retrieves a value from `Var`; it is an atom with a map. The second `@` gets the map from the atom.

The map's key is a text link to the component. In our case, it is `#'book.systems.mount.config/config`. The value is a nested map with the state of the component. We are interested in the `order` field, i.e., its number. Let's sort the components by it and get the correct order:

~~~clojure
(->> _state
     vals (sort-by :order)
     (map (fn [cmp]
            (-> cmp :var meta :name))))

;; (config server db worker)
~~~

The `running` atom holds a map of started components with a similar structure:

~~~clojure
@@(resolve 'mount.core/running)
~~~

The `state-seq` atom stores a global component counter. To read it, run the following:

~~~clojure
@@(resolve 'mount.core/state-seq) ;; 4
~~~

Since our components (server, database, and others) have occupied values from 0 to 3, we get 4.

When working with Mount, you should not edit its contents. The examples above are needed so that you better understand the structure of this library.

## State

The ease of changing a component when calling `start` and `stop` is like magic. Here's the secret behind `defstate`: it works using the `alter-var-root` function, which we covered in the chapter on mutability. Let's remember the server's component:

~~~clojure
(defstate server
  :start
  (let [{jetty-opt :jetty} config]
    (run-jetty app jetty-opt))
  :stop
  (.stop ^Server server))
~~~

The `defstate` form expands  into multiple expressions. These are the global variable with no value:

~~~clojure
(def server)
~~~

and anonymous start and stop functions. The function bodies will get the code from the `:start` and `:stop` keys.

~~~clojure
(fn [] ;; start
  (alter-var-root #'server
   (fn [_]
     (let [{jetty-opt :jetty} config]
       (run-jetty app jetty-opt)))))

(fn [] ;; stop
  (alter-var-root #'server
   (fn [_]
     (.stop ^Server server))))
~~~

Mount places references to these functions in the `meta-state` atom. To start a component, you need to find a start function in the map and call it. The function will assign a new value to the `#'server` variable. Stopping works the same way.

## Selective Startup

So far, we have run the entire system. The call `(mount/start)` without parameters runs through the `meta-state` and enables all components. This is not always convenient. Let's say we are working on a worker and would like to run only it and its dependencies. In this case, we do not need a webserver.

To start only the required components, their references are passed to the `mount/start` function. Note: This function expects precisely a reference, a `Var` object.

~~~clojure
(mount/start
  #'book.systems.mount.config/config
  #'book.systems.mount.db/db
  #'book.systems.mount.worker/worker)
~~~

If you pass a value without `#'`, Mount will not start the component. The relationship between a variable and a value works one-way: the former cannot be found using the latter. In the example below, there will be no error; nothing will happen:

~~~clojure
;; does nothing
(mount/start
  book.systems.mount.config/config
  book.systems.mount.db/db
  book.systems.mount.worker/worker)
~~~

Newbies are confused by the fact that the function expects `Var`, not a value. This is not obvious since variables are rarely used in Clojure.

With a manual startup, you are responsible for the order of the components. Let's say you forgot that the database and the worker need a configuration:

~~~clojure
(mount/start
  #'book.systems.mount.db/db
  #'book.systems.mount.worker/worker)
~~~

We'll get a weird exception. It will be thrown in the `db` component where the pool is created. The `config` object is not running, and the expression `(:pool config)` will return `nil`. If we try to create a pool from nil, we get `NPE`.

As the system grows, it becomes more difficult to track dependencies. Mount only knows the order of the components, not how they are related. This is the weak point of this library -- programmers have to specify components manually to enable the subsystem. To improve this scenario, the library offers component selectors. These are functions that return references to components by some hallmark.

For example, the `except` selector will return component names other than those listed. If we pass the result into `start`, we get a system without the specified components. Below, a subset is enabled without a web server:

~~~clojure
(-> [#'book.systems.mount.server/server]
    mount/except
    mount/start)
~~~

For other selectors and their combinations, see the project's GitHub page. In addition to selectors, Mount offers functions to invert the system: to enable what is not working and vice versa.

## Reboot problem

When working with a project, we connect to it from the editor via the REPL. To make the Lisp machine state match the code, we execute the changed sections on the server. Question: what happens if you fix a component that is already running? How will Mount react to module reboots?

If you are using Emacs and CIDER, connect to the project via `M-x cider-connect`. Start the system as we did above. Open the server module and execute `M-x cider-eval-buffer` (or with keyboard buttons `C-c C-k`). The command will execute the file on the server. All definitions, including `ns`, `def`, and `defstate`, will trigger again.

A message will appear saying that the server rebooted. The `defstate` macro checks if a component with that name exists and is running. Mount will stop the component and start it with new versions of `start` and `stop`.

Rebooting is not always the desired behavior. With frequent changes, "out of sync" occurs -- a situation when a component is considered disabled, but its resource is busy. For example, in the `:stop` block, we did not call the `(.stop)` method. If we restart such a component, we will get an exception saying the port is busy.

The component's response to reloading is indicated in metadata. It is the `:on-reload` field, which by default is `:restart`. With it, the component restarts itself when `defstate` is called again. If you set `:stop`, the component will stop. The `:noop` key means "do nothing." The component with metadata looks like this:

~~~clojure
(defstate
  ^{:on-reload :noop}
  server
  :start (run-jetty app {:join? false :port 8080})
  :stop (.stop server))
~~~

If unsure, choose `noop`; with it, loading code into the REPL has no side effects. Changes to the code are not always component-related: there may be a typo or a comment. If you modified the component exactly, restart it manually.

## Independent Work

Let's go back to the `get-ip-info` function from the worker module. For each call, it makes an HTTP request. At a low level, we open a TCP connection, work with it, and close it. That is not optimal, and the problem is solved as with databases by using a connection pool. Examine the following example from the [Clj-http](https://github.com/dakrone/clj-http) library:

~~~clojure
(require
 '[clj-http.conn-mgr :refer
   [make-reusable-conn-manager
    shutdown-manager]])

;; create a new pool
(def cm (make-reusable-conn-manager
         {:timeout 2 :threads 3}))

;; make a request within the pool
(client/get "http://example.org/"
            {:connection-manager cm})

;; shut down the pool
(shutdown-manager cm)
~~~

Write a component that sends requests through the pool. Component parameters (timing, number of threads) come from the config. At the start, the component opens the pool and closes it upon stopping. Modify the worker to depend on the new component.

# Component

The [Component](https://github.com/stuartsierra/component) library also describes a system and components. It is a small framework where the main thing is the idea, rather than the code amount. The design of the Component library is fundamentally different from Mount, which we have already discussed.

Here, as in Mount, the `start` and `stop` operations act on a component. They return a *copy* of the object in a new state; the original component remains unchanged. You can say that components are immutable. This cuts off errors related to global variables.

A system is a map of components with dependencies. Initially, the system is at rest; its components are not running. The startup code walks through and enables them. The result is a working copy of the system. It is similar to the original one, but each component has been replaced with its started version. Stopping acts the same way: the output will be a disabled copy of the system.

Global state is illegal in Component. The library has no hidden atoms to account for components. One component can only access another if they are dependent. Like a system, a component avoids atoms and other mutable types. For each action, it generates a new copy of itself.

## How It Works

A component is an object that implements the `Lifecycle` protocol. The protocol includes the `start` and `stop` methods. A typed map that is declared in the `defrecord` form can be a component. They are also called “typed records” or “records”.

A record differs from a map in that it lists keys in advance. These are called slots of the record. Slots work faster than the keys of a regular map. The component needs them for input parameters and state.

Records and protocol fit together at the language level. When declaring a record, you can immediately expand it with the protocol. In protocol methods, slots are available as local variables. This way, a programmer can reduce the amount of code and save time.

A component hides its state, and only it knows how to control it. It would be a mistake to read its slots and pass them to functions. Instead, use the methods included in the component protocol. In Component, code is somewhat similar to OOP: it is an object and a set of operations. Like a class, you can initiate, start, and stop a component.

The difference is that the components are immutable. The transition to a new stage will not affect the old component, while in classical languages, its fields would be rewritten. The `SOLID` principle and the encapsulation, inheritance, polymorphism triple do not have the same power in Clojure. Most of them are unnecessary. When programming in Clojure, we might not adhere to OOP postulates.

## The First Component

Let's rewrite our system from the previous section with Component. We'll start with a web server. In the `server.clj` file, declare the namespace:

~~~clojure
(ns book.systems.comp.server
  (:require
   [com.stuartsierra.component :as component]
   [ring.adapter.jetty :refer [run-jetty]]))
~~~

A component is a record with two slots -- `:options` and `:server`. Options contain the Jetty server's parameters, and `server` contains its instance. The `component/Lifecycle` line stands for the protocol that implements the record. Below is the implementation of the protocol.

~~~clojure
(defrecord Server [options server]

  component/Lifecycle

  (start [this]
    (let [server (run-jetty app options)]
      (assoc this :server server)))

  (stop [this]
    (.stop server)
    (assoc this :server nil)))
~~~

The `start` method will return the same record, but with the full `:server` slot. It contains the server object. The `stop` method takes a running component. It terminates the server and returns another record in which the `:server` slot is `nil`.

Inside the methods, we refer to slots as local variables. This only works if the methods are declared inside `defrecord`. If you extend the record with a separate step, for example, via `extend`, access to the slots will be lost. In this case, you have to extract them from the `this` variable, for example:

~~~clojure
(stop [{:as this :keys [server]}]
  (.stop server))
;; or
(stop [this]
  (.stop (:server this)))
~~~

The `Server` entity is not a component yet but just an abstract description. In the first step, it is initiated, that is, an instance of it is created. To do this, use the `map-><Record>` function, where `<Record>` is the name of the record. The `defrecord` macro automatically creates this function in the same namespace. In our case, it is named `map->Server`. The function takes a regular map and returns its typed version. The keys of a map correspond to the record slots. If the key does not exist, the slot is `nil`.

~~~clojure
(def s-created
  (map->Server {:options {:port 8080 :join? false}}))
~~~

The `s-created` variable is an instance of the `Server` record. We specified the `options` slot, but not the `server` one because it will be filled out later. Now pass the result into the `start` method:

~~~clojure
(def s-started (component/start s-created))
~~~

This expression will return the running component. Open your browser at `http://127.0.0.1:8080` and check if the server responds. The `s-started` record has the `:server` slot filled:

~~~clojure
(-> s-started :server type)
;; org.eclipse.jetty.server.Server
~~~

Stop the component. Make sure that the page no longer opens and the server slot is `nil`.

~~~clojure
(def s-stopped (component/stop s-started))
(:server s-stopped) ;; nil
~~~

We went through the full life cycle of the component: preparation, start-up, and shutdown. The transition to each stage gives a new component. The `s-created`, `s-started`, and `s-stopped` variables keep the history of component stsates. In practice, the components are not manually controlled; the system controls them. We will know soon how exactly.

## Constructor

Let's recall how we created the `Server` instance.

~~~clojure
(map->Server {:options {:port 8080 :join? false}})
~~~

This expression has a drawback: you need to remember which slots are for initialization and which are for internal state. This is not critical for simple records, but, in practice, there are components with ten or more slots. To avoid confusion, declare a constructor function.

The constructor takes only arguments needed for the component initialization. In our case, it's `options`, so the function looks like this:

~~~clojure
(defn make-server
  [options]
  (map->Server {:options options}))

(def s-created (make-server {:port 8080 :join? false}))
~~~

The constructor makes it easier to create the component: it is impossible to pass something superfluous to `map->Server`. A constructor is a function; so you can add documentation and spec to it. An advanced editor will suggest a signature at the place of a call. We recommend you to write a constructor even for trivial components.

## Slots feature

When the server stops, we do two things: we call the method `(.stop)` and replace the slot with `nil`. Why not replace `assoc` with `dissoc`? Why store `nil` when you can detach a field?

~~~clojure
;; why this?
(assoc this :server nil)

;; but not this?
(dissoc this :server)
~~~

The reason is in the way records and slots are arranged. A record retains its unique properties as long as its slots are in place. If we take a slot away from a record via `dissoc`, we get a regular map. Let's show this with an example:

~~~clojure
(-> s-stopped
    (assoc :server nil)
    type)
;; book.systems.comp.server.Server
;; (still a record)

(-> s-stopped
    (dissoc :server)
    type)
;; clojure.lang.PersistentArrayMap
;; (a plain map)
~~~

If a component calls `dissoc` on itself -- we'll get a map instead of the component at a new stage. This leads to a strange behavior: if you pass the result of the `stop` method in `start` again, nothing will happen. Why? To answer this, we dive in some details related to classes in JVM.

When a record is extended by a protocol, it forms a relationship between the class and the implementation. For `Server`, the `start` and `stop` methods do one thing, and for `DB` or `Worker` -- another.

Every `defmacro` expression declares a separate Java class. Finding a method in Clojure works in a class hierarchy similar to catching exceptions. When loaded, Component extends the base `Object` class with the `start` and `stop` methods, which only return `this`. If you take a slot away from a record, it becomes a map. The map does not directly implement `Lifecycle`; therefore, the search will go up the inheritance tree. As a result, it resolves to `Object`, which will return `this` by default.

Below is an example of a failed component. Its `start` method will return a map with the `server` field:

~~~clojure
(defrecord BadServer [options server]
  component/Lifecycle
  (start [this]
    {:server (run-jetty app options)})
  (stop [this]
    (.stop server)
    nil))
~~~

The server will start without errors, but then a problem awaits us. If we pass the result of `start` to `stop`, the server will not stop. No matter how many times you call `component/stop`, nothing will happen.

~~~clojure
(def bs-created (map->BadServer
                  {:options {:port 8080 :join? false}}))

(def bs-started (component/start bs-created))
(type bs-started)
;; clojure.lang.PersistentArrayMap

(component/stop bs-started)
;; does nothing, the server still works
~~~

In the last expression, the `stop` implementation for `Object` will actually work. Let's review its code which obviously does nothing.

~~~clojure
(extend-protocol Lifecycle
  #?(:clj java.lang.Object :cljs default)
  (start [this]
    this)
  (stop [this]
    this))
~~~

An error also crept into the `stop` method. When stopped, the component will terminate the server but return `nil`. If you pass the result of `stop` to `start`, the latter gets `nil`. This will throw an exception because `nil` does not implement the `Lifecycle` protocol.

Make sure that the component changes only the values of the slots rather than their structure. Typically, the `start` and `stop` code ends with the `assoc` form, but not `dissoc`. It's easy to check if your component was designed properly. There should be no problems with forwarding it through a set of stages like this:

~~~clojure
(-> (make-component {...})
    (component/start)
    (component/stop)
    (component/start))
~~~

## Database Component

Let's write a component to work with a database. It contains the `options` and `db-spec` slots. The former is a map of future pool options, and the latter is a JDBC spec with a pool. You know how it works from the previous sections.

~~~clojure
(defrecord DB [options db-spec]
  component/Lifecycle
  (start [this]
    (let [pool (cp/make-datasource options)]
      (assoc this :db-spec {:datasource pool})))
  (stop [this]
    (-> db-spec :datasource cp/close-datasource)
    (assoc this :db-spec nil)))
~~~

Let's add a constructor:

~~~clojure
(defn make-db [options]
  (map->DB {:options options}))
~~~

Now the component is ready to start: it can be run through the `make-db` &rarr; `component/start` &rarr; `component/stop` functions.

It is not yet clear how to execute a request through this component. We are interested in the `db-spec` slot, which stores the spec. You can fetch it out from the component and pass to the function:

~~~clojure
(let [{:keys [db-spec]} db-started
      users (jdbc/query db-spec "select * from users")]
  (process-users users))
~~~

But this is an invalid approach: you must not invade a component, even if the language offers such a possibility. That violates the idea that the component is impartible for the consumer. We can only pull the "controls" that the component offers. Now the controls are not there, so let's add them.

Let's expand the `DB` record with methods for working with the database. We will put them in a separate protocol. Signatures are similar to JDBC functions, with the difference that the first parameter is the `this` component, not a spec:

~~~clojure
(defprotocol IDB
  (query [this sql-params])
  (update! [this table set-map where-clause]))
~~~

In the body of `defrecord`, right after `stop`, we implement the new protocol. Methods boil down to JDBC functions, into which we pass the `db-spec` slot and arguments. At first, the component implements `Lifecycle` as usual:

~~~clojure
(defrecord DB [options db-spec]
  ;; ...component/Lifecycle
~~~

A private protocol follows:

~~~clojure
  IDB
  (query [this sql-params]
    (jdbc/query db-spec sql-params))

  (update! [this table set-map where-clause]
    (jdbc/update! db-spec table set-map where-clause)))
~~~

The component is ready for requests. We are not calling JDBC functions but protocol methods. This will isolate the dependency on JDBC and reduce code coupling.

~~~clojure
(def db-created (make-db options))
(def db-started (component/start db-created))

(query db-started "select * from users")
(update! db-started :users {:name "Ivan"} ["id = ?" 42])

(def db-stopped (component/stop db-started))
~~~

## Transactional Component

Transactions are needed for consistent changes in a database. So far, we've used the `jdbc/with-db-transaction` macro. It makes a transactional connection out of a regular one and assigns it to a symbol.

Unlike JDBC, our macro works with a component. It takes a regular component and assigns its *transactional* version to the symbol. The macro performs the following steps:

- get a spec from a component;

- wrap body in JDBC macro, assign the transactional connection to a variable;

- get the component in which this variable replaces the `:db-spec` slot;

- assign a new component to a symbol from a macro.

~~~clojure
(defmacro with-db-transaction
  [[comp-tx comp-db & trx-opt] & body]
  `(let [{db-spec# :db-spec} ~comp-db]
     (jdbc/with-db-transaction
       [t-conn# db-spec# ~@trx-opt]
       (let [~comp-tx (assoc ~comp-db :db-spec t-conn#)]
         ~@body))))
~~~

When analyzing, you will notice that the principle of component closedness is violated. We read and manually replace the `db-spec` slot. Think about how to improve the code. Hint: You can move access to the slot into the `get-` and `set-spec` methods, that is, the usual "getter" and "setter" from the OOP world. The difference is that the "setter" will return a new component. Here is the macro in action:

~~~clojure
(with-db-transaction
  [db-tx db-started]
  (let [query "select * from requests limit 1 for update"
        result (query db-tx query)]
    (when-let [id (some-> result first :id)]
      (update! db-tx :requests
               {:is_processed false}
               ["id = ?" id]))))
~~~

In the PostgreSQL logs, we will see the following entries:

~~~sql
BEGIN
select * from requests limit 1 for update
UPDATE requests SET is_processed = $1 WHERE id = $2
DETAIL:  parameters: $1 = 'f', $2 = '3'
COMMIT
~~~

`SELECT` and `UPDATE` queries were indeed in a transaction.

## Worker

Let's write a background worker component. Let's declare the module and dependencies:

~~~clojure
(ns book.systems.comp.worker
  (:require
   [com.stuartsierra.component :as component]
   [book.systems.comp.db :as db]
   [clj-http.client :as client]))
~~~

A worker is a record with `Lifecycle` and `IWorker` protocols. You are already familiar with the `Lifecycle` protocol: it is the `start` and `stop` functions. Let's put the component logic in `IWorker`. The logic includes an infinite loop and its preparation. We expect `task-fn` to be a function that the worker calls at each step of the loop. The `make-task` method wraps the function in the loop and `try/catch`.

~~~clojure
(defprotocol IWorker
  (make-task [this])
  (task-fn [this]))
~~~

The record stores four slots: input options, an atom with a continuation flag, a future with a loop (`task`), and a database. The fourth slot is the dependent component, the `DB` instance we just wrote. Let's implement `Lifecycle`:

~~~clojure
(defrecord Worker
  [options flag task db]

  component/Lifecycle

  (start [this]
    (let [flag (atom true)
          this (assoc this :flag flag)
          task (make-task this)]
      (assoc this :task task)))

  (stop [this]
    (reset! flag false)
    (while (not (realized? task))
      (log/info "Waiting for the task to complete")
      (Thread/sleep 300))
    (assoc this :flag nil :task nil)))
~~~

Notice line 8: we're adding a status flag to `this`. The `make-task` method expects `this` with a filled `flag` slot. If we remove this line, the `make-task` will get an entry with an empty flag.

Let's describe the `IWorker` protocol. You are already familiar with the `make-task` and `task-fn` code from the Mount section. The difference is that now we are working with methods instead of functions. The method has direct access to slots, so you don't need to pass them in parameters. Below we moved the `make-task` and `task-fn` code into the component. For the sake of shortness, we will skip error catching and some details.

~~~clojure
(defrecord Worker
  ;; ...component/Lifecycle
  IWorker

  (make-task [this]
    (future
      (while @flag        ;; ...try/catch
        (task-fn this)))) ;; ...sleep

  (task-fn [this]
    (db/with-db-transaction [tx db]
      (when-let [request (first (db/query tx query))]
        (let [fields (...))] ;; get fields
          (db/update! tx :requests
                      fields ["id = ?" id]))))))
~~~

Let's add a constructor, and the component is ready to use:

~~~clojure
(defn make-worker
  [options]
  (map->Worker {:options options}))
~~~

## Manual dependencies

The worker differs from other components in dependencies. It is not yet clear how it knows about the database because the constructor accepts only options. The system solves this problem rather than a programmer. We should not pass components to each other when creating.

But for educational purposes, we will break this rule and assemble the mini-system manually. This way helps us better understand how it works and test the code. Let's run an experiment in the `core` module with two components. Add constructors to the module:

~~~clojure
(ns book.systems.comp.core
  (:require
   [com.stuartsierra.component :as component]
   [book.systems.comp.worker :refer [make-worker]]
   [book.systems.comp.db :refer [make-db]]))
~~~

Take a look at the system below. It is a function that takes a config. We manually start the database and worker and return the component map. In the tenth line, we set the slot with the database for the worker.
Pay attention when to do it -- the database component has already been enabled, but the worker hasn't yet (because `assoc` comes before `start`).

~~~clojure
(defn my-system-start
  [config]
  (let [{opt-db :pool
         opt-worker :worker} config

        db (-> opt-db
               make-db
               component/start)

        worker (-> opt-worker
                   make-worker
                   (assoc :db db)
                   component/start)]

    {:db db :worker worker}))
~~~

To start the system, pass the pool and worker parameters to the function. Save the system to a variable for later termination.

~~~clojure
(def _sys (my-system-start {:pool {...} :worker {...}}))
~~~

Test the system: add records to the `requests` table and make sure that the worker adds data to their fields. The stop function will stop the components in reverse order:

~~~clojure
(defn my-system-stop
  [system]
  (-> system
      (update :worker component/stop)
      (update :db component/stop)))

(my-system-stop _sys)
~~~

## System in Production

Let's consider how a system is built in practice. The `system-map` function takes a chain of values. The odd elements are the component names, and the even ones are the calls of their constructors. The `system-map` call builds a component tree with filled slots. We get the system at rest.

When building the system, there should be no side effects. Constructors only create records. If a constructor is accessing a disk or changing state, this is a blunder in your design.

The system depends on the config, so assembling is moved to the `make-system` function. It takes a config map and divides it into parts. Each constructor takes its part. It is convenient when the config structure repeats the system with the component keys are at the top level, and option maps are below them.

To pass dependencies to the component, it is wrapped in the `component/using` function. The second argument is the keys of the components, which it should receive before starting. The key can be a vector or a map. If the slot and component names match, it is a vector. If they differ, pass the map of the form `{:slot :component}`.

Below, the `make-system` function builds the system we agreed on at the beginning of the chapter. The `worker` component is wrapped in `component/using`. We passed the `[:db]` vector because this is the name of both the worker slot and the system component.

~~~clojure
(defn make-system
  [config]
  (let [{:keys [jetty pool worker]} config]
    (component/system-map
     :server (make-server jetty)
     :db     (make-db pool)
     :worker (component/using
              (make-worker worker) [:db]))))
~~~

If the component name were `:storage`, we would define a map:

~~~clojure
(component/system-map
 :server  (make-server jetty)
 :storage (make-db pool)
 :worker  (component/using
           (make-worker worker) {:db :storage}))
~~~

Maps are useful for third party components, because their creators don't know what the entities in your project are called. Suppose the third-party component depends on `:database`, but we have `:db`. The map removes the name discrepancy problem.

To start the system, it is passed into `component/start`. The system has a special `Lifecycle` implementation. At startup, it builds a dependency graph and determines the traversal order. Before starting a component with dependencies, the system will pass them to the component through `assoc`, as we did manually. Stopping the system works the same way.

~~~clojure
(def config {...})
(def sys-init (make-system config))
(def sys-started (component/start sys-init))
(def sys-stopped (component/stop sys-started))
~~~

Like a component, the system flows freely through the `start` and `stop` functions in the \arr operator. If not, there is an error in the system.

~~~clojure
(-> (make-system config)
    (component/start)
    (component/stop)
    (component/start))
~~~

## System Storage

Above, we set the system using `def`, which is not entirely correct. The system is an entity that we turn on on-demand. Treat it like a global variable that changes its value. `Alter-var-root` is good for that.

A future system variable is declared in the module. The `defonce` macro, which is executed strictly once, does this. Thanks to this, we will not lose the old system when reloading the module.

Like a component, the system is in one of three states: rest, start, and stop. The `system-init`, `system-start`, and `system-stop` functions bring the system to the required state. They work using the service `alter-system`, which we specified using `partial` for shortness.

~~~clojure
(defonce system nil)

(def alter-system (partial alter-var-root #'system))

(defn system-init [config]
  (alter-system (constantly (make-system config))))

(defn system-start []
  (alter-system component/start))

(defn system-stop []
  (alter-system component/stop))
~~~

Now there's a way to control the system. The `main` function is the entry point to the program. It boils down to three steps: reading the configuration, preparing and starting the system.

~~~clojure
(defn -main [& args]
  (let [config (load-config "config.edn")]
    (system-init config)
    (system-start)))
~~~

The system is global, but we cannot access it directly. If a component is looking for something in the depths of the system, it means that the developer has failed. This approach destroys the very idea of a system and components. The system can only be accessed during development or testing. It is worth making the system private for higher reliability: this way, we will protect it from external access.

~~~clojure
(defonce ^:private system nil)
~~~

Of course, you can also get to the private system with `resolve` and a fully qualified symbol. This is an advanced technique that no variable can resist. But `resolve` stands out, and the trick is easier to spot.

## Correct Completion

Now let's spend some time on how to stop a system correctly. This problem is also known as "graceful shutdown". By correctness, we mean that all resources -- files, connections, and transactions, -- have been closed with no failures.

The problem is, when running, some components cannot stop at an arbitrary moment. For example, if a queue ends abnormally, we either lose the message or process it twice. Close resources properly, even if you have to wait for them.

In production mode, the application listens for POSIX signals and responds to them appropriately. If SIGTERM comes, the application should stop the system, wait for the shutdown, and only then exit.

The [Signal](https://github.com/pyr/signal) library provides a macro to associate a signal with a reaction to it. Add the library to the project:

~~~clojure
;; project.clj
[spootnik/signal "0.2.2"]

;; src/book/systems/comp/core.clj
(ns ...
  (:require [signal.handler :refer [with-handler]]))
~~~

Extend the `-main` function. After the system starts, add an event to the SIGTERM and SIGHUP signals. The former stops the system and ends the program. We regard the latter as a system reboot.

~~~clojure
(with-handler :term
  (log/info "caught SIGTERM, quitting")
  (system-stop)
  (log/info "all components shut down")
  (System/exit)

(with-handler :hup
  (log/info "caught SIGHUP, reloading")
  (system-stop)
  (system-start)
  (log/info "system reloaded"))
~~~

The signals do not work when the project is run via `lein run` or in the REPL. To test the signals, build `uberjar`, and run it as a Java application.

~~~bash
$ lein uberjar
$ java -jar target/book-standalone.jar
~~~

Press `Ctrl+C`. The application will not end immediately, and you will see a message that all components have stopped.

Process control programs like systemd or Kubernetes usually wait 30 seconds for a process to stop. Otherwise, it is terminated forcibly. The system wait should be within reasonable limits. If it goes beyond 30 seconds, you need to find and improve the problematic component.

A simple way to find such a component is to add debug log into the `stop` method:

~~~clojure
(stop [this]
  (log/debug "Stopping the web-server...")
  (.stop server)
  (log/debug "The web-server has been stopped.")
  (assoc this :server nil))
~~~

The time window between log entries will show which component slows down the system.

## More about Waiting

Let's go back to the `-main` function of the app. It is the entry point of the Clojure program:

~~~clojure
(defn -main [& args]
  (let [config (load-config "config.edn")]
    (system-init config)
    (system-start)))
~~~

If you are not familiar with the JVM intricacies, you may be wondering why the program does not terminate after calling `(system-start)`? There is no loop, hook, or event behind it, but the platform continues to work.

This is the standard JVM behavior. If the program executed the instructions without error, the main thread waits until the child ones have stopped. System startup generates new threads (server, connection pool). After `(system-start)`, the main thread will be running until they are finished. It will wait until the system is stopped in another thread or a termination signal, SIGTERM, arrives.

If the component is stateless, there will be no new threads. You can modify the `DB` component so that the `db-spec` slot is not a connection pool but a static map. Some components perform a one-time task at the start. If no components have created a thread, the program will execute `start` for each one and exit.

## Improving Dependencies

We passed dependencies to the component with `using`:

~~~clojure
(component/system-map
 ;; ...
 :worker (component/using
          (make-worker worker) [:db]))
~~~

When there are many components, dependencies make the code noisy and difficult to read. In the example above, we first get the component from the constructor and then add dependencies to it. The last step can be moved to the constructor. It will return a component with dependencies, and `using` will leave the system. Let's rewrite the worker constructor:

~~~clojure
(defn make-worker [config]
  (-> config map->Worker (component/using [:db])))
~~~

The new system is cleaner and shorter:

~~~clojure
(component/system-map
 :server (make-server jetty)
 :db     (make-db pool)
 :worker (make-worker worker))
~~~

Such an approach requires the component names to match the slot ones. If these are your components, agree on naming them with the team. It's easy to write your own constructor for third-party components.

Now let's look at how dependencies work. As you might guess, the `component/using` call passes something to the component, but the latter doesn't change. It has no `:deps` field or hidden atom. The component stores dependencies in metadata.

Metadata is a map of additional information about an object. Metadata works with collections and some Clojure types (symbols, vars). Metadata is used to supplement an object without changing it. Component dependencies are in line with this concept.

The `meta` function returns the object's metadata. The code below proves that the constructor added dependencies:

~~~clojure
(-> {...} make-worker meta)
#:com.stuartsierra.component{:dependencies {:db :db}}
~~~

Another way to see them is to set the `*print-meta*` variable to true. When the object is printed to the REPL, the metadata will appear:

~~~clojure
(set! *print-meta* true)

(make-worker {...})
^#:com.stuartsierra.component{:dependencies {:db :db}}
#book.systems.comp.worker.Worker{...}
~~~

## Grouping slots

Slots are divided into three groups: inputs, state, and dependencies. Let's recall the worker component:

~~~clojure
(defrecord Worker
    [options flag task db])
~~~

In this example, the `options` slot is for initialization, `flag` and `task` are for state, and `db` is for dependency. The more complex the component is, the more slots are in each group. When slots are in random order, it's difficult to understand their semantics. It is considered good practice to separate groups of slots from each other with a comment:

~~~clojure
(defrecord Worker
    [;; init
     options
     ;; runtime
     flag
     task
     ;; deps
     db])
~~~

The first is the `init` group -- the input parameters. The same arguments are expected from the constructor. The `runtime` group lists the slots that the component will fill at startup. The `deps` group indicates dependencies. They are the same as the key vector from the `using` function.

Grouping slots improves code and makes it more readable. Agree with the whole team to implement this practice. When there are too many slots, that indicates the component is overcomplicated. In this case, you can move some of the logic into a child component and connect it depending on the first one.

We do not group slots here because, otherwise, the code will take up a lot of space. But be sure to do this in your real project.

## Conditional System

In the chapter on config, we told you about feature flags. They are parameters that enable layers of logic. With flags, there is no need for a new build of the application. It is enough to change the configuration and restart the service.

Sometimes a system is not built linearly, but according to conditions. Let's say the worker is still in test mode and works only on certain machines. Let's add a field with the semantics "run worker" to the configuration. If the field is false, the system will start without this component.

Let's add the `:features` group to the configuration for the features flags:

~~~clojure
{:features {:worker? true}
 :jetty {:join? false :port 8088}
 ...}
~~~

Now rewrite the `make-system` function with flags. Before getting into `system-map`, the components are selected. Let's select the essential components that always work and gradually add optional ones.

The `cond->` macro passes the vector through the chain of conditions and forms. If the `worker?` condition is true, the following form will add the worker's key and its instance to the vector. Other flags and expressions may appear below.

~~~clojure
(defn make-system [config]
  (let [{:keys [features jetty pool worker]} config
        {:keys [worker?]} features
        comps-base [:server (make-server jetty)
                    :db (make-db pool)]
        comps (cond-> comps-base
                worker?
                (conj :worker (make-worker worker)))]
    (apply component/system-map comps)))
~~~

Let's make sure the flag works. Since a system is a record, so the `keys` function will return its keys. The `:worker` slot appears depending on the flag:

~~~clojure
(keys (make-system {:features {:worker? false}}))
;; (:server :db)

(keys (make-system {:features {:worker? true}}))
;; (:server :db :worker)
~~~

Flags make it easier to work with the project. Some components are complex and require special environments, so it's hard to run them locally. If you can set them with a flag, it will benefit the whole team.

## Sharing Components

Within a system, components communicate freely with each other. If one of them needs the other, we'll add a dependency and a slot. The problem arises when another entity rather than a component accesses the system.

That mainly happens when an HTTP request handler needs individual components. At first glance, it's not obvious how to bind a handler to a system. A function does not fit well with the component ideas: the latter stores state, and the former avoids it. Starting and stopping the functions is meaningless. In general, a component and a function are opposite to each other.

Let's see a case where a database component is required in an HTTP request. How can you pass a part of the system into a function without violating the principle of the library? We are not considering using a global variable because, in this case, it is surrender. Two methods will help -- plugging in components to a request and routes.

**In the first case**, some components become a part of the HTTP request. The handler still accepts one argument but additionally fetches the database, cache, and other components from it. The variant has the right to exist because the request is a part of the server, and the server is a component. Based on this, the latter can add fields to the request. To make the database component available to the server, we include it in dependencies. Let's change the server slots and the constructor:

~~~clojure
(defrecord Server
  [options server db]) ;; db added

(defn make-server
  [options]
  (-> (map->Server {:options options})
      (component/using [:db])))
~~~

Let's extend the server `start` method. If earlier we passed `app` directly to `run-jetty`, now we will add a new step. The `make-handler` function wraps `app` so that every request in `app` is supplemented with a database.

~~~clojure
;; app factory
(defn make-handler [app db]
  (fn [request]
    (app (assoc request :db db))))

;; Lifecycle
(start [this]
  (let [handler (make-handler app db)
        server (run-jetty handler options)]
    (assoc this :server server)))
~~~

Let the home page display data from the database. The code below shows how to read a table from an HTTP request. In order not to complicate the example with HTML layout, let's return just pretty printed data.

~~~clojure
(defn app [{:keys [db]}]
  (let [data (db/query db "select * from requests")]
    {:status 200
     :body (with-out-str
             (clojure.pprint/pprint data))}))
~~~

Over time, we will need other components, such as a task queue or cache. Let's do the same, add them to server dependencies and forward them to the `make-handler`.

When there are too many components, it is inconvenient to store them at the top level of the request -- there is a risk of keys conflicting. Let's put them in the nested `:system` map. But pay attention: there is not the entire system in `:system`, but the minimum subset required by the web part.

**The second approach** changes the route tree. It is no longer static but comes from a builder function. The function takes the components that the handlers need. Internally, we call them with a request and dependencies. This changes the function's signature: now the handler can have several arguments (request, base, cache). Let's see, by example, how to build such a system.

Recall how we built HTTP routes. The macros `defroutes` makes a function which accepts a request and returns a response.

~~~clojure
(defroutes app
  (GET "/"      request (page-index request))
  (GET "/hello" request (page-hello request))
  page-404)
~~~

The new route tree is not static anymore as it depends on components. Let's assume the web pages require a database and a mail server. The function `make-routes` accepts the `db` and `smtp` arguments and makes routes closed over them. The functions `page-user` and `page-feedback` take two arguments: a request and a component.

~~~clojure
(defn make-routes [db smtp]
  (routes
   (GET "/users" request
      (page-users request db))
   (POST "/feedback" request
      (page-feedback request smtp))))
~~~

The `start` method builds the routes and pass them into `run-jetty`:

~~~clojure
(start [this]
  (let [routes (make-routes db smtp)
        server (run-jetty routes options)]
    (assoc this :server server)))
~~~

In turn, to make the `db` and `smtp` components available for the server, declare them as dependencies:

~~~clojure
(component/system-map
 :db   (make-db pool)
 :smtp (make-smtp smtp)
 :server
 (-> jetty
     make-server
     (component/using [:db :smtp])))
~~~

Pay attention that `page-users` and `page-feedback` functions take more then one argument now. Before, it was only a request, but now a component was added to it. In the case of the `page-users` handler, it is a running database.

~~~clojure
(defn page-users
  [request db]
  (let [users (db/query db "select * from users")]
    {:status 200
     :body (with-out-str
             (clojure.pprint/pprint users))}))
~~~

This approach changes our habits slightly yet makes the code more obvious. An HTTP request and a database component are different subjects, so it makes more sense to pass them separately. The schema might be easily extended in the future. If a web page needs a cache, we will add it in three steps. First, we grow the server dependencies; second, pass the component in `make-routes`; finally, we accept it in the HTTP handler.

Both methods -- passing components in a request or routes -- solve the same problem. The difference is in passing arguments to a handler. Passing components through a request map is convenient because the handler usually takes one argument, and you don't have to change the signature.

On the other hand, passing components in a request isn't always obvious. When a request has many fields, it can be difficult to work with it in testing and development. When printing a request or in logs, you will get too much output. The variant with closure and two arguments is clearer. The signature directly tells what data is expected at the input. The choice of method depends on team conventions.

## Idempotence

Until now, we have written components so that re-running them would lead to an error. Let's demonstrate this with the example of a web server:

~~~clojure
(def s-started (-> {:port 8088 :join? false}
                    make-server
                    component/start))

(component/start s-started)
;; Execution error (BindException)
;; Address already in use
~~~

In the `start` body, we don't check if the server is already running. If we try to start it again, we'll get an exception: the port is busy. That's right: we wouldn't want two servers running. There may not be exceptions for other components. If we restart the database, we'll get a new connection pool. The old one will remain in memory and will work. As a result, resources are wasted.

Idempotence is property when a repeated operation returns the same result. A component has to follow it to avoid resource leaks. To do this, we check the slot before opening the resource. If the slot is `nil`, we create a new server and attach it to the slot. Otherwise, the server is already running and `this` is returned.

~~~clojure
(start [this]
  (if server
    this
    (let [server (run-jetty app options)]
      (assoc this :server server))))
~~~

`Stop` works similarly: before stopping the resource, the slot is checked for emptiness:

~~~clojure
(stop [this]
  (when server
    (.stop server))
  (assoc this :server nil))
~~~

The variant with the `or` macros is slightly shorter. We always update the slot, but the value is either the current server or the new one.

~~~clojure
(start [this]
  (let [server (or server
                   (run-jetty app options))]
    (assoc this :server server)))
~~~

## Alternative Way

Throughout this chapter, we have described components with records (typed maps). The record creates a separate class, which is extended by a protocol. When calling a method from the protocol, Clojure looks for an implementation by class.

`Defrecord` is the recommended way to work with Component. Of course, it has its drawback: the code with records takes up more space than functions. The record does not work with `dissoc`: if you take away a slot from it, you get a regular map with an empty implementation of `start` and `stop`. There is a second way to describe a component. It takes less code but requires a deep understanding of how methods in Clojure work.

In fact, the thesis that method lookup works by class is incomplete. If the `:extend-via-metadata` property is set for the protocol, then Clojure looks for the implementation in the object's metadata.

Suppose you have declared an `API` protocol in the `project.api` namespace with this property and the `get-user` method:

~~~clojure
(defprotocol API
  :extend-via-metadata true
  (get-user [this id]))
~~~

The method expects the first argument to be an object that implements this protocol. However, it is acceptable to pass another one instead, which has the `project.api/get-user` symbol with a function in its metadata. In this case, the method is resolved to the function from the metadata.

~~~clojure
(def api
  ^{`get-user
    (fn [this id]
      {:id id
       :name (format "User %s" id)})}
  {:any "map"})

(get-user api 5)
;; {:id 5, :name "User 5"}
~~~

The `Lifecycle` protocol has the required flag set to true. The idea is to generate an object that has implementations of `start` and `stop` methods in its metadata. A map is suitable for the role of the object because the library adds dependencies through `assoc`.

Let's describe the server component in a new way. It consists of three functions: a constructor, `start`, and `stop`. The constructor takes server parameters and wraps them in a map. The `with-meta` function adds metadata to the component. Metadata keys are fully qualified symbols that point at the `Lifecycle` protocol methods. The keys will contain so far unknown start and stop functions:

~~~clojure
(defn init [options]
  (with-meta {:options options}
    {'com.stuartsierra.component/start start
     'com.stuartsierra.component/stop stop}))
~~~

Let's write the starting of the component. The `start` function takes the map we returned from `init`. For convenience, let's call it `this` and unpack the options at the signature level. Then we start the server and add it to the current map.

~~~clojure
(defn start
  [{:as this :keys [options]}]
  (let [server (run-jetty app options)]
    (assoc this :server server)))
~~~

The stopping is similar. Note that at the end, we detach the key with `dissoc`. The loss of the key does not affect anything, because the method search works by metadata rather than class. `Dissoc` doesn't change the metadata.

~~~clojure
(defn stop
  [{:as this :keys [server]}]
  (.stop server)
  (dissoc this :server))
~~~

In the system, the component behaves as usual. When the system is built, the `init` function is called with configuration parameters. When starting, the library calls the `start` method on all entities, and the implementation from metadata will run for the server.

Nothing changes in the dependencies: they are also specified through `using` and with a vector or dictionary. The second method also works, when the constructor "charges" the component with dependencies upon creation.

~~~clojure
(defn init [options]
  (-> {:options options}
      (with-meta
        {'com.stuartsierra.component/start start
         'com.stuartsierra.component/stop stop})
      (component/using [:compA :compB])))
~~~

The `start` and `stop` methods only need to unpack them at the signature level.

~~~clojure
(defn start
  [{:as this :keys [compA compB options]}]
  ...)
~~~

Let's examine the metadata of the component after `init` to make sure it has everything we need. For debugging such components, the `*print-meta*` variable, which we talked about in the last chapter, is useful.

~~~clojure
(-> (init {:port 8080})
    meta
    clojure.pprint/pprint)

#:com.stuartsierra.component
  {start #function[book.systems.comp.server/start]
   stop #function[book.systems.comp.server/stop]
   :dependencies {:compA :compA :compB :compB}}
~~~

The alternative method does not override what we said earlier in this chapter. As with `defrecord`, the component must follow the practices we've covered. For example, we must have made our start and stop methods being idempotent but didn't do that to keep the code short.

In this section, we have learned an extended technique. Even if you like the new type of components more, do not rush to switch to it. First, understand the classic approach as the library recommends.

This concludes the Component overview and moves on to the third library --

# Integrant

The [Integrant](https://github.com/weavejester/integrant) library is the next step in the development of the system. It is at the end of the overview for several reasons. Integrant builds on the Component ideas we just discussed. The former is more flexible and generally more advanced. To know better its features, let's recall what usually confuses us when using Component.

Component entities resemble classes and OOP. In Clojure, in the background of data and functions, this looks like a complication. Let a component be a function. It is simpler than an object because only one operation acts on functions -- a call.

The component has only two states -- `start` and `stop`. Integrant offers additional stages: pause and resume, spec validation, and parameter preparation. These stages are empty by default, but a component can complement them. With this approach, we have more control over the system.

Integrant is declarative: you can describe the system in a file and build it with one function. That compares favorably with the Component library, where you compose a system manually.

Integrant is tolerant of dependencies. In Component, a dependency requires two steps: adding a slot and metadata, but in Integrant -- only one. In Component, a dependency can be only another component. Sometimes an object is wrapped in a component just to fulfill this requirement. In Integrant, a dependency can be anything: a dictionary, a future, or a function.

## Basic Arrangement

Work with Integrant begins with a description of the future system. It is a map on which further logic is based. The map key is the machine name of the component, and the value is the startup parameters. The system below consists of a web server and a database.

~~~clojure
(def config
  {::server {:port 8080 :join? false}
   ::db {:username      "book"
         :password      "book"
         :database-name "book"
         :server-name   "127.0.0.1"
         :port-number   5432}})
~~~

The system and components are related through multimethods. To add a reaction to an event, we extend the required multimethod with a component key. For example, at startup, the system calls the `init-key` method for each key. To explain to the system how to start the server, we extend the method with the `::server` key.

Integrant expects at least two methods from the key: start and stop. Since these are the main actions, there is no default response for them. Other events are optional and are up to you.

## First components

Let's write server and database components. They are simple and have no dependencies. Prepare the `integrant.clj` module with a header:

~~~clojure
(ns book.integrant
  (:require [integrant.core :as ig]))
~~~

For the sake of brevity, let's skip the imports of `Jetty`, `HikariCP`, and other libraries. They are similar to the examples from Mount and Component.

Let's begin with the server. The `init-key` method takes a key and an options map. These are the `::server` and `{:port 8080: join? false}` values from config. This method should return the state of the component. In our case, this is the result of `run-jetty`.

~~~clojure
(defmethod ig/init-key ::server
  [_ options]
  (run-jetty app options))
~~~

The key is known from the method definition, so the first argument is shadowed with an underscore. Let's describe the database in a similar way. Its state is a JDBC spec with a connection pool.

~~~clojure
(defmethod ig/init-key ::db
  [_ options]
  {:datasource (cp/make-datasource options)})
~~~

The `init` function runs through the configuration and calls `init-key` for each key. We should get a dictionary with the same keys, but the values will be the states of components. Let's save the running system to a variable to turn it off later.

~~~clojure
(def _sys (ig/init config))

(keys _sys)
(:book.integrant/db :book.integrant/server)
~~~

In terms of Integrant, stopping a system is named "halt". The `halt-key!` method determines how to shut down the component. It takes the key and state from the `init-key` method. Let's describe the halt of the server and database:

~~~clojure
(defmethod ig/halt-key! ::server
  [_ server]
  (.stop server))

(defmethod ig/halt-key! ::db
  [_ db-spec]
  (-> db-spec :datasource cp/close-datasource))
~~~

The `halt!` function will stop the entire system. Pass the result of `ig/init` to it.

~~~clojure
(ig/halt! _sys)
~~~

## Dependencies

To specify dependencies, we add a reference to the options. At startup, Integrant searches for references in the system and builds a dependency graph. Set the reference with the `ig/ref` function. It takes a key that the component depends on.

Let's consider the dependency on the example of a worker. Add a key to the config, as in the example below. To separate component options from dependencies, put them in the separate `:options` field.

~~~clojure
{::worker {:options {:sleep 1000}
           :db (ig/ref ::db)}}
~~~

When the `init-key` reaches the `::worker` key, `:db` will contain the value that `init-key` returned for `::db`. Here is the code for starting and stopping the worker. Since this is already the third implementation, we will leave only the main part. If you need to brush up on how a worker works, refer to the section where we wrote it for the first time.

~~~clojure
(defmethod ig/init-key ::worker
  [_ {:keys [db options]}]
  (let [flag (atom true)
        task (make-task db flag options)]
    {:flag flag :task task}))

(defmethod ig/halt-key! ::worker
  [_ {:keys [flag task]}]
  (reset! flag false)
  (while (not (realized? task))
    (Thread/sleep 300)))
~~~

## Parallels with Component

Several techniques we covered in the Component section also apply to Integrant. Let's recall some of them.

**Global storage**: To manage the system, you need to store it somewhere. The easiest way to do this is to add a global variable and start and stop functions.

~~~clojure
(defonce ^:private system nil)

(def alter-system (partial alter-var-root #'system))

(defn system-start []
  (alter-system (constantly (ig/init config))))

(defn system-stop []
  (alter-system ig/halt!))
~~~

As with Component, the system must be private. Free access to it is unacceptable for consumers.

**Waiting and signals**: An application waits for all components to stop before exiting. The `with-handler` macro and signal interception work in the same way for Integrant:

~~~clojure
(with-handler :term
  (log/info "caught SIGTERM, quitting")
  (system-stop)
  (log/info "all components shut down")
  (exit))
~~~

**Sharing components**: In Integrant, it is easier to access the system from an HTTP request. The handler can be a database-dependent component. Let's add a key of a handler with a reference to `::db`:

~~~clojure
{::handler {:db (ig/ref ::db)}}
~~~

Let the page display the number of records in the database. In the run of the key, we return a function closed over `db`:

~~~clojure
(defmethod ig/init-key ::handler
  [_ {:keys [db]}]
  (fn [request]
    (let [query "select count(*) as total from requests"
          result (jdbc/query db query)
          total (-> result first :total)]
      {:status 200
       :body (format "You've got %s records." total)})))
~~~

Let's modify the server so that it depends on the handler. When starting the server, we pass the handler to `run-jetty`:

~~~clojure
{::server {:options {:port 8080 :join? false}
           :handler (ig/ref ::handler)}}

(defmethod ig/init-key ::server
  [_ {:keys [handler options]}]
  (run-jetty handler options))
~~~

As with Component, `::handler` can return both a single page and a route tree, using Compojure.

**Conditional build**: The system can be changed conditionally before starting. A special function determines if a worker can be run on this machine. If yes, add the component key and its settings to the system.

~~~clojure
(cond-> sys-config
  (is-worker-supported?)
  (assoc ::worker {:options {:sleep 1000}
                   :db (ig/ref ::db)}))
~~~

Another way to start a subset of the system is similar to Mount. The `init` function accepts an optional list of keys to include. The list is prepared in advance according to a rule. Below we take all the configuration keys and remove some by the condition.

~~~clojure
(let [components (-> config keys set)
      components (cond-> components
                   (not (is-worker-supported?))
                   (disj ::worker))]
  (ig/init config components))
~~~

## Loss of Keys

Specify for components fully qualified keys (e.g., `::server`, `::db`). The double colon stands for the current namespace in which the key is declared. The `::db` notation is a shorthand for `:book.integrant/db`.

When a key is fully qualified (with a namespace), it is easy to understand in which module it is declared. Real systems consist of more than ten components and more. Let's say there is a problem with the `:queue` key. How to understand which module it is in? Whereas the `:my-project.queue/task` key contains this information. We recommend you always use fully qualified keys.

A situation may arise that you forgot to import the module in which you extended the multimethod. If the module is not loaded, Integrant will not know about the component. Sometimes you are left completely confused: you remember that you wrote the code; however, there is no component. We've discussed a similar problem in Mount. To avoid an error, add all modules with components to the header of the main module, which is always loaded. Let it be the system module.

~~~clojure
(ns project.system
  (:require project.db
            project.server
            project.worker
            project.queue))
~~~

Syntax checking utilities (linters) may generate a warning. In their terms, you've added a module, but you are not using it because there is no `project.db/<something>` expression in your code. To suppress such warnings, correct linter settings. Add `project.db` into the "known namespaces" section or similar.

Integrant offers the `load-namespaces` function to load modules automatically. It accepts the configuration of the system. For each key, the function looks for its namespace and loads it. Below, there is a real system which keys come from different modules:

~~~clojure
(def config
  {:project.server/server
   {:options {:port 8080 :join? false}
    :handler (ig/ref :project.handlers/index)}
   :project.db/db {...}
   :project.worker/worker
   {:options {:sleep 1000}
    :db      (ig/ref :project.db/db)}
   :project.handlers/index
   {:db (ig/ref :project.db/db)}})
~~~

To load all modules that participate in the system, execute the following:

~~~clojure
(ig/load-namespaces config)
~~~

We recommend beginners to refrain from automatic imports. Add them explicitly to `ns`: the method, although verbose, is obvious. Use `load-namespaces` only if you fully understand how namespaces and their loading work.

## System in File

Integrant encourages a declarative approach, because a system configuration is a static dictionary. For space-saving, the system is taken to the EDN resource and read at the start of the application.

You might notice that we have specified references with the `ig/ref` function, and it is not entirely clear how to express them in the file. Integrant extends Clojure reader with the `#ig/ref` tag which acts like the function of the same name.

~~~clojure
{:project.worker/worker {:options {:sleep 1000}
                         :db #ig/ref :project.db/db}}
~~~

Integrant offers its own version of `read-string` for reading EDN. It is a wrapper over the usual `clojure.edn/read-string` with tags from the `#ig/` family. To read the system from a file, do the following:

~~~clojure
(def config
  (-> "config.edn" slurp ig/read-string))
~~~

From the configuration chapter, we remember that it is undesirable to store passwords and access keys in a file. The `project.db/db` component violates this principle: the password for the database is written publicly. Let's make the parser read the password from the environment. For this, we will reuse our previous work for configuration and END.

Let's put the settings into the `integrant.test.edn` file (snippet):

~~~clojure
{:project.db/db {:password #env DB_PASSWORD}
 :project.worker/worker {:options {:sleep 1000}
                         :db #ig/ref :project.db/db}}
~~~

The `ig/read-string` function takes additional EDN tags. Integrant will combine them with its ones when reading the file. Let's wrap the read configuration in the function `load-config`. The underlying `ig/read-string` takes a map with the `#env` tag from the previous chapter. Integrant supplements our map with its own one, so both `#ig/ref` and `#env` will work.

~~~clojure
(defn load-config [filename]
  (ig/read-string {:readers {'env tag-env}}
                  (slurp filename)))

(load-config "integrant.test.edn")
~~~

Here is the result:

~~~clojure
{:project.db/db {:password "c8497b517da25"}
 :project.worker/worker
 {:options {:sleep 1000}
  :db #integrant.core.Ref{:key :project.db/db}}}
~~~

## Key Inheritance

In Clojure, keys can be hierarchical. The `derive` function takes two keys and sets the superiority of the first over the second.

~~~clojure
(derive ::postgresql ::database)
~~~

When the multimethod looks for an action by a key, it takes inheritance into account. If the method is specified for `::database`, calling from `::postgresql` will not result in an error because the `::database` version will work.

Integrant runs on multimethods, so inheritance is useful. Suppose we have a project with two databases: a master for writing and a replica for reading. The `::db-master` and `::db-replica` components differ only in their input parameters.

If we didn't know about inheritance, we would write `ig/init-key` and `ig/halt-key!` for each key. It is considered bad practice to repeat code. We have already described the `::db` component for database sharing. Let's inherit two new ones from it:

~~~clojure
(derive ::db-master ::db)
(derive ::db-replica ::db)
~~~

Let's update the configuration. For the replica, set the `:read-only` flag to protect ourselves from writing to the wrong source. Pay attention to the dependencies. The worker writes data to the database and therefore refers to `::db-master`. The `::hander` component only reads data, so it depends on `::db-replica`.

~~~clojure
(def config
  {::server {:options {:port 8080 :join? false}
             :handler (ig/ref ::handler)}
   ::db-master {;; other fields
                :read-only false}
   ::db-replica {;; other fields
                 :read-only true}
   ::worker {:options {:sleep 1000}
             :db (ig/ref ::db-master)}
   ::handler {:db (ig/ref ::db-replica)}})
~~~

The function and the `ig/refset` tag will return a set of references based on the hierarchy. Let one of the components accepts all databases to perform a service query on them. In order not to refer to each database manually, specify the root key `::db`.

To do this, add the `::db-maintenance` component. It depends on all databases through `refset`. The `init-key` method will return a background task that runs through the databases and executes the query. Its starting and stopping are similar to the worker. The example below shows how to get to both databases from the parameters.

~~~clojure
{::db-maintenance {:dbs (ig/refset ::db)}}

(defmethod ig/init-key ::db-maintenance
  [_ {:keys [dbs]}]
  (future
    (every-1h-interval
      (doseq [db dbs]
        (run-system-query db)))))
~~~

## Other Component Stages

In addition to starting and stopping, Integrant offers other stages of a component. Unlike `init` and `halt!`, their multimethods will return `nil` or an empty action by default (i.e., just return the passed object). To subscribe a component to an event, extend the multimethod with its key. Let's take a look at a few useful stages.

### Preparation

The `ig/prep-key` method prepares parameters. It basically combines them with the default set. Suppose, through trial and error, we have found the ideal database pool metrics. To avoid specifying all fields in the configuration, we will move them to the default map.

~~~clojure
(def db-defaults
  {:auto-commit        false
   :read-only          false
   :connection-timeout 30000
   :idle-timeout       600000
   :max-lifetime       1800000
   :maximum-pool-size  10})

(defmethod ig/prep-key ::db
  [_ options]
  (merge db-defaults options))
~~~

The `prep-key` method combines defaults with the passed parameters. In the config, it is enough to specify only the access fields and, if required, overrides for special cases:

~~~clojure
{::db {:auto-commit   true ;; override the default
       :adapter       "postgresql"
       :username      "book"
       :password      "book"
       :database-name "book"
       :server-name   "127.0.0.1"}}
~~~

The `ig/prep` function takes the configuration and prepares each key. Add this step to our function `load-config` before the system gets started.

### Spec

The `ig/pre-init-spec` method assigns a spec to a component. If the method returns a spec, it will validate the parameters of the component. Make sure we set all the fields in the database connection properly:

~~~clojure
(require '[clojure.spec.alpha :as s])

(s/def :db/username string?)
(s/def :db/database-name string?)
;; host, password, etc

(defmethod ig/pre-init-spec ::db [_]
  (s/keys :req-un [:db/username
                   :db/database-name])) ;; etc
~~~

If we pass invalid parameters, we'll get a Spec error. Its message and a context map are familiar to you from previous chapters.

### Suspending

In addition to starting and stopping, Integrant offers the third state of the system -- `suspended`. In it, the component does not lose its state but only pauses internal processes. If this component is a consumer of messages from a queue, it stops reading the source but does not close the connection. The inverse operation to suspending is `resume`. When resuming, the component continues to work without creating new connections.

By default, these events act like `halt` and `init`. Extend the `ig/suspend-key!` and `ig/resume-key` methods to specify a custom response to `suspend` and `resume`. We confess that suspending a system is required quite rarely, and this why we will skip the details. You may find all the required information in the official documentation of Integrant project in GitHub.

# Summary

Just as a machine is made up of parts, a program consists of components. They are governed by a system -- an agreement on how components are arranged and related to each other.

Every project needs a system, and the longer it develops, the stronger the need. If there is no agreement in the project on how to write the constituent parts, it starts to slow down. Over time, the project will become too expensive to maintain.

Clojure offers several ways to build a system. The most popular libraries are Mount, Component, and Integrant. They offer different approaches so that every developer will find what he or she likes.

Mount relies on global variables. If the project is full of definitions like the one below:

~~~clojure
(def server (run-jetty app {:port 8080}))
~~~

, then porting it to Mount will be easy. The `server` variable will become an entity that changes its value on command. Mount is fine for those new to Clojure.

Component takes a step towards real components. We define entities that isolate the state. Access to a component is determined by the methods that it implements. Components and protocols are similar to objects from modern languages. For this reason, some people accuse the Component of being bloated and too enterprise-like.

Sometimes the solution using components takes up more space than with atoms and functions. On the other hand, it is Component that gives an understanding of how to build sustainable systems. Note, that we discussed most of the system-related questions in the section about Component.

The Integrant project goes further: it is free of the OOP heaviness and is generally more Clojure-friendly. Integrant stands on Clojure idioms and techniques and thus considered as a good choice for experienced developers.

Our goal is not to find out which library is better. Don't rush to rewrite your project from, relatively speaking, Mount to Component or vice versa. This is exhausting work, and you will not understand what benefits you have achieved until you feel the need for them.

Instead of arguing about *which* system is better, think about *why* the project needs it. When the answer is clear, technical solutions will come up by themselves.
