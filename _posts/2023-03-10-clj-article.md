---
layout: post
title:  "Эта удивительная Clojure: что на ней разрабатывают, чем она отличается от других языков и подходит ли для входа в программирование"
permalink: /clojure-article/
tags: programming clojure
---

[pavel]: https://dside.ru/
[ivan]: https://grishaev.me/
[darovska]: https://darovska.com/

Эта статья была написана для одного издания, но по ряду причин ее не опубликовали. Размещаю здесь, чтобы материал не пропал. В подготовке статьи участвовали:

- [Павел Пеганов][pavel] и [Иван Гришаев][ivan], программисты;
- [Маша Даровская][darovska], редактор.

***

[clojure_ru]: https://t.me/clojure_ru

*Мы расспросили разработчиков на Clojure из сообщества [clojure_ru][clojure_ru]. Выясняли, как применяют язык, что на нём пишут, легко ли на нём программировать.*

## Что программируют на Clojure

[metabase]: https://www.metabase.com/
[penpot]: https://penpot.app/

**Павел:** сфера применения Clojure в техническом плане — в основном веб и серверные приложения. На успешно работающий Clojure-код можно посмотреть, например, в продуктах [Metabase][metabase] и [Penpot][penpot], их исходный код открыт.

Но постепенно язык проникает и в другие области. ClojureScript работает в браузерах и других средах для JavaScript, с помощью проекта Esprit его уже запускают на микроконтроллерах, а сейчас развивают ClojureDart, чтобы захватывать мир Flutter. Конечно, не все эксперименты в итоге «взлетят», но такое разнообразие работающих проектов показывает, что применимость языка ограничена скорее настроениями разработчиков, чем самим языком.

Если говорить о предметных областях, то в вакансиях и проектах с Clojure, о которых слышу я, эмпирически кажется, что финтеха больше, чем прочих. Даже компания, поддерживающая Clojure, Cognitect, принадлежит банку Nubank. Но кроме финтеха областей тоже хватает.

**Иван:** сфера применения Clojure широка, она решает те же задачи, что Java, Python и другие языки. На ней пишут сетевые сервисы, бэкенд веб- и мобильных приложений. Clojure подходит для обработки данных из разных источников — баз данных, очередей, HTTP API — и часто служит их оркестратором.

[clojurescript]: https://clojurescript.org/

Существует [ClojureScript][clojurescript] — компилятор кода на Clojure в JavaScript. С его помощью создают браузерный фронтенд и мобильные приложения на базе React Native.

Код на Clojure можно скомпилировать при помощи GraalVM и native image, получив бинарный файл. С этим подходом пишут утилиты командной строки, интерпретаторы, AWS Lambda и многое другое.

<!-- more -->

## Чем Clojure отличается от других языков семейства Lisp и почему эти языки несовместимы

**Павел:** с другими диалектами Lisp я за всю карьеру (с 2014 года) ни разу на практике не столкнулся. Видел пару образцов кода на Scheme, и те в книге Structure and Interpretation of Computer Programs, а не на работе. Слышал также о Racket, но только в академическом контексте.

Вообще от разных языков странно ожидать совместимости на основе только некоторого сходства в синтаксисе. Иногда такое действительно можно наблюдать, но разве что когда сходство — это следствие родства, когда один язык прямо вырастает из другого, и несовместимости достаточно быстро возникают и в таких случаях. К примеру, некоторые программы на C можно собрать и как C++, но только совсем простые. Аналогичная ситуация между Ruby и Crystal.

[xkcd]: https://xkcd.ru/297/

Clojure же относительно самобытен. [Обилие скобок бросается в глаза][xkcd], конечно, но это лишь одна из составляющих языка. И в отличие от многих собратьев по семейству, он активно пользуется *разными видами* скобок, не только круглыми.

**Иван:** Lisp — это семейство языков, объединённых схожим синтаксисом, а Clojure — наиболее современный их представитель. Для обывателя все Лиспы одинаковы, но на самом деле разница велика: каждый диалект силён в своих областях.

