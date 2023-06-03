---
layout: post
title:  "SQL и DISTINCT ON"
permalink: /sql-distinct-on/
tags: programming sql postgresql logs
---

Еще одна полезная штукенция в SQL — это оператор `DISTINCT ON`. От обычного `DISTINCT` он отличается тем, что определяет уникальность записей не по всем полям, а только указанным. Эта особенность позволяет писать интересные запросы.

Предположим, мы ведем таблицу курсов валют относительно рубля. Вот ее структура:

~~~sql
create table rates (
  id      serial primary key,
  value   text not null,
  instant timestamp without time zone not null default now(),
  cost    bigint not null
);
~~~

и данные:

~~~sql
insert into rates (value, cost) values ('usd', 7968);
insert into rates (value, cost) values ('usd', 7988);
insert into rates (value, cost) values ('usd', 8031);

insert into rates (value, cost) values ('eur', 8623);
insert into rates (value, cost) values ('eur', 8581);
insert into rates (value, cost) values ('eur', 8699);
~~~

Величины хранятся целым числом в центах. Все запросы я специально ввел по одному, чтобы у курсов были разные даты.

У подобных таблиц особенность: часто нужны их срезы по датам, например первые или последние на текущий момент. На первый взгляд не ясно, как это делать. До того, как я узнал про `DISTINCT ON`, я делал весьма топорно: сперва группировал по валюте, чтобы получить максимальную дату:

~~~sql
select value, max(instant) as instant
from rates
group by value;

 value |          instant
-------+----------------------------
 eur   | 2023-06-02 22:17:04.902392
 usd   | 2023-06-02 22:16:57.007947
~~~

, а затем оборачивал это в подзапрос и соединял с основной таблицей по валюте и дате:

~~~sql
select rates.*
  from rates
join (
  select value, max(instant) as instant
  from rates
  group by value
) as sub
on
      rates.value = sub.value
  and rates.instant = sub.instant;

 id | value |          instant           | cost
----+-------+----------------------------+------
  6 | usd   | 2023-06-02 22:16:57.007947 | 8031
  9 | eur   | 2023-06-02 22:17:04.902392 | 8699
~~~

Это неуклюже и требует индексов для эффективного JOIN.

Гораздо лучше смотрится с `DISTINCT ON`. Укажем в запросе, что записи уникальны в разрезе валют. В этом случае в выборке останется одна запись с `usd` и одна с `eur`, а остальные будут пропущены. Остается отсортировать их так, чтобы первыми шли записи с максимальной датой или минимальной.

Пример с минимальной датой:

~~~sql
select distinct on (value) *
from rates
order by value, instant;

 id | value |          instant           | cost
----+-------+----------------------------+------
  7 | eur   | 2023-06-02 22:16:59.46867  | 8623
  4 | usd   | 2023-06-02 22:16:50.603444 | 7968
~~~

А это — с максимальной, потому что записи следуют по убываю `instant`:

~~~sql
select distinct on (value) *
from rates
order by value, instant desc;

 id | value |          instant           | cost
----+-------+----------------------------+------
  9 | eur   | 2023-06-02 22:17:04.902392 | 8699
  6 | usd   | 2023-06-02 22:16:57.007947 | 8031
~~~

Обратите внимание, что сортировка `order by` начинается с того поля, которое указано в `distinct on`. За счет этого база эффективно пропускает дубли без промежуточных списков.

Если записей много, понадобится индекс на поле уникальности и критерий сортировки, в нашем случае `value` и `instant`:

~~~clojure
CREATE INDEX rates_last_idx ON rates (value, instant desc);
~~~

Похоже решаются другие задачи, связанные со временем или версиями, например прогноз погоды, версионирование статей или сущностей.
