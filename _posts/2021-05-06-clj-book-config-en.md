---
layout: post
title:  "Configuration in Clojure"
permalink: /en/clj-book-config/
tags: clojure book programming configuration
lang: en
---

{% include toc-clj-book-en.md %}

{% include toc.html id="clj-book-exceptions-en" title="In This Chapter" %}

# Configuration

*In this chapter, we will discuss how to make a Clojure project easy to configure. We'll take a look at the basics of config: file formats, environment variables, libraries, and their pros and cons.*

## Formulation of the Problem

In materials on Clojure, there are such examples:

~~~clojure
(def server
  (jetty/run-jetty app {:port 8080}))

(def db {:dbtype   "postgres"
         :dbname   "test"
         :user     "ivan"
         :password "test"})
~~~

These are the server on port 8080 and the parameters for connecting to the database. The examples are useful because you can execute them in the REPL and check their result: open a page in a browser or perform a SQL query.

In practice, we should write code so that it does not carry concrete numbers and strings. Explicitly setting a port number to a server is considered bad practice. That is fine for documentation and examples, but not for the production launch.

Port 8080 and other combinations of zeros and eights are popular with programmers. There is a good chance that the port is occupied by another server. This happens when instead of running one service, you start a bunch of them at once during development or testing.

The code written by a programmer goes through several stages. These stages may differ between companies, but in general, they are development, testing, staging/pre-production, and production.

At each stage, the application runs alongside other projects. The assumption that port 8080 is free anytime is fanciful. In developer slang, the situation is called "hardcode" or "nailed down." If there are nailed-down values in the code, they introduce problems into its life cycle. You cannot run two projects in parallel which declare port 8080 in their code.

The application does not need to know the server port -- information about this comes from the outside. In a simple case, this source is the config file. The program reads the port from it and starts the server exactly as it needs to do on a specific machine.

In more complex scenarios, the file is not compiled by a person but a special program -- a configuration manager. The manager stores information about network topology, machine addresses, and database access parameters. On request, it generate a config file for a specific machine or network segment.

The process of passing parameters to an application and accepting them is called configuration. This step in software development deserves close attention. When it is done well, the project easily goes through all the stages of production.

<!-- more -->

##  Semantics

The purpose of a config is to control the program without changing the code. The need for it arises with the growth of the code base and infrastructure. If you have a small Python script, there is nothing wrong with opening it in notepad and changing a constant. At enterprises, such scripts have been working for years.

But the more complex a company's infrastructure, the more constraints it has. Today's software development practices negate spontaneous changes in a project. You can't `git push` directly to the master branch; `git merge` is prohibited until at least two colleagues approve your work; an application will not reach the server until tests pass.

This leads to the fact that even a slight change in the code will take hours to get in production. Editing in configuration is cheaper than releasing a new version of the product. The rule follows from this: if you can make something a configurable option, do it right now.

Large companies practice what is called a feature flag. It is a boolean field that enables a vast layer of the application logic. For example, a new interface, a ticket processing system, or an improved chat. Of course, updates are tested before releasing them, but there is always a risk of something going wrong in production. In this case, we set the flag to false and restart the service. Thus, the company will not only save time but also preserve its reputation.

## Configuration Cycle

The better an application is designed, the more of its parts rely on parameters. That's why, on startup, the program immediately looks for configuration. Processing of configuration is a collection of steps, not a monolithic task. Let's list the most important of them.

At the first stage, the program **reads the configuration**. Most often, they are environment variables or a file. Data in a file is stored in JSON, YAML, and other formats. An app contains code to parse a format and get the data. We'll look at the pros and cons of the well-known formats below.

Environment variables are part of an operating system. Think of them as a global map in memory. Every application inherits it when starting. Languages and frameworks offer functions to read variables into strings and maps.

Files and environment variables complement each other. For example, an application reads data from a file but looks for its path in environment variables. There might be an opposite approach. Sensitive data such as passwords and API keys are omitted in the file. So, other programs, including spyware, won't see them. The application reads normal parameters from a file, but the secret information comes from variables.

Advanced configurations use tags. In the file, the tag is placed before the value: `:password #env DB_PASSWORD`. A tag is a short string meaning that the next value is processed specially. In our example, the `password` field contains not the `DB_PASSWORD` string but the value of the same name variable.

The first stage ends when we have received the data. It doesn't matter if it was a file, environment variables, or something else. The application moves on to the second stage, **type inference**.