От классических диалектов вроде Common Lisp или Scheme Clojure отличается более современными идеями. Язык поощряет неизменяемость данных, асинхронные и ленивые вычисления.

Clojure предлагает мощные коллекции и функции для работы с ними. Они доступны по умолчанию без сторонних библиотек. По сравнению с Common Lisp улучшены макросы. Разработка на Clojure ведётся через REPL, как это принято в классических диалектах.

Синтаксис Clojure отличается от других Lisp-языков. В нём меньше круглых скобок, а некоторые формы задают квадратными и фигурными. Шутки про множество скобок уже не так актуальны для Clojure, как это было со старыми диалектами.

Главное отличие Clojure от других Лиспов в том, что Clojure работает на платформе JVM. При компиляции получается jar-файл, который позже запускается силами Java. Это отпугивает разработчиков на Common Lisp, которые привыкли к машинному коду. JVM позволяет повторно использовать код на Java, который писали и отлаживали десятилетиями. Этому Clojure обязана своей популярностью: сразу после выхода на ней можно было писать промышленный код, не дожидаясь библиотек.

## Можно ли начать программировать с Clojure

[clojure]: https://clojure.org/

**Павел:** можно ли освоить Clojure как первый язык? Определённо. Язык довольно прост по своей структуре, встроенных механик в нём немного, удивительно многие механизмы можно реализовать посредством функций и макросов у себя в коде, не вмешиваясь в устройство самого языка. А REPL можно поначалу воспринимать как своего рода «чат» с языком или как калькулятор, способный на обработку не только чисел. Не нужно разбираться со структурой проекта, процессом сборки, способом запуска, обязательной точкой входа (необходимостью всё заворачивать в классы и методы, например) — всё это для самого первого языка лишнее.

[cookbook]: http://clojure-cookbook.com/
[cookbook-gh]: https://github.com/clojure-cookbook/clojure-cookbook

Логично начинать знакомство с сайта языка: [clojure.org][clojure]. Там прекрасные руководства, пусть кому-то они и могут показаться суховатыми. Есть и книги, причём некоторые даже в свободном доступе. Мне здорово помогла [Clojure Cookbook][cookbook], объясняющая язык на примерах — от примитивных и абстрактных до прикладных. Её исходники доступны [на GitHub][cookbook-gh].

Мой совет: не браться изучать редактор Emacs одновременно с Clojure, что часто рекомендуют другие. Так *можно* делать, конечно, просто Emacs — это целый мир, и не каждому хватит упорства осваивать сразу две настолько широких темы параллельно. Для начинающего я бы посоветовал в качестве редактора VSCode с расширением Calva, его интерфейс гораздо больше похож на то, с чем можно столкнуться в повседневной околокомпьютерной деятельности.

[learn-xy]: https://learnxinyminutes.com/docs/clojure/
[learn-xy-ru]: https://learnxinyminutes.com/docs/ru-ru/clojure-ru/

Для тех же, кто погружается после других языков, для ознакомления может быть полезна краткая шпаргалка по Clojure с сайта [Learn X in Y Minutes][learn-xy] (есть и на [русском языке][learn-xy-ru]).

[scheme]: https://ru.wikipedia.org/wiki/Scheme
[racket]: https://racket-lang.org/
[sicp]: https://mitpress.mit.edu/1984-structure-and-interpretation-of-computer-programs/

**Иван:** я бы не рекомендовал Clojure в качестве первого языка. Она опирается на JVM, и это усложнит путь начинающему разработчику. [Scheme][scheme] или [Racket][racket] станут отличным выбором — неслучайно курс SICP и [одноимённая книга][sicp] используют Scheme.

Начните с официального сайта Clojure: его разделы описывают философию языка, устройство и первые шаги.

[clojure-dmk]: https://www.labirint.ru/books/483189/
[clojure-in-prod]: https://grishaev.me/clojure-in-prod/
[the-joy-of-clojure]: https://www.manning.com/books/the-joy-of-clojure-second-edition
[braveclojure]: https://www.braveclojure.com/

