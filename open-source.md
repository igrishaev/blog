---
layout: page
title:  "Open Source"
permalink: /open-source/
---

This page collects my open source libraries and projects. Some of them I
maintain, some of them I put here for history purpose only.

[clj-commons]: https://github.com/clj-commons
[webdriver]: https://www.w3.org/TR/webdriver/
[rome]: https://github.com/rometools/rome

-- [Etaoin](https://github.com/clj-commons/etaoin) is a library to automate a
browser from code. It implements the [Webdriver][webdriver] protocol in pure
Clojure. I have been a maintainer of this lib for several years. At the moment,
it is maintained by the [Clj-commons][clj-commons] community.

-- [Remus](https://github.com/igrishaev/remus) is an RSS/Atom feed parser based
on top of the [Rome Tools][rometools] Java library. It supports HTTP2 and some
other features.

-- [PG2](https://github.com/igrishaev/pg2) is a client for PostgreSQL
database. It doesn't rely on JDBC abstractions and is written from scratch. The
performance is a faster than the Next.jdbc wrapper for Clojure. It supports some
of PostgreSQL features that other drivers don't.

-- [Ring JDK Adapter](https://github.com/igrishaev/ring-jdk-adapter) is a
Ring-compatible web server build on top of Java's `jdk.httpserver` module. It
has no dependencies and thus is great for testing or stubbing local
HTTP. Although it's a bit slower than Jetty, it still can be used in production.

-- [Deed](https://github.com/igrishaev/deed) is library to serialize arbitrary
Clojure data into binary format, and decode back. Deed is faster than Nippy and
doesn't suffer from some issues that Nippy has. Free from dependencies and
provides flexible API like lazy reading or writing.

-- [JSam](https://github.com/igrishaev/jsam) is a JSON encoder and decoder
written from scratch in Java and a thin Clojure layer. Has no dependencies and
thus doesn't suffer from missing Jackson modules or conflicts (which is common
when using Cheshire or Jsonista). Not the fastest one but is still pretty good.

-- [Lambda](https://github.com/igrishaev/lambda) is a number of tools to run
Clojure in AWS Lambda service. It provides a middleware that turns AWS messages
into Ring maps and back, so your lambda can serve a Ring web application. The
documentation describes how to compile and deploy your lambda to AWS.

-- [Whew](https://github.com/igrishaev/whew) is library wrapping the Java's
`CompletableFuture` class and its capabilities. It provides a number of
functions and macros to chain futures and organize pipeline based on async
computations. Free from dependencies.

-- [Virtuoso](https://github.com/igrishaev/virtuoso) is a collection of wrappers
on top of Java's 21 virtual threads. The library ships functions and macros
named after their Clojure counterparts but acting by means of virtual
threads. Useful for IO-heavy logic.

-- [Teleward](https://github.com/igrishaev/teleward) is a Telegram captcha bot
working in `@clojure_ru` Telegram community. It implements some of Telegram Bot
API from scratch including a small template system. It stores state in the
DynamoDB cloud service.

-- [Dynamo DB](https://github.com/igrishaev/dynamodb) is a client to the Dynamo
DB AWS service. Fast and robust with narrow dependencies. Supports almost every
API. The library is also compatible with Yandex YDB (see on Yandex cloud).

-- [Bogus](https://github.com/igrishaev/bogus) is a simple UI debugger for
Clojure. It interrupts execution and renders a window with local variables and a
simple REPL. By using this REPL, you evaluate expressions relevant to the
current position and state. I use it every day since I made it.

-- [Taggie](https://github.com/igrishaev/taggie) is a joke library that
overrides printing rules for most of Clojure objects. Say, the `java.io.File` is
now rendered as `#File "/path/to/file.txt"` but not
`#<java.io.File@7fea5978>`. This approach is extremely arguable yet still useful
for local setup.

-- [Zippo](https://github.com/igrishaev/zippo) is a set of additions to the
standard `clojure.zip` module for Zippers. Namely, the library provides
functions for better search and iteration, e.g. breadth-first algorithm,
specific zippers and so on.

-- [User Agent](https://github.com/igrishaev/user-agent) is a simple library to
parse HTTP User-Agent header. It is based on the UADetector Java library. The
library has got 650.000 of downloads on Clojars (don't know why).

-- [Farseer](https://github.com/igrishaev/farseer) is my attempt to build a JSON
RPS client and server. It's split on sub-projects with core functionality, a
server, a client and utilities. It follows the official spec and serves multiple
commands at once, parallel processing, input and output validation and so on.

-- [Pact](https://github.com/igrishaev/pact) is a simple library to chain values
through a number of handlers. This approach reminds monads in Haskell and how
this language manages them: "good" values propagate further, "bad" ones
interrupt the pipeline.

-- [Mockery](https://github.com/igrishaev/mockery) is a tool for mocking
functions in tests. Nothing but a wrapper on top of the `with-redefs` macro but
saves lines of code.

-- [Mask](https://github.com/igrishaev/mask) is a simple library introducing a
`Masked` type. This type wraps secret values preventing them from leaking though
printing or logging. Should be used in configuration when all passwords and
tokens are masked.

-- [Spec Dict](https://github.com/igrishaev/spec-dict) is my attempt to make a
better Clojure spec for maps. This spec is declared like map of key &rarr;
another spec (like we do it in Malli nowadays).

-- [Toy Parser](https://github.com/igrishaev/toy-parser) is demo repository for
students learning parsers. It provides a number of basic parsers and two
showcases processing prefix and infix notations.
