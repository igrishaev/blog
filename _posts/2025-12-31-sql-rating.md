---
layout: post
title: "Рейтинг пользователей на чистом SQL"
permalink: /sql-rating/
tags: programming sql postgres rating
telegram_id: 989
---

Решил написать небольшой мануальчик по SQL. В нем рассматривается задача,
которую я подсмотрел у Кирилла Мокевнина. Возможно, кому-то из вас ее дадут на
собеседовании.

Задача следующая: сделать на сайте систему рейтинга. Пользователям начисляют
баллы за разные достижения, и нужно вывести топ-100 пользователей. Позиция в
рейтинге должна изменяться мгновенно. Например, кто-то получил 200 баллов и
сразу оказался на вершине таблицы. Предполагается, что имеется только
реляционная база данных, все остальное – по желанию.

Эта задача мне очень понравилась. Она лаконичная, но ее можно наращивать очень
долго. Удивился тому, в комментариях – а у Кирилла довольно большая аудитория –
почти никто не решил ее силами SQL. Большинство предлагали какие-то кэши,
редисы-кафки, Firebase и прочую ахинею. Дело не в том, что это плохие
инструменты. Наоборот, в нужных местах Редис и Кафка изумительны. Просто в
данном случае их выбор ничем не обоснован.

Итак, давайте решим задачу силами ванильного Postgres. Чтобы разогреться,
сначала сделаем небольшое послабление – предположим, что рейтинг нужно обновлять
не мгновенно, а раз в час. Позже мы сделаем все как надо.

Подготовим таблицу пользователей:

~~~sql
create table users(
    id serial primary key,
    name text not null
);
~~~

Вставим в нее 100 тысяч случайных людей:

~~~sql
insert into users
select n, format('User %s', n)
from generate_series(1, 100000) as seq(n);
~~~

Создадим таблицу для начислений баллов. Она хранит код пользователя, баллы и
поле reason – опциональный комментарий за что эти баллы.

~~~sql
create table user_points(
    user_id integer not null,
    points integer not null dеfault 0,
    reason text
);
~~~

Начислим каждому пользователю десять раз случайное число баллов:

~~~sql
insert into user_points(user_id, points, reason)
select
    n / 10 + 1,
    (random() * n)::int % 100,
    format('iteration %s', n)
from
    generate_series(1, 1000000) as seq(n);
~~~

Объявим материализованную вьюху. Ее запрос выбирает сумму баллов с группировкой
по коду пользователя:

~~~sql
create materialized view mv_user_rating as
select
    user_id, sum(points) as total_points
from user_points
group by user_id;
~~~

Обновим ее:

~~~sql
refresh materialized view mv_user_rating;
~~~

Добавим индекс по убыванию общего числа баллов и обновим его:

~~~sql
create index idx_mv_total_points on mv_user_rating
    using btree (total_points desc);

analyze mv_user_rating;
~~~

Теперь к сути: выберем топ-100 записей из вьюхи по убыванию суммы
баллов. Подцепим джоином пользователей, выберем их имена. Запрос:

~~~sql
select
    u.id as user_id,
    u.name as user_name,
    mv.total_points as total_points
from
    mv_user_rating mv,
    users u
where
    mv.user_id = u.id
order by
    mv.total_points desc
limit
    100;
~~~

Частичный результат:

~~~text
┌─────────┬────────────┬────────┐
│ user_id │ user_name  │ points │
├─────────┼────────────┼────────┤
│      96 │ User 96    │   1225 │
│      45 │ User 45    │   1185 │
│      85 │ User 85    │   1169 │
│      10 │ User 10    │   1140 │
│      33 │ User 33    │   1138 │
│      80 │ User 80    │   1135 │
│      48 │ User 48    │   1133 │
│      53 │ User 53    │   1121 │
│      11 │ User 11    │   1119 │
│      91 │ User 91    │   1099 │
│      79 │ User 79    │   1096 │
│      56 │ User 56    │   1090 │
│      72 │ User 72    │   1089 │
│      31 │ User 31    │   1088 │
│      40 │ User 40    │   1072 │
│      97 │ User 97    │   1070 │
│      65 │ User 65    │   1068 │
│      42 │ User 42    │   1058 │
│      43 │ User 43    │   1057 │
│      78 │ User 78    │   1054 │
│      63 │ User 63    │   1053 │
│      54 │ User 54    │   1049 │
│      93 │ User 93    │   1031 │
│      29 │ User 29    │   1029 │
│      51 │ User 51    │   1028 │
│     100 │ User 100   │   1027 │
│      98 │ User 98    │   1021 │
│      69 │ User 69    │   1014 │
│      28 │ User 28    │   1003 │
│      67 │ User 67    │    994 │
│      60 │ User 60    │    990 │
│      21 │ User 21    │    987 │
│      58 │ User 58    │    986 │
│      26 │ User 26    │    984 │
~~~