На русском языке доступны книги [«Программирование на Clojure»][clojure-dmk] и [«Clojure на производстве»][clojure-in-prod], причём последняя — не перевод. Из английских подойдут [The Joy of Clojure][the-joy-of-clojure] и [Clojure for Brave and True][braveclojure]. Вступите в группу [clojure_ru][clojure_ru] в «Телеграме», чтобы обращаться за советами.

## Полезные библиотеки

**Павел:** Clojure подключается к уже существующим экосистемам. Классическая реализация для JVM позволяет использовать почти всё, что можно использовать из Java. Такое же отношение между ClojureScript и JavaScript, ClojureDart и Dart и т.д. Так что в переписывании существующих библиотек из других экосистем Clojure не сильно нуждается, разве что в обёртках для библиотек с платформы, а их уже написано немало.

Но есть интересные образцы и в самой Clojure. Мне, например, было интересно попробовать re-frame (для браузерных приложений), Instaparse (даёшь строку с грамматикой — получаешь функцию-парсер) и DataScript (иммутабельная БД с языком запросов).

**Иван:** в Clojure есть все необходимые библиотеки для баз данных, кэширования, очередей сообщений. Поддерживаются известные форматы и протоколы, средства веб-разработки. Проблемы отсутствующих библиотек нет как таковой.

Можно сказать, Clojure умеет всё, что умеет Java из-за повторного использования кода. При этом абстракции, построенные на Clojure, оказываются изящными и ёмкими.

[clojars]: https://clojars.org/

У Clojure свой репозиторий библиотек, который называется [Clojars][clojars]. Поддерживается Maven — центральный репозиторий Java. Зависимости могут находиться в репозитории Git, что позволяет указывать версии с точностью до тега или коммита. Этим пользуются в больших фирмах, где много внутренних библиотек.

## Популярность языка

**Иван:** Clojure менее популярна, чем Java или Python, но ее доля растёт. На западе Clojure используется всё чаще, и эта тенденция плавно доходит до нас. Пока что в России мало компаний, которые ищут разработчиков на Clojure: это Health Samurai, Сбербанк, Ростелеком. На мировом рынке вакансий больше на порядок, но из-за последних событий тяжело устроиться, если находишься в России.

Типичная вакансия на Clojure — это сетевой сервис или бэкенд веб- или мобильного приложения. Код сводится к обработке данных из разных источников — БД, Redis, Kafka. Реже встречается фронтенд с различными обертками над React.

Зарплата удалённого разработчика в западных фирмах варьируется от 5 до 12 тысяч долларов в месяц. Среди российских вакансий самая высокая ЗП обозначена «до 330 тысяч рублей». Я не собеседовался туда и не могу подтвердить, правда это или для привлечения внимания.

[clojurians]: https://clojurians.slack.com
[clojurejobboard]: http://clojurejobboard.com
[remoteok]: https://remoteok.com/

Работу в России ищут в телеграм-канале clojure_ru в топике вакансий. Удалённые вакансии размещают в слаке [Clojurians][clojurians] в канале `#remote-jobs`, а также на сайтах [clojurejobboard.com][clojurejobboard] и [remoteok.com][remoteok] по тегу clojure.

## Как в сообществе относятся к новичкам

**Павел:** Clojure редко изучают как первый язык. Наверное, поэтому в части сообщества, что я видел, много сформировавшихся и опытных специалистов с холодными головами, привыкших к странным вопросам от людей, пришедших из других экосистем. Те дискуссии, что я видел, были конструктивными, познавательными и в целом приятными. Инциденты случаются, но редко, чаще даже с особо абразивными участниками получается наладить диалог, хоть выглядит этот диалог порой забавно. В целом, как и почти везде — на уважительное отношение можно ожидать взаимности.