JSON and YAML have basic types: strings, numbers, booleans, and `null`. It is easy to see that there is no date among them. We use dates to define promotions or calendar events. In files, dates are specified either as an ISO string or as the number of seconds since January 1, 1970 ([UNIX era](https://en.wikipedia.org/wiki/Unix\_time)). Specially designed code runs through the data and converts dates to the type accepted in the language.

Type inference applies to collections as well. Sometimes maps and arrays are not enough to work comfortably. For example, possible types of something are stored as a set because it cuts off duplicates and quickly validates if a value belongs to it. It's easier to describe some complex types with plain values (strings, numbers) and coerce them later. A string `http://test.com` will become an instance of `java.net.URL`, and a sequence of 36 hexadecimal characters will be a `UUID`.

Environment variables are less flexible than modern formats. JSON provides scalars and collections, while variables contain nothing but text. Type inference is not only desirable, but necessary for them. You cannot pass a port as a string to where a number is expected.

**Data validation** starts after type inference. In the chapter on Spec, we found out that a proper type does not promise a correct value. Validation is needed to make it impossible to specify port 0, -1, or 80 in the configuration.

From the same chapter, we remember that sometimes the values are correct individually but cannot be paired. Suppose we specified the promotion period in the configuration. It is an array of two dates: start and end ones. These dates may be be easily confused, and then checking of any date against an interval will return false.

After validation, proceed to the last stage. The application decides where **to store the configuration**, for example, in a global variable or a system component. Other parts of the program will read parameters from there, not from the file.

## Config Errors

At each stage, an error may occur, e.g., file not found, syntax violations, invalid field. In this case, the program displays a message and exits. The text should explicitly answer the question of what happened. Too often, programmers keep in mind only the positive path and forget about errors. When running their programs, you see a stack trace that is difficult to understand.

If an error occurred during the verification stage, explain which field was a culprit. In the chapter on Spec, we looked at how to improve a spec report. It takes effort but pays off over time.

In the IT industry, some people write code, and others manage it. Your DevOps colleagues don't know Clojure and won't understand the raw `s/explain`. Sooner or later, they will ask you to improve the configuration messages. Do this in advance out of respect for your colleagues.

If there is something wrong with the config, then the program should terminate immediately rather than work, hoping that everything will settle somehow. Sometimes one of the parameters is specified incorrectly, but the program does not use it for the time being. Avoid this: the error will appear at the most inopportune moment.

If one of the configuration steps fails, the program should exit with nonzero code. The message is sent to the `stderr` channel to signal an abnormal condition. Advanced terminals print text from `stderr` in red to catch your attention.

## Configuration Loader

To reinforce theory with practice, let's write our configuration system. It will be a separate module of about one hundred lines. Before opening the editor, let's think over the main points.

Let's store the configuration in a JSON file. We'll assume that the company has recently switched to Clojure, and DevOps has already written Python scripts to manage configuration settings. Of course, EDN would be the best choice for Clojure programs, but it will complicate work for our colleagues, so we'll not use it for now.

The path to the config file is specified by the `CONFIG PATH` environment variable. From the file, we expect to get a server port, database parameters, and promotion date range. Dates should become `java.util.Date` objects. The start date is strictly less than the end date.

We will put the final map into the global variable `CONFIG`. If an error occurs at one of the steps, we will show a message and exit the program.

Let's start with the `exit` helper function. It takes a completion code, a text, and formatting options. If the code is equal to zero, write the message to `stdout`, otherwise -- to `stderr`.

~~~clojure
(defn exit
  [code template & args]
  (let [out (if (zero? code) *out* *err*)]
    (binding [*out* out]
      (println (apply format template args))))
  (System/exit code))
~~~

Now let's move on to the loader. It is a set of steps, where each one takes the result of the previous one. The logic of the steps is easy to understand from their name. Namely, there are four actions: finding the path to the config, reading a file, infer data types, and setting a global variable. Type coercion and validation were combined into `coerce-config` since, technically, this is the `s/conform` call.

~~~clojure
(defn load-config! []
  (-> (get-config-path)
      (read-config-file)
      (coerce-config)
      (set-config!)))
~~~

Now we will describe each step. The `get-config-path` function reads an environment variable and checks if such a file exists on disk. If everything is okay, the function will return the file path; otherwise, it will call `exit`:

~~~clojure
(import 'java.io.File)

(defn get-config-path []
  (if-let [filepath (System/getenv "CONFIG_PATH")]
    (if (-> filepath (new File) .exists)
      filepath
      (exit 1 "File %s does not exist" filepath))
    (exit 1 "File path is not set")))
~~~

The `read-config-file` step reads the file by its path. The Cheshire library parses JSON. Its `parse-string` function returns data from a document string.

~~~clojure
(require '[cheshire.core :as json])

(defn read-config-file
  [filepath]
  (try
    (-> filepath slurp (json/parse-string true))
    (catch Exception e
      (exit 1 "Malformed config, file: %s, error: %s"
            filepath (ex-message e)))))
~~~

Type inference and validation are the most important steps. The application must not receive invalid parameters. The `coerce-config` step passes data from the file through `s/conform`. There is a chance of getting an exception when calling it, so wrap it in `pcall` -- a safe call that will return an error and the result.

If there was an exception, we print its message and terminate the program. The same applied to the case when we got `::s/invalid` keyword. The only difference is, we compose the message with the Expound library. We have to consider both cases because a failure and an incorrect result are different things.

~~~clojure
(require '[clojure.spec.alpha :as s])
(require '[expound.alpha :as expound])

(defn coerce-config [config]
  (let [[e result] (pcall s/conform ::config config)]
    (cond
      (some? e)
      (exit 1 "Wrong config values: %s" (ex-message e))

      (s/invalid? result)
      (let [report (expound/expound-str ::config config)]
        (exit 1 "Invalid config values: %s %s"
              \newline report))

      :else result)))
~~~

Now, only a spec is missing. Let's open the configuration and examine its structure:

~~~json
{
    "server_port": 8080,
    "db": {
        "dbtype":   "mysql",
        "dbname":   "book",
        "user":     "ivan",
        "password": "****"
    },
    "event": [
        "2019-07-05T12:00:00",
        "2019-07-12T23:59:59"
    ]
}
~~~

Describe the spec from top to bottom. It is a map with the keys:

~~~clojure
(s/def ::config
  (s/keys :req-un [::server_port ::db ::event]))
~~~

The server port is a combination of two predicates: a number check and a range check. Checking for a number is needed so that `nil` and a string do not get into the second predicate. Otherwise, this will throw an exception where you least expect it.

~~~clojure
(s/def ::server_port
  (s/and int? #(<= 1024 % 65535)))
~~~

We meet number and range checks frequently, so Spec offers the `s/int-in` macro for this case. Please note that the right border is exclusive, meaning that it belongs to the interval. The mathematical notation for such an interval is written like `[1024, 65535)`.

~~~clojure
(s/def ::server_port
  (s/int-in 1024 (inc 65535)))
~~~

Now let's describe the database connection. There won't be any problems with it, because all its fields are strings. For more rigor we use `::ne-string` to prevent empty lines. The database engine is specified as a enumeration of strings with the only item <<mysql>>. This will eliminate extraneous values.

~~~clojure
(s/def :db/dbtype   #{"mysql"})
(s/def :db/dbname   ::ne-string)
(s/def :db/user     ::ne-string)
(s/def :db/password ::ne-string)

(s/def ::db
  (s/keys :req-un [:db/dbtype
                   :db/dbname
                   :db/user
                   :db/password]))
~~~

The `event` field is the most challenging one. It consists of a tuple of dates and an interval check:

~~~clojure
(s/def ::event
  (s/and (s/tuple ::->date ::->date)
         ::date-range))
~~~

The `s/tuple` spec validates if a collection has exact number of items. In our case, a vector of one or three dates won't pass it. The `::->date` spec converts a string to a date. In order not to parse it manually, let's take the `read-instant-date` function from the `clojure.instant` package. This function is format-tolerant and reads incomplete dates, for example, only a year. Let's wrap it in `s/conformer`. We put `::ne-string` in front to cut off the non-date garbage.

~~~clojure
(require '[clojure.instant :as inst])

(s/def ::->date
  (s/and ::ne-string (s/conformer read-instant-date)))
~~~

Let's describe range checking. It takes a couple of `Date` objects and compares them. Dates cannot be compared using "greater than" or "less than" signs. Instead, use the `compare` function, which will return -1, 0, and 1 for the less than, equal or greater than cases, respectively. We are interested in the first case when the result is negative.

~~~clojure
(s/def ::date-range
  (fn [[date1 date2]]
    (neg? (compare date1 date2))))
~~~

The last step is `set-config!` that writes the map to the global `CONFIG` variable. We chose an uppercase name to avoid shadowing it with the local one `config`. To change a global variable, use `alter-var-root`.

~~~clojure
(def CONFIG nil)

(defn set-config!
  [config]
  (alter-var-root (var CONFIG) (constantly config)))
~~~

At the start of the program, execute `(load-config!)` so that the configuration appears in the variable. Other modules import `CONFIG` and read the keys they need. Below is how to start a server or execute a request based on configuration:

~~~clojure
(require '[project.config :refer [CONFIG]])

(jetty/run-jetty app {:port (:server_port CONFIG)
                      :join? false})

(jdbc/query (:db CONFIG) "select * from users")
~~~

If there is something wrong with your configuration, the program will terminate with a clear message.

### Improvements

We have written a configuration loader. It is simple to maintain: every step is a function that is easy to modify. Our code does not pretend to be an industrial solution, but it is suitable for small projects.

Its advantage is that the configuration can be re-read at any time. This is handy for development: modify the file and run `load-config!` in the REPL. A new configuration appears in the `CONFIG` variable.

The downside of the loader is that the code is bound to the `exit` function, which terminates a JVM. In production, this is the right approach: you cannot continue if the parameters are misconfigured. In development, a termination is more of a problem than a benefit: any error kills the REPL, and you need to start it again.

The termination of a JVM is too drastic. We should separate an error and reaction to it. The naive way is to call `load-config!` while the `exit` is being redefined with a function that only throws an exception. Let's name it `fake-exit`. The code below will not terminate the JVM; it will only throw an exception with the text that we passed to `exit`:

~~~clojure
(defn fake-exit
  [_ template & args]
  (let [message (apply format template args)]
    (throw (new Exception ^String message))))

(defn load-config-repl! []
  (with-redefs [exit fake-exit]
    (load-config!)))
~~~

A better solution is to pass additional parameters to `load-config!`. Let's call one of them `die-fn` (the "death function") that takes an exception. In production, it terminates the JVM, and in development, it writes a message to the REPL. Modify the loader to support the `:die-fn` parameter. Consider default behavior if the parameter is not specified.

Another point that addresses the issue of inferring types. The loader relies on the `s/conform` function for type inference. In the chapter on Spec, we looked at the case when `s/conform` adds logical tags and changes the data structure. If we replace our custom `::db` spec with the `::jdbc/db-spec` one, we will get the same case. We have set our database spec without `s/or` macros in order not to distort the data.

In another way, you can coerce types using tags. We will discuss this technique in the following sections of this chapter.

## More on Environment Variables

A loader reads data from a file, taking only a small part -- the file path -- from environment variables. Let's modify the loader: now it reads all data from the environment without using files. To know better the advantages of the new approach, let's discuss it first in isolation from any specific language.

Environment variables are sometimes called ENV for short, for example, when reading a file of the same name or working with them in the code. This is a fundamental property of the operating system. Think of variables as a global map that is populated at a computer startup. The map contains the main system parameters: locale, home directory, a list of paths where the system looks for programs, and much more.

To see the current variables, run `env` or `printenv` in a terminal. The pairs `NAME=value` will appear on the screen. Variable names are in uppercase to make them stand out and emphasize their priority. Most systems are case sensitive, so `home` and `HOME` are different variables. Spaces and hyphens are not allowed; lexemes are separated by underscores. Here's a snippet of `printenv`:

~~~bash
USER=ivan
PWD=/Users/ivan
SHELL=/bin/zsh
TERM_PROGRAM=iTerm.app
~~~

Each process receives a copy of this map. A process can add or remove a variable, but the changes are visible only to it and its descendants. A child process inherits the variables from its parent.

### Local and Global Variables

Distinguish between environment and shell variables; they are also called global and local variables. Newbies often confuse them. Run the command in the terminal:

~~~bash
$ FOO=42
~~~

You have set a shell variable. To refer to a value by name, precede it with a dollar sign. The example below will print 42:

~~~bash
$ echo $FOO
42
~~~

If we execute `printenv`, we won't see `FOO` in the output. The `FOO=42` instruction sets a shell variable, not an environment variable. These variables are only visible to the shell, and its descendants do not inherit them. Let's check it: start a new one from the current shell and repeat printing.

~~~bash
$ sh
$ echo $FOO
~~~

We get an empty string because the child does not have such a variable. Run `exit` to return to the parent shell.

The `export` command puts a variable into the environment. Printenv sees the variable set this way:

~~~bash
$ export FOO=42
$ printenv | grep FOO
FOO=42
~~~

The child processes also see it:

~~~bash
$ sh
$ echo $FOO
42
~~~

Sometimes you need to start a process with a variable but so as not to affect the current state. In such a situation, you should place the expression `NAME=value` before the basic command:

~~~bash
$ BAR=99 printenv | grep BAR
BAR=99
~~~

`Printenv` generates a new process that has access to the `BAR` variable. If we print `$BAR` once again, we'll get an empty string.

Programs often read parameters from environment variables. A PostgreSQL client distinguishes between two dozen variables: `PGHOST`, `PGDATABASE`, `PGUSER`, and others. Environment variables take precedence over `--host`, `--user`, and similar parameters. If you execute the following in the current shell:

~~~bash
$ export PGHOST=host.com PGDATABASE=project
~~~

then each PostgreSQL utility will run on the specified server and database. This is convenient for a series of commands: you don't have to specify `--host` and other arguments every time.

Pay attention to the `PG` prefix. It prevents overwriting someone else's `HOST` variable. There are no namespaces in the environment, so the prefix is the only way to separate your variables from others.

## Config in the Environment

Each language provides functions to read a single variable to a string or get all of them as a map. It means we can set config with environment variables. Let's look at the pros and cons of this approach.

The application does not access the disk while reading the environment since it is located in memory. We're not aiming at the performance benefits, though. Yes, memory is much faster than disk, but you will never notice the difference between 0.01sec and 0.001sec. Our main point is that an application that does not depend on files is more autonomous and easier to maintain.

Sometimes a configuration file is unexpectedly located in a different folder, and an application cannot find it, or worse, the app starts up with an old file version. This makes things slower and more confusing.

Storing passwords and keys in variables is safer than in files. These data can be read in files by other programs, including malware. By mistake, a file can get into the repository and remain in history. Some scripts search open repositories for keys to cloud platforms and wallets (and sometimes find them, unfortunately).

Even if a file belongs to a user, others can get read access. Environment variables are ephemeral: they live only in operating system memory. One user cannot read another's variables -- this is a strict limitation at the operating system level.

The industry is moving from files to virtualisation. If earlier we copied files via FTP, today applications are running from images. They are archives that contain the code and its environment. Unlike a regular archive, we cannot change an image. To update a file in the image, you need to rebuild it, which complicates the process.

On the contrary, virtualisation is loyal to the environment variables. They are specified in the parameters when you start the image. The same image is used with different variables, so a new build is not required. The more options you can set with variables, the more convenient it is to work with the image. In the example below, the PostgreSQL server starts with a ready-to-use database and a user:

~~~bash
$ docker run \
  -e POSTGRES_DB=book \
  -e POSTGRES_USER=ivan \
  -e POSTGRES_PASSWORD=**** \
  -d postgres
~~~

The [Twelve-Factor App](https://12factor.net) is a famous set of rules for developing robust applications. It also prescribes storing configuration in the environment. The author mentions the same advantages of variables that we have looked at -- file independence, security, and support on all platforms.

## Disadvantages of the Environment

Variables do not support types: any value is text. Type inference is up to you. Do it declaratively, not manually. Here's a bad example in Python:

~~~python
db_port = int(os.environ["DB_PORT"])
~~~

When there are more than two variables, the code becomes ugly. Specify a map where a key is a variable name and value is a function to transform a text value. The special code traverses the map and fills up the result. For the sake of shortness, let's skip error handling:

~~~python
import os
env_mapping = {"DB_PORT": int}

result = {}
for (env, fn) in env_mapping.iteritems():
    result[env] = fn(os.environ[env])
~~~

The approach is also valid for other languages: less code, more of the declarative part. In Clojure, we usually transform the data with spec.

Environment variables do not work with hierarchy. They are a flat set of keys and values that is not always suitable for config. The more parameters the configuration has, the more often they are grouped by meaning. Let's say ten parameters define the connection to the database. We'll take them out to the child map in order not to put a prefix in front of each.

~~~clojure
;; so-so
{:db-name "book"
 :db-user "ivan"
 :db-pass "****"}
~~~

vs

~~~clojure
;; better
{:db {:name "book"
      :user "ivan"
      :pass "****"}}
~~~

Nested variables are read differently on different systems. For example, a single underscore separates lexemes but does not change the structure. Double underscore stands for nesting:

~~~clojure
DB_NAME=book
DB_PASS=pass

{:db-name "book"
 :db-pass "pass"}
~~~

and

~~~clojure
DB__NAME=book
DB__PASS=pass

{:db {:name "book"
      :pass "pass"}}
~~~

An array is specified in square brackets or separated by commas. When parsing one, there is a risk of false splitting. This happens when the comma or a bracket refers to a word, not syntax.

The JSON and YAML formats set a clear standard for how to describe collections. But there is no single convention for environment variables. The situation gets more complicated when a highly nested parameter is expected, such as a list of dictionaries. Environment variables do not fit well with such a structure.

The development reveals one more trade-off of these variables: they are read-only on some systems. That is ideologically true, but it forces you to re-enable the REPL for every configuration change, whereas the file only needs to be changed and read again.

### Env Files

When there are many variables, entering them manually via `export` is tiresome. In such situations, we move the variables to a file called the env-configuration. Technically, it is a shell script, but the less scripting capabilities it has, the better. Ideally, such a file holds only `NAME=value` pairs, one for each line. Let's just call it `ENV` without extension.

~~~bash
DB_NAME=book
DB_USER=ivan
DB_PASS=****
~~~

To read the variables into the shell, call `source <file>`. It is a `bash` command that will execute the script in the current session. The shorthand for this often-used command is a dot: `. <file>`. The script will add variables to the shell, and you will see them after `source`. This is an important difference from `bash <file>` command, which will execute the script in a new shell, and you won't see any changes in the current one.

~~~bash
$ source ENV
$ echo $DB_NAME
book
~~~

If you run the application from the current shell, the app still won't get the variables from the file. Recall that the expression `VAR=value` defines a local variable. `DB_NAME` and other variables will not get into the environment, and the program will not inherit them. Let's check this with `printenv`:

~~~bash
$ source ENV
$ printenv | grep DB
# exit 1
~~~

You can solve the problem in two ways. The first is to open the file and place the `export` expression before each pair. Then the `source` command of this file will add variables to the environment:

~~~bash
$ cat ENV
export DB_NAME=book
export DB_USER=ivan
export DB_PASS=****

$ source ENV
$ printenv | grep DB
DB_NAME=book
DB_USER=ivan
DB_PASS=****
~~~

The disadvantage of this method is that now the file has become a script. If you do not put `export` before a variable, the application will not read it.

The second way is based on the `-a` (**a**llexport) parameter of the current shell. When it is set, the local variable is sent to the environment as well. Before reading variables from a file, set the flag to "true" and then to "false" again.

~~~bash
$ set -a
$ source ENV
$ printenv | grep DB
# prints all the vars
$ set +a
~~~

The `set` statement is counterintuitive: the parameter is enabled with a minus and disabled with a plus. This is an exception to remember.

If you read a variable that is already in the environment, it will replace the previous value. This way, files with overrides appear. If you need particular settings for your tests, you don't have to copy the entire file. Create a file with the fields to be replaced and execute it after the main one.

Let the test settings of our program differ by the base name. The `ENV` file contains the main parameters, and in `ENV_TEST` we put a single pair `DB_NAME=test`. Let's read both files and see how it turned out:

~~~bash
$ set -a
$ source ENV
$ source ENV_TEST
$ set +a

$ echo $DB_NAME
test
~~~

You can notice that using ENV files is contrary to the statement above. We said that variables remove the dependency on files, but in the end, we put them in a file. Why?

The difference between JSON and ENV files is what reads them. In the first case, an application does it, and in the second case, an operating system. A file is located in a strictly defined directory, whereas environment variables are available from everywhere. We will free the application from the code that looks for and reads the file. At the same time, we will make it easier for our DevOps colleagues: they set variables differently depending on the tool (shell, Docker, Kubernetes). This makes the environment the main exchange point of all settings.

##  Environment Variables in Clojure

Clojure is a hosted platform, so the language does not provide access to system resources. There is no function for reading environment variables in its core module. Let's get them from the `java.lang.System` class. You don't need to import the class: it is available in any namespace.

The static `getenv` method will return either one variable by name or the entire map if no name is specified.

~~~clojure
;; a single variable
(System/getenv "HOME")
"/Users/ivan"

;; all variables
(System/getenv)
{"JAVA_ARCH" "x86_64", "LANG" "en_US.UTF-8"} ;; truncated
~~~

In the second case, we got not a Clojure collection but a Java one. It is an instance of `UnmodifiableMap` class, so the variables cannot be changed after the JVM has started.

Let's cast the map to the Clojure type to make it easier to work with it. At the same time, we will fix the keys: at the moment, these are uppercase strings with underscores. Clojure uses keywords and kebab-case: lowercase with hyphens.

Let's write a function to convert a single key:

~~~clojure
(require '[clojure.string :as str])

(defn remap-key [^String key]
  (-> key
      str/lower-case
      (str/replace #"_" "-")
      keyword))
~~~

and make sure that it works correctly:

~~~clojure
(remap-key "DB_PORT")
:db-port
~~~

The `remap-env` function traverses the Java map and returns its Clojure version with keywords for keys:

~~~clojure
(defn remap-env [env]
  (reduce
   (fn [acc [k v]]
     (let [key (remap-key k)]
       (assoc acc key v)))
   {}
   env))
~~~

Here is a small part of the map:

~~~clojure
(remap-env (System/getenv))

{:home "/Users/ivan"
 :lang "en_US.UTF-8"
 :term "xterm-256color"
 :java-arch "x86_64"
 :term-program "iTerm.app"
 :shell "/bin/zsh"}
~~~

Now that we have a map of variables, it follows the same pipeline: type inference, validation with a spec. Since all values are strings, the spec needs to be modified so that it converts strings to proper types. Previously, there was no need for this because the numbers came from JSON. Let's make a better spec that considers both number and string types for numeric values. A smart number parser looks like this:

~~~clojure
(s/def ::->int
  (s/conformer
   (fn [value]
     (cond
       (int? value) value
       (string? value)
       (try (Integer/parseInt value)
            (catch Exception e
              ::s/invalid))
       :else ::s/invalid))))
~~~

With this spec, you can change the data source without editing the code.

### Extra Keys Problem

The variable map has the disadvantage of many extraneous fields. The application doesn't need to know the terminal version or the path to Python. These fields introduce noise during printing and logging. If the spec fails, we'll see excessive data in `explain`.

In the last step of `s/conform`, you need to select only the useful data part from the map. The `select-keys` function will return a subset of another map with only the keys passed to the second argument. But where to get the keys? It takes a long time to list them manually, and besides, we duplicate the code. We have already specified the keys in the `::config` spec, and we don't want to do this a second time. We'll use a trick to get the keys out of the spec.

The `s/form` function takes a spec key and returns the frozen form of whatever was passed to `s/def`. We will get a list where each item is a primitive or a collection of primitives (number, string, symbol, and others). For the `::config` spec, we'll get the following form:

~~~clojure
(s/form ::config)

(clojure.spec.alpha/keys
 :req-un [:book.config/server_port
          :book.config/db
          :book.config/event])
~~~

Please note: this is a list indeed, not a code. The keys you need are in the third item after the `:req-un` keyword. We should consider other types of keys, for example, `:opt-un`. Let's write a universal function that will return all keys from the `s/keys` spec.

We'll drop the first symbol of the form. That leaves a list, where the odd items are the type of keys, and the even ones are their vector. Let's rebuild the list into a map and combine the values. For `-un` keys, discard the namespace. As a result of these actions, we get the function:

~~~clojure
(defn spec->keys
  [spec-keys]
  (let [form (s/form spec-keys)
        params (apply hash-map (rest form))
        {:keys [req opt req-un opt-un]} params
        ->unqualify (comp keyword name)]
    (concat req
            opt
            (map ->unqualify opt-un)
            (map ->unqualify req-un))))
~~~

Let's check the spec of our loader. Indeed, we get three keys:

~~~clojure
(spec->keys ::config)
(:server_port :db :event)
~~~

Let's rewrite reading variables into the map. In the last step, we select only those keys that we declared in our spec.

~~~clojure
(defn read-env-vars []
  (let [cfg-keys (spec->keys ::config)]
    (-> (System/getenv)
        (remap-env)
        (select-keys cfg-keys))))
~~~

The advantage is that we managed to avoid repetitions. If a new field appears in `::config`, the `spec->keys` function will automatically pick it up.

### Environment Loader

Let's modify the loader to work with environment variables. Replace the first two steps with `read-env-vars`. Now the program does not depend on the config file.

~~~clojure
(defn load-config! []
  (-> (read-env-vars)
      (coerce-config)
      (set-config!)))
~~~

Make it so the data source can be specified using a parameter. For example, `:source "/path/to/config.json"` means read the file, and `:source :env` means environment variables.

An even more difficult problem is how to read both sources and combine them? Is the order important, and how to ensure it? How to combine maps asymmetrically, that is, when the second map only replaces the fields of the first one but does not add new fields?

### Inference of Structure

It rarely happens that a configuration is a flat dictionary. Parameters related by their meaning are placed in nested dictionaries; for example, server and database fields are separate. When the settings are in a group, they are easier to maintain. A good example is splitting config into pieces using `{:keys [db server]}` syntax. Each component of the system accepts the part of the same name as a mini config.

Let's improve our loader: we will teach it to read nested variables. Let's agree that double underscore means a level change. We'll put the following variables
in the `ENV_NEST` file:

~~~bash
DB__NAME=book
DB__USER=ivan
DB__PASS=****
HTTP__PORT=8080
HTTP__HOST=api.random.com
~~~

Now read it and start the REPL with the new environment:

~~~bash
$ set -a
$ source ENV_NEST
$ lein repl
~~~

Let's change the parsing of the environment. The `remap--key--nest` function takes a string key and returns a vector of its constituent parts (lexemes):

~~~clojure
(defn remap-key-nest
  [^String key]
  (-> key
      str/lower-case
      (str/replace #"_" "-")
      (str/split #"--")
      (->> (map keyword))))

(remap-key-nest "DB__PORT")
;; (:db :port)
~~~

Now we change the function that builds a map. For each name, we will get a vector of lexemes. Let's add a value with `assoc-in` that produces a nested structure.

~~~clojure
(defn remap-env-nest
  [env]
  (reduce
   (fn [acc [k v]]
     (let [key-path (remap-key-nest k)]
       (assoc-in acc key-path v)))
   {}
   env))
~~~

The code below will return the parameters grouped as expected. Here is a subset of them:

~~~clojure
(-> (System/getenv)
    (remap-env-nest)
    (select-keys [:db :http]))

{:db {:user "ivan", :pass "****", :name "book"},
 :http {:port "8080", :host "api.random.com"}}
~~~

Then we act as usual: write a spec, infer types from strings, and so on.

Think about setting an array in a variable. How to separate array elements? When is false splitting possible, and how to prevent it?

## Simple configuration manager

At this point, you might decide that config in a file is a bad idea. However, don't rush to rewrite your code with environment variables. In practice, *hybrid* models are used combining both approaches. The application reads basic parameters from a file, but passwords and API keys from the environment.

Let's look at how to use both files and environments. A naive solution doesn't require you to write any code: it runs on command-line utilities. The `envsubst` program from the "GNU gettext" package provides a simple templating system. To install `gettext`, run the command in a terminal:

~~~bash
$ <manager> install gettext,
~~~

, where `<manager>` is your system's package utility (`brew`, `apt`, `yum`, and others).

The template text comes from `stdin`, and the environment variables are the context. The utility replaces the `$VAR_NAME` expressions with the values of the same name variable. Let's put the template into the `config.tpl.json` file. The "tpl" part means a template.

~~~json
{
    "server_port": $HTTP_PORT,
    "db": {
        "dbtype":   "mysql",
        "dbname":   "$DB_NAME",
        "user":     "$DB_USER",
        "password": "$DB_PASS"
    },
    "event": [
        "$EVENT_START",
        "$EVENT_END"
    ]
}
~~~

Note that the server port is not quoted because it is a number (line 2). Now we create an env `ENV_VARS` file with the following content:

~~~bash
$ cat ENV_VARS
DB_NAME=book
DB_USER=ivan
DB_PASS='secret123'
HTTP_PORT=8080
EVENT_START='2019-07-05T12:00:00'
EVENT_END='2019-07-12T23:59:59'
~~~

Let's read them and render the template:

~~~bash
$ source ENV_VARS
$ cat config.tpl.json | envsubst
~~~

The substitution was successful:

~~~json
{
    "server_port": 8080,
    "db": {
        "dbtype":   "mysql",
        "dbname":   "book",
        "user":     "ivan",
        "password": "*(&fd}A53z#$!"
    },
    "event": [
        "2019-07-05T12:00:00",
        "2019-07-12T23:59:59"
    ]
}
~~~

To write the result to a file, add an output statement to the end:

~~~bash
$ cat config.tpl.json | envsubst > config.ready.json
~~~

The `envsubst` method seems primitive, but it is useful in practice. The template frees you from worries about the structure: variables are in the right places, so no trouble with nesting.

Sometimes an application requires multiple config files, including one for infrastructure. You need to specify the same parameter in different files to make the programs work in concert. For example, Nginx requires a web server port for proxying. In Sendmail, you need to specify the same email address as in the application. It goes without saying that there should be a single data source, and a template render can be such a source.

The `envsubst` utility becomes the configuration manager. To automate the process, add a script that runs templates and renders them based on variables. It is not an enterprise-level solution, but it is suitable for simple projects.

##  Reading the Environment from Config

The following techniques make an application read parameters from file and environment simultaneously. The difference is at what step it happens.

Suppose we put the main parameters in a file, and the password for the database comes from the environment. Since such solutions are team-wide, agree among yourselves that the `password` field contains not a password, but a variable name, for example, `"DB_PASS"`. Let's write a spec that infers the variable value by its name:

~~~clojure
(s/def ::->env
  (s/conformer
   (fn [varname]
     (or (System/getenv varname) ::s/invalid))))
~~~

If the variable is not set, the output will return an error. For more control, remove the white space around the edges and make sure the string is not empty.

~~~clojure
(s/def ::db-password
  (s/and ::->env
         (s/conformer str/trim)
         not-empty))
~~~

A quick test: run the REPL with the `DB_PASS` variable and read it using the spec:

~~~bash
DB_PASS='secret123' lein repl

(s/conform ::db-password "DB_PASS")
"secret123"
~~~

To move a field out of the file to the environment, replace its value with the variable name. Update the spec for this field: add `::->env` to the beginning of the `s/and` chain.

Another way to read variables from a file is to expand it with tags. A tag is a short word that indicates that the meaning behind it is read in a certain way. YAML and EDN formats support tags. Libraries offer several basic ones for them. You can easily add your own tag.

In EDN, a tag starts with a hash sign and captures the next value. For example, `#inst "2019-07-10"` converts a string to a date. The tag is associated with a single argument function that finds a value from the initial one. To set your tag, pass a special map to the `clojure.edn/read-string` function. Its keys are symbols, and values are functions.

Add the `#env` tag that will return the value of the variable by name. The name can be a string or a symbol. Let's define a function:

~~~clojure
(defn tag-env
  [varname]
  (cond
    (symbol? varname)
    (System/getenv (name varname))
    (string? varname)
    (System/getenv varname)
    :else
    (throw (new Exception "Wrong variable type"))))
~~~

Now we'll read the EDN line with the new tag:

~~~clojure
(require '[clojure.edn :as edn])

(edn/read-string {:readers {'env tag-env}}
                 "{:db-password #env DB_PASS}")
;; {:db-password "secret123"}
~~~

To avoid passing the tags every time, let's prepare the `read-config` function "charged" with the tags. We build it using `partial`. The new function accepts only a string:

~~~clojure
(def read-config
  (partial edn/read-string
           {:readers {'env tag-env}}))
~~~

To parse a file with tags, read it into a string and pass it to `read-config`:

~~~clojure
(-> "/path/to/config.edn"
    slurp
    read-config)
~~~

YAML tags start with one or two exclamation marks, depending on the semantics. Standard tags have two marks, while third-party tags have one. This way, when we run into a tag, we immediately understand its semantics.

The Yummy library offers a YAML parser that has useful tags. Among others, we are interested in the `!envvar` tag, which returns the value of a variable by name. Let's describe the configuration in the `config.yaml` file:

~~~yaml
server_port: 8080
db:
  dbtype:   mysql
  dbname:   book
  user:     !envvar DB_USER
  password: !envvar DB_PASS
~~~

Let's add the library and read the file. In place of the tags, we get the environment values:

~~~clojure
(require '[yummy.config :as yummy])

(yummy/load-config {:path "config.yaml"})

{:server_port 8080
 :db {:dbtype "mysql"
      :dbname "book"
      :user "ivan"
      :password "*(&fd}A53z#$!"}}
~~~

We'll take a closer look at Yummy in the next section of the chapter.

Tags have both advantages and disadvantages. On the one hand, they make the config more concise: a line with a tag makes more sense. An expression like `#env DB_PASS` is shorter and more pleasing to the eye. Some libraries provide tags for complex types and classes.

On the other hand, tags make a config platform-specific. For example, the Python library fails to read the `!envvar` tag in the YAML file because this library does not have such a tag (more precisely, it does, but with a different name). Technically, this can be fixed: skip unfamiliar tags or install a stub. However, the approach does not guarantee the same results across platforms.

With tags, a config is overgrown with side effects. In functional programming terms, it loses its purity. It is tempting to move too much logic into a tag: include a child file, format strings. Tags blur the line between reading a config and processing it. When there are too many of them, the configuration is difficult to maintain.

These techniques — parsing with spec and tags — are opponents. Choose the method that is convenient for the team and process.

## Overview of Formats

We have mentioned three data formats: JSON, EDN, and YAML. Let's run through the features of each of them.  Our goal is not to identify the ideal format but to prepare you for the unobvious moments that arise while working with these formats.

### JSON

Even non-web developers are familiar with JSON. It is a data format based on JavaScript syntax. The standard's basic types are numbers, strings, boolean, null, and two collections -- an array and object, which is considered as a map. The collections can be nested within each other.

The advantage of JSON is its popularity. Today it is the standard for exchanging data between client and server.  It is easier to read and maintain than XML. Today's editors, languages, and platforms work with JSON. It is the natural way to store data in JavaScript.

But JSON does not provide an opportunity to comment. At first glance, this is a trifle, but in practice, comments are important to us. If you have added a new parameter, you should write a comment about what it does and what values it takes. Look at Redis, PostgreSQL, or Nginx configurations -- more than half of the file are comments.

Developers have come up with tricks to get around this limitation in JSON.  For example, put the same name field in front of the one to which the comment relates:

~~~json
{
    "server_port": "A port for the HTTP server.",
    "server_port": 8080
}
~~~

We expect the library to walk through the fields in turn, and the second field will replace the first. The JSON standard does not specify the order of the fields, so proceed at your own risk. The library logic can be different, for example, to throw an exception or skip an already processed key.

Some programs carry their own JSON parser that supports comments. For example, Sublime Text editor stores settings in `.json` files with JavaScript comments (double slash). But there is no general solution to the problem.

The format does not support the tags we talked about above. There are [Cheshire](https://github.com/dakrone/cheshire) and [Data.json](https://github.com/clojure/data.json) libraries to work with JSON in Clojure. Both of them provide two main functions: to read and write a document. You will find detailed examples in GitHub pages of the projects.

JSON compares favorably with the verbose XML it replaces. JSON data looks cleaner and more convenient than a tag tree.  But more modern formats express data even more clearly. In YAML, you can express any structure without a single bracket, thanks to indentation.

JSON syntax is noisy: it requires quotes, colons, and commas where other formats do without them. A comma at the end of an array or object is considered an error. Map keys must not be numbers. It is not allowed to write text on multiple lines.

Compare data in JSON and YAML (on the right). The YAML entry is shorter and visually better perceived:

~~~json
{
    "server_port": 8080,
    "db": {
        "dbtype":   "mysql",
        "dbname":   "book",
        "user":     "ivan",
        "password": "****"
    },
    "event": [
        "2019-07-05T12:00:00",
        "2019-07-12T23:59:59"
    ]
}
~~~

vs

~~~yaml
server_port: 8080
db:
  dbtype:   mysql
  dbname:   book
  user:     user
  password: '****'
event:
  - 2019-07-05T12:00:00
  - 2019-07-12T23:59:59
~~~

### YAML

The YAML language, like JSON, has basic types: scalars, null, and collections. YAML focuses on code conciseness: it sets the nesting using indents rather than brackets. Commas are optional where they might be guessed by parser.  An array of numbers written to a line looks like in JSON:

~~~yaml
numbers: [1, 2, 3]
~~~

But for columns, commas and square brackets disappear:

~~~yaml
numbers:
  - 1
  - 2
  - 3
~~~

DevOps engineers like YAML because it supports Python-style comments (with hashes). Programs like Docker-compose and Kubernetes use YAML for configuration.

YAML allows you to write text across multiple lines. It is easier to read and copy than a single line with a newline character `\n`.

~~~yaml
description: |
  To solve the problem, please do the following:

  - Press Control + Alt + Delete;
  - Turn off your computer;
  - Walk for a while.

  Then try again.
~~~

The language officially supports tags.

The cons of YAML stem from its pros. Indentation seems to be a good solution until the file gets too large. The gaze hops across the file to check if the structure levels are correct. Sometimes part of the data steps to the wrong level due to an unnecessary indent. In terms of YAML, there is no error, so it's hard to find it.

Sometimes, missing quotes will result in incorrect types or structure. Suppose the `phrases` field lists phrases that a user will see:

~~~yaml
phrases:
  - Welcome!
  - See you soon!
  - Warning: wrong email address.
~~~

Because of the colon in the last line, the parser will think it is a nested map (pay attention to syntax highlighting). As a result, we get the wrong structure:

~~~clojure
{:phrases ["Welcome!"
           "See you soon!"
           {:Warning "wrong email address."}]}
~~~

Other examples: product version `3.3` is a number, but `3.3.1` is a string. Phone `+79625241745` is a number because the plus sign is considered a unary operator by analogy with the minus. Leading zeros mean octal notation, so if you don't add quotes to `000042`, you'll get `34`.

This does not mean that YAML is a failed format. The cases above are described in the documentation and have a logical explanation. But sometimes YAML doesn't behave the way you expect -- it’s a price to pay for a simplified syntax.

### EDN

The EDN format occupies a special place in our review. It is as close as possible to Clojure and therefore plays the same role in the language as JSON in JavaScript. It is a Clojure-native way to associate data with a file.

EDN syntax is almost identical to the language grammar. The format covers more types than JSON and YAML. It contains scalars such as symbols and keywords (the `Symbol` and `Keyword` classes from the `clojure.lang` package). In addition to vectors and maps, EDN offers lists and sets. Maps can be typed to allow creating `defrecord` instances upon reading. We will talk more about entries in the chapter on systems.

A tag starts with a hash character. The standard offers two tags by default: `#inst` and `#uuid`. The former reads a string into a date and the latter into a `java.util.UUID` instance. Above, we showed how to add your own tag: you need to bind it to a one-argument function when reading a line.

Here's an example with different types, collections, and tags:

~~~clojure
{:user/banned? false
 :task-state #{:pending :in-progress :done}
 :account-ids [1001 1002 1003]
 :server {:host "127.0.0.1" :port 8080}
 :date-range [#inst "2019-07-01" #inst "2019-07-31"]
 :cassandra-id #uuid "26577362-902e-49e3-83fb-9106be7f60e1"}
~~~

In EDN, data does not differ from code. If you copy them to the REPL or a module, the compiler will execute them. Conversely, the REPL output can be written to a file for further work.

Saving data to EDN means to bake them into a string a write to a file. The function `pr-str` returns a string which would appear in console if you would print an object. The code below creates a file `dataset.edn` with the data:

~~~clojure
(-> {:some ["data"]}
    (pr-str)
    (->> (spit "dataset.edn")))
~~~

The opposite action is to read the file and parse the code in Clojure using `edn/read-string`:

~~~clojure
(require '[clojure.edn :as edn])

(-> "dataset.edn" slurp edn/read-string)
;; {:some ["data"]}
~~~

 (ignoring)}

EDN supports more than just regular comments. The `#_` tag ignores any item following it, including the collection. If you need to "ignore" a map that spans several lines, put `#_` in front of it, and the parser will skip it.

 (comments)}

This way, you can disable entire sections of the configuration. In the following example, we ignore the third element of the vector. If you put a regular comment (semicolon) on a line, it would affect the closing brackets, and the expression will become invalid.

~~~clojure
{:users [{:id 1 :name "Ivan"}
         {:id 2 :name "Juan"}
         #_{:id 3 :name "Huan"}]}
~~~

EDN is closely related to Clojure and, therefore, is not popular in other languages. Editors don't highlight its syntax without plugins. EDN will provide challenges for DevOps engineers who mostly work with JSON and YAML. If your configuration is precessed with Python or Ruby scripts, you will have to install a library to work with EDN format.

Choose EDN where Clojure prevails over other technologies. It is the right choice when both the backend and the frontend run on the same Clojure(Script) stack.

## Industrial Solutions

Configuration is significant to understand, but we don't encorouge you to write it from scratch every time you run a new project. In the final section, we'll take a look at what does the community provides for configuration handling. We'll focus on Cprop, Aero, and Yummy. These libraries differ in ideology and architecture. We have specially selected them to see the problem from different angles.

### Cprop

The [Cprop](https://github.com/tolitius/cprop) library works on the principle of "data from everywhere". Unlike our loader, Cprop understands more sources. The library can read not only a file and environment variables but also resources, property files, and ordinary maps.

The library has a preset order of walking through sources and their priority. Fields from one source replace others. For example, environment variables are considered more important than a file. In Cprop, you can easily set your own loading order for special cases.

We are interested in the `load-config` function. If you call it without any parameters, it will start the standard loader. By default, it looks for two data sources: a resource and a property file. This resource must be named `config.edn`. If the system property `conf` is not empty, the library assumes that this is the property file path and loads it.

Properties are Java runtime variables, similar to the system environment. When loaded, JVM receives the default properties: operating system type, line separator, and others. Additional properties are set with the `-D` parameter when starting. The example below runs a jar file with a `conf` property:

~~~bash
$ java -Dconf="/path/to/config.properties" -jar project.jar
~~~

The `.properties` files are `field=value` pairs, one per line. Fields are like domains: they are lexemes separated by dots. Lexemes follow in descending order of priority:

~~~ini
db.type=mysql
db.host=127.0.0.1
db.pool.connections=8
~~~

The library treats dots as nested maps. The file above will return the following structure:

~~~clojure
{:db {:type "mysql"
      :host "127.0.0.1"
      :pool {:connections 8}}}
~~~

After receiving the configuration, Cprop looks for overriding in the environment variables. For example, the variable `DB__POOL__CONNECTIONS=16` will replace the value 8 in the nested map. Cprop ignores variables that are not part of the config and thus keeps it tidy.

Non-standard paths to the resource and file are specified with the keys:

~~~clojure
(load-config
 :resource "private/config.edn"
 :file "/path/custom/config.edn")
~~~

For delicate work, Cprop offers the `cprop.source` module. Its `from-env` function reads all environment variables, and `from-props-file` loads the property file, and so on. It is easy to build the combination that the project needs using the module.

The `:merge` key unites the config with any source. The former holds a sequence of expressions that will return a map. Here is a detailed example from documentation:

~~~clojure
(load-config
 :resource "path/within/classpath/to.edn"
 :file "/path/to/some.edn"
 :merge [{:datomic {:url "datomic:mem://test"}}
         (from-file "/path/to/another.edn")
         (from-resource "path/within/classpath/to-another.edn")
         (from-props-file "/path/to/some.properties")
         (from-system-props)
         (from-env)])
~~~

To track loading, set the `DEBUG=y` environment variable. With it, Cprop displays service information: a list of sources, loading order, overrides, and so on.

Cprop only reads data from sources but doesn't validate it. There is no validation with a spec in the library, as it is done in our loader. The step is up to you.

The library casts types its own way. If the string contains only digits, it is converted to a number. Comma-separated values become lists. Sometimes these rules are not enough for complete type control. Thus, Spec and `s/conform` are still useful for error reporting and type inference.

### Aero

[Aero](https://github.com/juxt/aero) works with EDN files. The library offers tags, making the format look like a mini-programming language. Branching, import, formatting operators appear in it. This approach can be figuratively called "EDN on steroids".

The `read-config` function reads an EDN file or resource:

~~~clojure
(require '[aero.core :refer (read-config)])

(read-config "config.edn")
(read-config (clojure.java.io/resource "config.edn"))
~~~

Tags are the main point in Aero, so let's take a look at the main ones. The familiar `#env` one discovers the value of a variable by its name:

~~~clojure
{:db {:passwod #env DB_PASS}}
~~~

The `#envf` tag formats a string using environment variables. Let's say the connection to the database consists of separate fields, but you prefer the JDBC URI, a long string that looks like a web address. In order not to duplicate data, the address is composed from the original fields:

~~~clojure
{:db-uri #envf ["jdbc:postgresql://%s/%s?user=%s"
                DB_HOST DB_NAME DB_USER]}
~~~

The `#or` tag is similar to its Clojure counterpart and is needed for default values. Suppose no database port is specified in the file. In this case, let's specify the standard PostgreSQL port:

~~~clojure
{:db {:port #or [#env DB_PORT 5432]}}
~~~

Pay attention, the value for the tag is always a vector or a list. Also, the example above introduces nested tags (`#env` inside `#or`).

The `#profile` tag allows you to find the value by profile. The value behind the tag must be a map. The map key is the profile, and the value is what we get as a result of its discovery. The profile is set in parameters of `read-config`.

The example below shows how to find the database name by profile. Without a profile, we get the "book" name, but for `:test`, it becomes "book_test".

~~~clojure
{:db {:name #profile {:default "book"
                      :dev     "book_dev"
                      :test    "book_test"}}}

(read-config "aero.test.edn" {:profile :test})
{:db {:name "book_test"}}
~~~

The `#include` tag puts another EDN file in the config. The file can also contain tags, and the library will execute them recursively. We use imports when the configuration becomes too large or there is a need to share its parts across multiple projects.

~~~clojure
{:queue #include "message-queue.edn"}
~~~

The `#ref` tag refers to any part of the configuration file. It is a vector of keys that is usually passed to `get-in`. A reference will allow you to avoid duplication. For example, a background task component needs the user we specified for the database. In order not to copy it, let's put the link:

~~~clojure
;; config.edn
{:db {:user #env DB_USER}
 :worker {:user #ref [:db :user]}}
~~~

When reading a file, the link resolves to the value:

~~~clojure
{:db {:user "ivan"}, :worker {:user "ivan"}}
~~~

Aero offers a simple configuration language. The library entices developers with the beauty of its idea and implementation. But the moment you feel like moving from inflexible JSON to Aero, think about the other side of the coin.

We do not accidentally separate config from code. If it weren't for the industry's need, we would store the parameters in the source files. But best practices, on the contrary, advise separating parameters from the code. This is also because, unlike code, the configuration is declarative.

Inflexible JSON files have an important feature: they are declarative. If you open a file or run `cat` on it, you will see the data. The syntax may be awkward, but data is self-explanatory, and there is only one way to read it.

On the contrary, a file with an abundance of tags is hard to read. It is not a config but code. To see the final data, you have to execute the file. When reading a file, your head runs a mini interpreter, which does not guarantee the correct result.

It turns out to be a kind of vicious circle: we moved the parameters into the config, added tags, and returned to the code. The approach has the right to exist, but you should choose it after weighing the pros and cons.

### Yummy

The [Yummy](https://github.com/exoscale/yummy) library closes the overview. It differs from the libraries discussed above in two ways. First, it works with YAML files to read a config (hence the name). Second, the loading process is similar to the one we covered at the beginning of the chapter.

A fully featured loader does more than just read parameters. The cycle includes data validation and error output. The message clearly explains the cause of the error. Using options, you can set a reaction to an exception that occurred while working. Yummy offers all of the above.

The file path either might be set with parameters, or the library searches for it according to special rules. Here's an option when the path is explicitly set:

~~~clojure
(require '[yummy.config :refer [load-config]])

(load-config {:path "/path/to/config.yaml"})
~~~

In the second case, we specified the name of the project instead of the path. Yummy looks for the file path in the `<project>_CONFIGURATION` environment variable or the `<project>.configuration` property:

~~~bash
$ export BOOK_CONFIGURATION=config.yaml
~~~

~~~clojure
(load-config {:program-name :book})
~~~

The library extends YAML with several tags. One is the familiar `!envvar` for environment variables:

~~~yaml
db:
  password: !envvar DB_PASS
~~~

The `keyword!` tag is useful for converting a string to the keyword:

~~~yaml
states:
  - !keyword task/pending
  - !keyword task/in-progress
  - !keyword task/done
~~~

Here is the result:

~~~clojure
{:states [:task/pending :task/in-progress :task/done]}
~~~

The `!uuid` tag is similar to the `#uuid` one for EDN; it returns the `java.util.UUID` object from a string:

~~~yaml
system-user: !uuid cb7aa305-997c-4d53-a61a-38e0d8628dbb
~~~

The `!slurp` tag reads the file, which is useful for encryption certificates. Their content is a long string that is inconvenient to store in a general configuration. The `:auth`, `:cert`, and `:pkey` keys will hold the contents of the files from the `certs` directory.

~~~yaml
tls:
  auth: !slurp "certs/ca.pem"
  cert: !slurp "certs/cert.pem"
  pkey: !slurp "certs/key.pk8"
~~~

To check the configuration, pass the spec key to the `load-config` parameters. When a key is specified, Yummy executes `s/assert` with the data from the file. If the validation returns false, an exception will float up. For better reading of spec validation reports, Yummy uses Expound.

~~~clojure
(load-config {:program-name :book
              :spec ::config})
~~~

An options map takes the `:die-fn` parameter. It is a function that will run if any stage fails. The function takes an exception and a label with a stage name.

If `:die-fn` is not specified, Yummy will call the default handler. It prints the text to `stderr` and exits the JVM with code 1. During the development phase, we do not want to terminate the REPL due to a config error. In an interactive session, our `die-fn` only prints the text and the error:

~~~clojure
(load-config
 {:program-name :book
  :spec ::config
  :die-fn (fn [e msg]
            (binding [*out* *err*]
              (println msg (ex-message e))))})
~~~

In production mode, write the exception to the log and exit the program.

~~~clojure
(load-config
 {:program-name :book
  :spec ::config
  :die-fn (fn [e msg]
            (log/error e "Config error" msg)
            (System/exit 1))})
~~~

One note about the `s/assert` macro which Yummy uses for validation. This macro does not coerce values, as `s/conform` does, but only throws an exception. This is done on purpose: types are coerced by tags, and the spec only validates them.

## Summary

Let us briefly outline the main points of this chapter. The configuration is necessary for the project to go through the production stages: development, testing, release. At each step, the project is launched with different settings. This is not possible without configuration.

Loading configuration means reading data, infer types and validate values. In case of an error, a program displays a message and exits with an emergency code. It cannot continue working with invalid parameters.

Configuration sources can be a file, a resource, or environment variables. There are hybrid schemes when most of the data come from the file and secret fields from the environment.

Environment variables live in operating system memory. When there are many of these variables, we can place them in the ENV file. An application does not read it; this is done by a script that controls the app on the server. The application does not know where the variables come from.

The environment is a flat map. Variables store only text; there is no nesting or namespace in keys. Different systems have different conventions on how to extract a structure from a variable name. Dots, double underscores, or something else can be used.

Data formats differ in syntax and types. General-purpose formats define strings, numbers, maps, and lists. They are not very flexible, but they work everywhere. On the contrary, the platform-specific data format is closely tied to the platform but is unpopular in other languages.

Some formats support tags. Use them to describe complex types with primitives: strings and numbers. Tags are also helpful for pre-processing a document, for example, to import its nested parts. The danger of tags is: when there are too many, config turns into code.

Clojure offers several libraries for configuring applications. They differ in design and architecture, and each developer will find what they like. There is no definite answer to the question of which format or library is better. Choose what will solve your problem most cheaply.