Будет ли он тормозить? Посмотрим план:

~~~text
explain analyze
select
    u.id as user_id,
    u.name as user_name,
    mv.total_points as total_points
from
    mv_user_rating mv,
    users u
where
    mv.user_id = u.id
order by
    mv.total_points desc
limit
    100;

├────────────────────────────────────────
│ Limit  (cost=0.58..38.88 rows=100 width
│   ->  Nested Loop  (cost=0.58..38292.36
│         ->  Index Scan using idx_mv_tot
│         ->  Index Scan using users_pkey
│               Index Cond: (id = mv.user
│ Planning Time: 0.286 ms
│ Execution Time: 0.368 ms
└────────────────────────────────────────
~~~


Обе таблицы попадают в индексы, стоимость – копейки. Поэтому ответ – не будет.

Теперь дело за тем, как обновлять вьюху. У материализованных вьюх особенность:
во время обновления она недоступна. Можно обновлять их параллельно при помощи
`refresh materialized view concurrently`. Это медленней, зато не блокирует
чтение. Попытаемся:

~~~sql
refresh materialized view concurrently mv_user_rating;

ERROR:  cannot refresh materialized view "public.mv_user_rating" concurrently
HINT:  Create a unique index with no WHERE clause
       on one or more columns of the materialized view.
~~~

Что такое? Дело в том, что для concurrently нужен хотя бы один уникальный
индекс. По нему обновление вьюхи отслеживает свой прогресс. Добавим такой
индекс:

~~~sql
create unique index idx_uq_mv_user_rating_user_id
    on mv_user_rating(user_id);
~~~

Теперь параллельное обновление работает:

~~~sql
refresh materialized view concurrently mv_user_rating;
~~~

Как сделать обновление регулярным? Поможет стороннее расширение `pg_cron`. Из
коробки его нет, но оно доступно во всех пакетах и установлено почти у всех
облачных провайдеров. После установки включите его:

~~~sql
create extension pg_cron;
~~~

Добавьте задачу на расписание:

~~~sql
SELECT cron.schedule(
    'cron_job_refresh_user_rating',
    'refresh materialized view concurrently mv_user_rating;'
);
~~~

В результате каждый час вьюха будет обновляться параллельно. Для проверки
регулярных задач и их истории служат специальные таблицы.

Это был прогрев. Напомню, что мы сделали послабление: обновляем рейтинг каждый
час, а не мгновенно. Пора это исправить.

Вьюха становится не нужна, ее можно удалить. Вместо нее создайте таблицу
аналогичной структуры:

~~~sql
create table user_total_points(
    user_id integer primary key,
    total_points integer not null dеfault 0
);
~~~

Чтобы начислить пользователю баллы и одновременно изменить его рейтинг, нужно
сделать два действия. Первое – записать баллы в таблицу `user_points` как мы
делали это раньше. Второе – если в итоговой таблице есть пользователь, прибавить
баллы к тем, что есть. Если нет – вписать туда пользователя и начальные баллы.

На этом месте говорят "триггеры", и зря. Триггеры – слишком сложный
инструмент. Они слишком строгие, и порой это неудобно, например в разработке, в
тестах, в моделировании ситуаций. Ниже мы решим задачу без триггеров.

Предположим, пользователю 999 начислено 100 баллов. Первый запрос будет таким:

~~~sql
insert into user_points(user_id, points)
values (999, 100);
~~~

Второй – посложнее. Это UPSERT в таблицу рейтинга, который либо вставляет
данные, либо обновляет их:

~~~sql
insert into user_total_points(user_id, total_points)
  values (999, 100)
  on conflict(user_id)
  do update
  set total_points = user_total_points.total_points
    + excluded.total_points;
~~~

Все это делается в транзакции, чтобы не допустить частичных изменений.

~~~sql
begin;
-- query 1;
-- query 2;
commit;
~~~

Выполним транзакцию два раза. В истории баллов будет две записи по 100, а в
таблице рейтинга – их сумма 200.

~~~text
select * from user_points;
┌─────────┬────────┬────────┐
│ user_id │ points │ reason │
├─────────┼────────┼────────┤
│     999 │    100 │ <null> │
│     999 │    100 │ <null> │
└─────────┴────────┴────────┘

select * from user_total_points;
┌─────────┬──────────────┐
│ user_id │ total_points │
├─────────┼──────────────┤
│     999 │          200 │
└─────────┴──────────────┘
~~~

Транзакцию можно опустить, если переписать запрос на CTE. У таких запросов
особенность – все они видят один снимок данных и выполняются атомарно. Это
удобно, потому что транзакцию begin/end легко потерять, а в случае CTE это
невозможно.

~~~sql
with
step_1 as (
    insert into user_points(user_id, points)
    values (999, 100)
)
insert into user_total_points(user_id, total_points)
    values (999, 100)
on conflict(user_id)
    do update
    set total_points = user_total_points.total_points
        + excluded.total_points;
~~~

Выполним его три раза, и в таблице рейтинга окажется значение 500:

~~~text
┌─────────┬────────┬────────┐
│ user_id │ points │ reason │
├─────────┼────────┼────────┤
│     999 │    100 │ <null> │
│     999 │    100 │ <null> │
│     999 │    100 │ <null> │
│     999 │    100 │ <null> │
│     999 │    100 │ <null> │
└─────────┴────────┴────────┘

select * from user_total_points;

┌─────────┬──────────────┐
│ user_id │ total_points │
├─────────┼──────────────┤
│     999 │          500 │
└─────────┴──────────────┘
~~~

Если в базу пишут клиенты из разных систем, им будет неудобно таскать за собой
этот запрос. Сделаем так: напишем функцию, которая принимает код пользователя и
сколько баллов добавить. Функция возвращает суммарные баллы после всех
изменений. Вот она:

~~~sql
create or replace function func_add_points
    (user_id integer, points integer)
returns integer
as $$
with step_1 as (
    insert into user_points(user_id, points)
    values (user_id, points)
)
insert into user_total_points(user_id, total_points)
    values (user_id, points)
on conflict(user_id) do update
    set total_points = user_total_points.total_points
        + excluded.total_points
returning
    total_points
$$
language sql strict parallel safe;
~~~

Теперь клиенты вызывают функцию select `func_add_points(1234, 500)`, а что
внутри – их не касается. Функцию удобно вызывать из psql, если это баш-скрипт.

Вот как накинуть баллы – и одновременно изменить рейтинг – некоторым
пользователям из диапазона:

~~~text
select
    n as user_id,
    func_add_points(n, n * 5)
    as total
from
    generate_series(25, 50) seq(n);

┌─────────┬───────┐
│ user_id │ total │
├─────────┼───────┤
│      25 │   125 │
│      26 │   130 │
│      27 │   135 │
│      28 │   140 │
│      29 │   145 │
│      30 │   150 │
│      31 │   155 │
│      32 │   160 │
│      33 │   165 │
│      34 │   170 │
│      35 │   175 │
│      36 │   180 │
│      37 │   185 │
│      38 │   190 │
│      39 │   195 │
│      40 │   200 │
│      41 │   205 │
│      42 │   210 │
│      43 │   215 │
│      44 │   220 │
│      45 │   225 │
│      46 │   230 │
│      47 │   235 │
│      48 │   240 │
│      49 │   245 │
│      50 │   250 │
└─────────┴───────┘
~~~

Функцию можно вызывать параллельно. Давайте накинем баллов пользователю 1003 и
посмотрим на результат:

~~~text
select
    user_id,
    points,
    func_add_points(user_id, points)
    as total
from (values
    (1003, 3),
    (1003, 2),
    (1003, 1),
    (1003, 5),
    (1003, 7))
    as vals(user_id, points);

┌─────────┬────────┬────────┐
│ user_id │ points │ total  │
├─────────┼────────┼────────┤
│    1003 │      3 │      3 │
│    1003 │      2 │      5 │
│    1003 │      1 │      6 │
│    1003 │      5 │     11 │
│    1003 │      7 │     18 │
└─────────┴────────┴────────┘
~~~

Видим, что итоговая сумма приращивалась каждый раз правильно.

Теперь делаем то же самое, что и со вьюхой. Добавим индекс по убыванию баллов:

~~~sql
create index idx_total_points on user_total_points
    using btree (total_points desc);
~~~

Выберем из таблицы рейтинга топ-100 записей, приклеим пользователей и готово:

~~~text
select
    u.id as user_id,
    u.name as user_name,
    total.total_points as total_points
from
    user_total_points total,
    users u
where
    total.user_id = u.id
order by
    total.total_points desc
limit
    100;

┌─────────┬───────────┬───────┐
│ user_id │ user_name │ total │
├─────────┼───────────┼───────┤
│     999 │ User 999  │  1500 │
│      50 │ User 50   │   250 │
│      49 │ User 49   │   245 │
│      48 │ User 48   │   240 │
│      47 │ User 47   │   235 │
│      46 │ User 46   │   230 │
│      45 │ User 45   │   225 │
│      44 │ User 44   │   220 │
│      43 │ User 43   │   215 │
│      42 │ User 42   │   210 │
│      41 │ User 41   │   205 │
│      40 │ User 40   │   200 │
│      39 │ User 39   │   195 │
│      38 │ User 38   │   190 │
│      37 │ User 37   │   185 │
│      36 │ User 36   │   180 │
│      35 │ User 35   │   175 │
│      34 │ User 34   │   170 │
│      33 │ User 33   │   165 │
│      32 │ User 32   │   160 │
│      31 │ User 31   │   155 │
│      30 │ User 30   │   150 │
│      29 │ User 29   │   145 │
│      28 │ User 28   │   140 │
│      27 │ User 27   │   135 │
│      26 │ User 26   │   130 │
│      25 │ User 25   │   125 │
└─────────┴───────────┴───────┘
~~~

Одно из улучшений вот в чем. Выше у каждого пользователя разное число баллов, и
нет случаев, когда двое участников делят одно место. Давайте добавим
пользователю 49 пять баллов, чтобы уравнять его с пользователем 50:

~~~text
select func_add_points(49, 5);

┌─────────────────┐
│ func_add_points │
├─────────────────┤
│             250 │
└─────────────────┘
~~~

Их место будет неоднозначно: мы сортируем по убыванию суммы баллов, но если
значения равны, порядок записей не гарантируется. Чтобы участники не спорили,
кто на втором, а кто на третьем месте, вычислим плотный ранг. Это оконная
функция, которая учитывает поля с одинаковым значением. Обычный ранг нумерует
записи подряд, а плотный – с учетом повторов. Запрос:

~~~sql
select
    u.id as user_id,
    u.name as user_name,
    total.total_points as total_points,
    dense_rank() over w as rank
from
    user_total_points total,
    users u
where
    total.user_id = u.id
window
    w as (order by total_points desc)
order by
    total_points desc
limit
    100;
~~~

Результат:

~~~text
┌─────────┬───────────┬───────┬──────┐
│ user_id │ user_name │ total │ rank │
├─────────┼───────────┼───────┼──────┤
│     999 │ User 999  │  1500 │    1 │
│      50 │ User 50   │   250 │    2 │
│      49 │ User 49   │   250 │    2 │
│      48 │ User 48   │   240 │    3 │
│      47 │ User 47   │   235 │    4 │
│      46 │ User 46   │   230 │    5 │
│      45 │ User 45   │   225 │    6 │
│      44 │ User 44   │   220 │    7 │
│      43 │ User 43   │   215 │    8 │
│      42 │ User 42   │   210 │    9 │
│      41 │ User 41   │   205 │   10 │
│      40 │ User 40   │   200 │   11 │
│      39 │ User 39   │   195 │   12 │
│      38 │ User 38   │   190 │   13 │
│      37 │ User 37   │   185 │   14 │
│      36 │ User 36   │   180 │   15 │
│      35 │ User 35   │   175 │   16 │
│      34 │ User 34   │   170 │   17 │
~~~

Теперь участники 49 и 50 делят второе место, и неоднозначность ушла.

Собственно, вот как решить эту задачу силами SQL. Не понадобились кафки,
распределенные кэши и все остальное. Схема простая и быстрая, ее легко
объяснить.

Любите Постгрес! Учите Пострес! Всем любви и добра.

Код: https://gist.github.com/igrishaev/7d171d6c4882e922d81469c2e556345c