**Иван:** пожалуй, у Clojure самое доброе и отзывчивое сообщество, что мне приходилось видеть. Возможно, потому, что к Clojure приходят в более старшем возрасте, нежели к PHP, Javascript или Python. Люди общаются в позитивном ключе — задавайте вопросы, и вам помогут.

## Тулинг языка и поддержка в IDE

**Павел:** это язык, за который без инструментов лучше не браться. Тот, кто впервые видит Clojure, чаще всего пугается обилия скобочек. Но, наверное, никто из тех, кто плотно занимается Clojure, вручную их не расставляет. Для этого есть как минимум Paredit и Parinfer. С ними можно фокусироваться на структуре кода, а не на тексте, из которого он состоит. Из поддержки именно в IDE я слышал только о Cursive для сред Intellij от JetBrains, чаще же используют просто текстовые редакторы вроде Emacs, Vim и VSCode. Есть и Language Server, интегрировать который в новые редакторы для облегчения работы с Clojure не должно быть большой проблемой.

Со сборкой дела обстоят чуть сложнее, но тоже хорошо. Нет какого-то одного инструмента, который используется везде, но и огромного ассортимента, из которого не понятно что выбирать, тоже нет. Сейчас на сайте clojure.org советуют Clojure CLI, который вполне неплох. А если касаться других реализаций Clojure вроде ClojureScript, то, скорее всего, там будет что-то своё, это придётся осваивать отдельно — единства инструментов в разных реализациях нет.

[leiningen]: https://leiningen.org/

**Иван:** Clojure предлагает все средства для комфортной работы. Прежде всего это утилита [Leiningen][leiningen] для управления проектами. При помощи неё код запускают с разными профилями, собирают uberjar, прогоняют тесты, загружают библиотеки в репозиторий Clojars. Lein расширяется плагинами, которых великое множество под разные задачи.

Кроме Leiningen, доступны утилиты Clojure CLI и Boot со схожими возможностями. Clojure CLI более гибко работает с classpath, что порой необходимо в сложных проектах.

Для Clojure создан и поддерживается LSP-сервер и плагины к популярным редакторам. LSP используют многие разработчики, его популярность растёт. Однако лучшей IDE для Clojure считается Emacs с модулем Cider.

Важно понимать, что разработка на Clojure протекает не так, как в других языках. В них программист полагается на статический анализатор кода. В Clojure всё решает REPL-driven-development, когда код запускают в REPL по мере написания. В этом плане связка Emacs+Cider предлагают наиболее продвинутые средства.

Поскольку Emacs — это вещь в себе, будет трудно изучать его вместе с Clojure. В идеале с ним знакомятся отдельно. Я перешёл на Emacs ещё до Clojure. Возможно, на первых порах подойдут IntelliJ IDEA или VS Code — они тоже поддерживают Clojure за счёт сторонних модулей.

[babashka]: https://babashka.org/

Набирает популярность проект [babashka][babashka] — интерпретатор Clojure, написанный на Clojure и скомпилированный GraalVM. С его помощью запускают скрипты, написанные на Clojure, без компиляции. Babashka полезен в облачном сервисе AWS Lambda и в системном программировании.

## Проблема статической типизации в Clojure

[typed]: https://typedclojure.org/

**Павел:** статическая типизация — это, пожалуй, тема самых бурных и длинных дебатов в чатах по Clojure.

Основная проблема со статической типизацией — негибкость. Либо статически типизирован весь код, либо код просто не собирается и не запускается, либо систему типов на определённых участках приходится отключать, лишаясь её преимуществ. Необходимость постоянно с этим бороться здорово замедляет разработку: очень непросто вписать предметную область, особенно недоосвоенную, в формальные ограничения системы типов.

Обычный контраргумент на это — что именно из-за таких взглядов нестабильное программное обеспечение распространилось настолько, что к этому даже пользователи уже привыкли. Я с ним не вполне согласен — на мой взгляд, для испытания какой-то новой идеи даже не вполне стабильное ПО полезнее, чем никакое. Проблемы начинаются, когда на такое нестабильное ПО начинают существенно опираться. Но это вина уже явно не типизации.

