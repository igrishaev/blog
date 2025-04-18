---
layout: post
title: "Про абстракции"
permalink: /abstractions/
tags: programming bingo abstractions
telegram_id: 788
---

Как только зашла речь об абстракциях, знайте: дело швах. Абстракции -- это
булщит-бинго, в котором линейному разрабу никогда не выиграть. Это штучки для
менеджеров и архитекторов, то есть тех, кто парит над кодом, а не стоит в нем по
колено.

Бывает, разработчики увлекаются и играют в эти шашни: всерьез обсуждают
абстракции. Они забывают, что любая функция -- это уже абстракция. Например,
функция поиска простого числа может перебирать числа линейно, с интервалом или
брать из таблицы. Может кешировать. Аналогично с факториалом или числом
Фиббоначи: потребитель не знает, что внутри, поэтому функция абстрактна. Сюда же
списки и хеш-таблицы.

В ООП языках абстракция вообще идет из коробки. Скажем, написал интерфейс
`UserRepo` с методом `User getUserById(Integer id)` и везде его передаешь. А
потом пишешь классы `JDBCUserRepo` и `MongoUserRepo`, каждый из которых ходит
куда нужно. Об этом пишут в каждом учебнике по Джаве. Что может быть проще?

Нормальный код в абстракции не нуждается -- он и есть абстракция над байткодом
или машкодом. Вот и весь разговор.

Лучшее, что можно сделать в споре про абстракции -- это промолчать. Скорее
всего, любителя абстракций отпустит, и он забудет о них так же быстро, как и
вспомнил. И вы спокойно сделаете все как надо. Спорить и лезть на рожон глупо,
потому что повторюсь: абстракции -- это булщит-бинго, в котором разработчику
никогда не выиграть.
