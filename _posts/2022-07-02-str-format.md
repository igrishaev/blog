---
layout: post
title:  "Use format rather than str"
permalink: /en/clj-format
tags: clojure strings format
lang: en
---

I'd like to share a Clojure trick I've been using for years. It's simple, it makes your code safer; I always highlight it when making reviews. The trick is: don't use the `str` function to concatenate strings. Instead, use `format`. A couple of examples:

~~~clojure
(let [username "Ivan"]
  (str "The user `" username "` has been created."))
;; "The user `Ivan` has been created."


(let [user-id 5234]
  (str "/files/uploads/" user-id))
;; "/files/uploads/5234"
~~~

**The first point** in favour of this approach is, `str` turns nil into an empty string. Thus, when printing the final message, that's unclear if a variable was an empty string or `nil`:

~~~clojure
(let [username nil]
  (str "The user `" username "` has been created."))
;; "The user `` has been created."
~~~

The difference between these two is important. Say, an empty string means broken validation; a title of a book, a name of a person must not be blank. But if I got nil, most likey I missed the key in a map because of a namespace:

~~~clojure
(def user
  {:acme.user/id 5234
   :acme.user/name "Ivan"})

(let [username (get user :id)]
  ...)
~~~

or the keyword/string case:

~~~clojure
(def user
  (parse-json "user.json"))
;; {"id" 5234 "name" "Ivan"}

(let [username (get user :name)]
  ...)
~~~

Now compare it to the `format` function. The `nil` value becomes `"null"` when passed to `format`:

~~~clojure
(let [username nil]
  (format "The user `%s` has been created." username))
;; "The user `null` has been created."

(let [user-id nil]
  (format "/files/uploads/%s" user-id))
;; "/files/uploads/null"
~~~

If I had my way, I would produce not `"null"` but `"nil"` string from `nil`, but that's not so important.

**The second point** is much more serious. `Nil` values are extremely dangerous when building file paths or URLs. Imagine you're about to delete files of a person who's terminating their account. Most likely you store files on disk like this:

~~~
files/<user-id>/avatars/...
files/<user-id>/attachments/...
files/<user-id>/uploads/...
~~~

Then you have a function that accepts the `user-id` parameter, then builds the right path and does recursive deletion:

~~~clojure
(defn drop-user-files [user-id]
  (let [path
        (str "files/" user-id)]
    (rm-rf-recur path)))
~~~

If you pass `nil` for `user-id`, the path will be `"files/"`. Running that code will erase all the files of all users which would be a disaster. But if you have used format, the path would have been `"files/null"`, which would just have thrown an exception saying there is no such a directory.

One may say: add an assert clause for user-id right before you build a path. Like this:

~~~clojure
(defn drop-user-files [user-id]
  (assert user-id "User-id is empty!")
  ...)

;; or

(defn drop-user-files [user-id]
  {:pre [user-id]}
  ...)
~~~

In practice, you easily forget doing this and recall when the data is lost. I don't see any reason for skipping that minor fix — change `str` to `format` — to reduce chances of a disaster.

The same applies to S3 URLs. Although it's a web-service, we all treat it as a file system. Composing S3 URLs reminds me of ordinary file paths. Again, if you're about to drop user's directory with uploads, be aware of the the same thing: `str` + `nil` for `user-id` produce a broken path:

~~~clojure
(defn drop-s3-user-files [s3-client user-id]
  (let [path
        (str "files/" user-id)]
    (s3.client/delete-files s3-client path)))
~~~

If you pass `nil` into a function that recursively drops S3 files, the data is all gone.

Of course, such an issue can be held with special Java classes like `Path` or `URI`. But in fact, in Clojure we use them quite rarely. Most often we just concatenate plain strings as it's enough for the task. It's simpler and takes less code.

I recommend using `str` for one purpose only — to coerce a non-string value to a string. For example:

~~~clojure
(str 5) ;; => "5"
(str (random-uuid)) ;; => "154ac...b2642"
~~~

Briefly, it's safe when the `str` function accepts strictly one argument. When there are more than one, I feel worried.

I always keep in mind a real story about a guy who owned a small hosting company. He released the `rm -rf $FOO/$BAR` command among the whole park. Too sad for him, both of the env vars were unset, nor special bash flags terminating a script were set as well. The command turned into `rm -rf /` with known consequences. A couple of missing vars has ruined one's business. By `str`-ing strings, especially when building file paths, you may easily mess up the same way (but I wish you won't).

Let's recap:

- `str` turns `nil` into an empty string;
- by reading such a message, you never know if a value was an empty string or nil;
- the difference between these two **does** matter;
- with that behaviour, it's easy to build a weird file/S3 path and lost the data;
- instead, `format` turns `nil` into `"null"`. This is much better than emptiness and solves the troubles mentioned above;
- use `str` only to coerce a value to a string.

Safe coding!