Для желающих есть библиотека [Typed Clojure][typed], ранее core.typed. Но я не припомню, чтобы она была сколько-нибудь популярна. Популярны *схемы* (schemas). Когда-то на слуху была библиотека Schema, позднее появились Clojure.spec и Malli. Они позволяют зафиксировать структуру значений в ключевых местах, где ожидаются проблемы. Но где это делать и как — дело ваше.

[closure-compiler]: https://developers.google.com/closure/compiler
[externs]: https://clojurescript.org/guides/externs

Хотя на «краях» языка, где идёт взаимодействие с хост-платформой, указывать типы местами всё-таки приходится. На классическом Clojure для JVM при вызове класса на Java может потребоваться type hint, чтобы ускорить код или вызвать правильный метод из разных перегрузок. А в ClojureScript они могут использоваться в [экстернах][externs] для сжатия скомпилированного кода на JavaScript с помощью [Google Closure Compiler][closure-compiler]. Но таких случаев немного, и их можно запереть в библиотеках-обёртках, чтобы в повседневной жизни с ними не сталкиваться.

**Иван:** Clojure — не чисто функциональный язык как Haskell. Разработчики говорят о нём: tends to be functional — тяготеет к функциональному стилю, но не полностью. Надёжность кода на Clojure обусловлена тем, что код на нём запускают в REPL по мере написания. Из-за этого сразу видно, работает код или нет. В других языках код проверяет статический анализатор. Он покажет типовые ошибки, но их отсутствие не гарантирует безошибочную работу. Clojure, напротив, отталкивается от запуска кода, что даёт решающее преимущество.

В Clojure были попытки создать статическую типизацию. Проект называется [Clojure Typed][typed]; с его помощью в код добавляют аннотации с типами, а затем он проверяется на корректность. Судьба проекта неоднозначна: некоторые компании им пользовались, но с развитием кодовой базы типизация скорее мешает, чем приносит пользу.

В Clojure приняты другие, более простые способы следить за типами. Прежде всего это тесты: юнит-, интеграционные, smoke- и другие. Есть набор линтеров — утилит для статического анализа кода. Встроенный пакет `clojure.spec` позволяет описать структуру данных, в том числе аргументов функции и её результата. Всё вместе это устраняет проблемы динамической типизации, оставляя только её положительные стороны.

За восемь лет работы с Clojure я помню лишь несколько случаев, когда на проде возникала ошибка типов. Чаще всего причина была в слабой проверке входных данных, что исправлялось тестами.

Динамическая типизация в лучшей степени ложится на окружающий мир. Хоть и возможно описать типами 90% данных, останутся те 10%, для которых это сделать тяжело, при этом сложность типов растёт нелинейно. Добавьте сюда частые изменения бизнес-требований. Как бы странно это ни звучало, именно динамическая типизация оказывается удобней на долгом этапе разработки.

## Язык для удовольствия

**Павел:** мне удовольствие приносит даже не сам язык, сколько *культура* разработки приложений на нём, подкреплённая не только архитектурными догмами и лучшими практиками, но и заметной практической пользой. По мере ознакомления с некоторыми библиотеками первое время мозг сворачивается в трубочку, но, когда приходит понимание для чего они нужны, остаётся недоумение, почему приведённые подходы так слабо распространены.

Я сходу припоминаю два крупных эпизода, заставивших меня испытать это ощущение.

[component]: https://github.com/stuartsierra/component
[mount]: https://github.com/tolitius/mount
[integrant]: https://github.com/weavejester/integrant

Первый — с [Component][component]. Он меня отправил в целое путешествие на тему подмены кода у уже работающего приложения новой версией (что при разработке хочется делать постоянно) без каких-либо скрытых сложных механизмов, лишний раз закрепил тему вреда глобальных переменных, продемонстрировал интересный сорт внедрения зависимостей (dependency injection) и подтолкнул ознакомиться с другими подходами к этой же задаче на этом же языке: [Mount][mount] и [Integrant][integrant].

[re-frame]: https://day8.github.io/re-frame/re-frame/

Второй — с [re-frame][re-frame]. Инструменты по созданию браузерных приложений, что я видел до того момента, то и дело вводили для генерации разметки собственные языки с собственными правилами и операторами, вместо того чтобы подключить к процессу всю мощь JavaScript. Это отбивало у меня охоту лезть во фронтенд без острой необходимости, которой так и не возникло. Re-frame же всё делал в ClojureScript, в том числе разметку, и этим убедил меня как минимум попробовать. И это очень быстро вылилось в работающий браузерный чат на WebRTC.

И с тех пор я раз за разом возвращаюсь, надеясь, что случится ещё один эпизод.

**Иван:** очевидно, что на языке, на котором приятно программировать, вы сделаете больше. Clojure дарит удовольствие тем, что ёмко выражает ваши мысли. На текущий момент я не знаю языка, который переводит мои намерения в код с той же лёгкостью. Много лет назад я писал на Python и был такого же мнение о нём, но Clojure в этом плане ушла дальше.

Преимущества Clojure это:

- неизменямость данных, из-за чего уходит целый пласт ошибок;
- REPL-driven development, когда код запускают из редактора по мере написания. Из-за этого раньше видны ошибки и дальнейший путь разработки;
- макросы, с которыми легко писать выразительный код, избегать повторов и многое другое.

Ни в одном языке нет этих трёх компонентов, развитых в той же мере, что и в Clojure.

## Про популярность языка

**Павел:** было бы неплохо, чтобы язык стал популярнее. Но популяризация может произойти по-разному, и не все возможные сценарии этого мне нравятся. Иногда возникают обсуждения, что Clojure нужен какой-то доминирующий фреймворк или инструмент, который привлечёт огромную толпу людей. Что от этого бывает, я своими глазами видел в Ruby on Rails. Фреймворк захватил экосистему самого языка настолько, что разработчиков на Rails, вероятно, больше, чем разработчиков на Ruby, как бы парадоксально это ни звучало. И вредные привычки из фреймворка растекаются далеко за его пределы.

Поэтому мне даже нравится то, что в экосистеме Clojure нет монополистов в отдельных видах инструментов — я считаю, что это постепенно культивирует качественные решения через конкуренцию, а также способствует хорошей сочетаемости инструментов разного рода.

Но это, так уж вышло, несколько поднимает порог входа из-за того, что выбирать первые инструменты приходится вслепую, а это неприятно. Разработчик испытывает страх неизвестности, не хочется потом переписывать весь проект из-за неверного выбора на ранних этапах. Но, к счастью, обычно это и не требуется, т. к. нет чего-то, что пронизывает весь проект настолько, чтобы замена одного узла потребовала полного переписывания. И жертвовать этой чертой ради популярности мне видится вредным.

[rbi]: https://www.rbinternational.com/en/raiffeisen.html
[nubank]: https://nubank.com.br/en/

**Иван:** Популярность Clojure растёт, но это происходит медленно. На западе ее не считают экзотикой. Clojure применяют в крупных банках, например [Raiffeisen Bank International][rbi] и [NuBank][nubank].

[companies]: https://clojure.org/community/companies

На официальном сайте Clojure есть раздел [Companies][companies], где перечислены компании, когда-либо замеченные в найме Clojure-разработчиков. Возможно, читателей удивит длина списка, а также участие в нём Facebook, Apple, Netflix, Atlassian и других крупных компаний. Мне довелось переписываться с разработчиком из Apple. По его словам, Clojure используют для сбора данных и статистики.

Участники [clojure_ru][clojure_ru] вносят посильный вклад в развитие языка: проводят встречи, пишут библиотеки, книги, статьи в блогах, в том числе на английском языке.

## Низкая производительность: правда или миф

**Павел:** в любом проекте можно встретить «функциональные ядра», в которых происходит решение прикладных задач, и «императивные оболочки», через которые получаются условия задач и приводятся в исполнение решения. Иногда они не очень чётко разделены в коде, но разделить деятельность программы на эти две категории несложно.

Основное назначение «ядра» — решить задачу правильно. И это проще сделать, имея выразительный язык и хорошие средства для абстрагирования. Этого у Clojure в достатке. Но за их использование обычно приходится платить скоростью, и Clojure на это повлиять не в силах. Но это «ядро» лишь часть системы.

Основная же задача «оболочки» — принять и передать дальше данные так, чтобы с обеих сторон были довольны. Там важны скорость, низкая задержка и высокая пропускная способность — поэтому часто приходится от высоких абстракций отказываться и излагать алгоритмы более императивно и мутабельно, чтобы машине было проще их выполнять

И хоть у Clojure и есть функциональные черты и идиомы, императивный подход ему тоже не чужд (хотя и вторичен). У встроенных иммутабельных коллекций есть более шустрые мутабельные версии, а с объектами хост-платформы, зачастую мутабельными, можно работать напрямую. В таком стиле писать надёжный код сложнее и дольше, но если нужно, то можно. И это, на мой взгляд, мощное преимущество перед «насквозь функциональными» языками. Он «осторожно, но настойчиво» подталкивает к функциональному стилю, а не ставит перед фактом.

**Иван:** тезис о том, что функциональные языки медленные, был справедлив лет двадцать назад, но сегодня вопрос уже не актуален. На это есть несколько причин.

Появились алгоритмы неизменяемых структур, где копирование данных сведено к минимуму. И хотя они медленней изменяемых аналогов, разница не столь велика, а преимуществ гораздо больше.

Многие промышленные языки в той или иной степени функциональны: это Scala, Kotlin, Rust. Опыт показывает, что неизменяемость и монадические типы (Try, Maybe) удобны в разработке. Эти абстракции не бесплатны, но они окупаются.

Сегодня уже нет чёткого разделения языков по принципу императивный — функциональный. Удачные решения из ФП заимствуют, потому что они оправдывают себя на практике.

Как мы упоминали, Clojure — гибридный язык. Код на нём варьируется от императивного до функционального стиля. Разработчик вправе выбирать, какой крайности придерживаться. Медленные участки кода легко переписать на манер Java. Например, заменить неизменяемый словарь Clojure на HashMap, неизменяемый вектор — на массив и так далее.

[performance]: https://www.diva-portal.org/smash/get/diva2:1424342/FULLTEXT01.pdf

Существует исследование, где сравнивают скорость Java и Clojure: [A performance comparison of Clojure and Java][performance]. Вывод гласит, что Clojure медленней Java в среднем от 2 до 5 раз, что совпадает с моими личными наблюдениями. Однако в силу своей гибкости Clojure предлагает много способов ускорить неоптимальный код.

Вместе с тем код на Clojure оказывается если не на порядок, то примерно в 5-7 раз короче аналогичного кода на Java. Меньший объем кода упрощает его поддержку, облегчает внедрение новых требований.

[teleward]: https://github.com/igrishaev/teleward
[blog-backend]: https://github.com/igrishaev/blog-backend

Если важен быстрый старт, например, в окружении AWS Lambda, проект компилируют с помощью GraalVM и native-image. Получается бинарный файл, запуск которого занимает микросекунды. Так работает несколько моих проектов: [телеграм-бот][teleward], обработчик [HTML-форм][blog-backend] и другие.

## Важность иммутабельности и ленивости языка

**Павел:** ленивости в Clojure, надо сказать, не так уж много. Есть ленивые преобразования коллекций, ленивые последовательности. Это встречается более-менее везде, где существуют итераторы. У Clojure преобразования в стандартной библиотеке ленивые *по умолчанию*, что встречается уже не так часто.

Полезна ли такая ленивость? Определённо. Она позволяет обрабатывать данные потоково, когда это возможно, держа в памяти только то, что нужно на текущем шаге. Это помогает проще обрабатывать непрерывные потоки или просто большие объёмы данных, не затрачивая больше ресурсов, чем необходимо. А поскольку это поведение по умолчанию, такую полезную возможность можно получить случайно, не задумываясь.

А вот с иммутабельностью интереснее. Я бы даже сказал, что это одна из самых полезных особенностей языка! Есть общие доводы в пользу иммутабельности — то, что вносимые в объекты изменения видны явно, т. к. изменённую версию надо откуда-то явно получить и передать дальше. Это снижает риск «случайной зависимости», когда разработчик, вызывая функцию на каком-то значении, не рассчитывает, что она его изменит и, возможно, поломает остаток алгоритма.

Но есть у иммутабельности и синергия с разработкой через REPL. Поскольку большинство конструкций вместо изменения уже сформированных объектов формируют новые, код очень часто можно выполнять по кусочкам и повторно. Благодаря этому можно делать в кусочках небольшие изменения и немедленно проверять, какой они вызывают эффект, не перезапуская всей программы. Это позволяет идти к решению постепенно, небольшими шагами, вовремя поворачивая, когда что-то идёт не по плану. А ещё это сильно упрощает юнит-тесты.

**Иван:** Clojure поддерживает ленивость и иммутабельность «из коробки», в этом его отличительная черта. Ленивость применяют по ситуации: иногда с ней трудно отладить код, поэтому ленивые вычисления «проталкивают», чтобы они совершились перед следующей операцией.

Гораздо важнее иммутабельность данных. Она исключает ошибки, связанные со случайными изменениями данных: словарей, списков, полей объекта. Метод `.setSomething(x)`, вставленный или удалённый где-то посередине, способен разрушить проект. В Clojure это невозможно в принципе: каждая функция принимает результат вычислений другой функции. Даже если какая-то функция дополнит словарь новыми полями, прежний экземпляр останется нетронутым.

В Clojure есть свои изменяемые типы, но они ведут себя как обёртки над неизменяемыми. Они применяются точечно и выделяются на общем фоне. Изменяемый объект, добавленный без причины, вызовет замечания коллег.

Программировать в неизменяемом стиле сперва непривычно. Однако позже, когда мозг перестроится на новый лад, будет так же трудно вернуться к изменяемому стилю. Станет очевидно, насколько он подвержен ошибкам.

## Ссылки и ресурсы

[rationale]: https://clojure.org/about/rationale
[getting_started]: https://clojure.org/guides/getting_started

- Документация
  - [Официальный сайт][clojure] и его разделы:
    - [Rationale][rationale]
    - [Getting Started][getting_started]

- Книги
  - [«Программирование на Clojure»][clojure-dmk]
  - [«Clojure на производстве»][clojure-in-prod]
  - [Clojure for Brave and True][braveclojure]
  - [The Joy of Clojure][the-joy-of-clojure]

[planet-clojure]: https://planet.clojure.in/
[ask-clojure]: https://ask.clojure.org/

- Веб-ресурсы
  - [Planet Clojure][planet-clojure] - агрегатор блогов на тему Clojure
  - [Ask Clojure][ask-clojure] - сайт вопросов и ответов

[ClojureTV]: https://www.youtube.com/@ClojureTV/videos
[simple-made-easy]:https://www.youtube.com/watch?v=LKtk3HCgTa8
[history]: https://www.youtube.com/watch?v=nD-QHbRWcoM
[inside-clojure]: https://www.youtube.com/watch?v=wASCH_gPnDw

- Видео
  - Канал [ClojureTV][ClojureTV] на Ютубе
  - [Simple Made Easy][simple-made-easy] by Rich Hickey (2011)
  - [A History of Clojure][history] by Rich Hickey with Q&A
  - [Inside Clojure][inside-clojure] - Rich Hickey and Brian Beckman

- Сообщества
  - Чат [@clojure_ru][clojure_ru] в Телеграме
  - Группа [Clojurians][clojurians] в Slack
