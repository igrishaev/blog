---
layout: post
title:  "Уровень"
permalink: /prog-level/
tags: programming
---

Пока мир сходит с ума по искусственному интеллекту, всплакну о низком уровне
разработчиков. Подкатило, нужно выплеснуть.

Ситуация: разработчик пишет функцию `get-by-id`, чтобы достать сущность из
базы. Не моргнув глазом, он передает ее в `map` на пять тысяч элементов. Это, на
минутку, один запрос, а запросов может десятки в секунду. Подобные вещи
приходится ловить в code review и объяснять, что один запрос лучше, чем пять
тысяч.

Идет 2023 год, а программисты пишут SQL конкатенацией строк. В порядке вещей код
на два экрана с `format`, `str` и `join`, который ни понять, ни
отладить. Полученный запрос уходит в базу, и дай бог, чтобы оно работало. Если
передать nil или пустой список, получим битый SQL, потому что автор этого не
предусмотрел. И конечно, инъекции во все поля.

Почему-то программисты не могут записать и получить данные из базы. Им нужна
ORM, и чтобы она сразу мапилась на REST. Получается километр глючного кода без
документации и поддержки. Автор ORM отлынивает от задач под видом ее
доработки. Часть команды уходит в партизаны: работают с базой через SELECT и
UPDATE в обход ORM. Так спокойней, главное чтобы автор ORM не зашел в
пулл-реквест.

Разработчики игнорируют линтеры. Проект уже не раз сменил команду, люди пишут в
разных редакторах, и ни у кого он толком не настроен. Кривые отступы, экраны
закомментированного кода, неиспользуемые импорты и переменные. Это в порядке
вещей.

Программисты не доводят дело до конца, хотя в бóльшей степени это упрек
руководству. Например, у кого-то задача, чтобы была документация
Swagger. Человек пишет генератор json-файла, покрывает тестами, все
готово. Осталось захостить, чтобы документацию увидели клиенты. Но прилетает
горящая задача или девопс-инженер уходит в отпуск. В итоге документация есть, но
ее никто не видит. Задача не достигнута, время потрачено зря, но это никого не
смущает. Недостигнутые цели я вижу постоянно.

Сюда же относятся висящие пул-реквесты. Открываешь борду и видишь у кого пять, а
у кого семь пул-реквестов. Зачем программист писал код, если его не принимают?
Если бы он смотрел Ютуб, эффект был бы тот же. Почти во всех системах можно
задать auto-expire, не говоря уж о ботах, которых полно.

Беда с локальным окружением. Программисту лень потратить день на
`docker-compose.yaml`, чтобы сервисы работали локально. Приходится объяснять,
что локальный ресурс лучше стейджинга где-то в Амазоне.

Иной программист генерит айдишники для базы вручную. Берет рандом от 0 до 9999 и
густо перчит номером треда, числом миллисекунд и фазой Луны. И это работает в
проде.

Программисты любят кокетничать, что уперлись в базу: терабайты данных, не
вывозит нагрузку, все дела. При этом в базу уходят кривейшие запросы, а сама она
превратилась в свалку, потому что там хранят кэш, логи S3 и черт знает что.

У программистов туго с отладкой. Под отладкой я имею в виду остановку кода на
середине, чтобы выяснить локальные переменные. Это могут единицы. Остальные либо
ставят принты и мотают экраны логов, либо вообще сдаются. Иные заявляют, что в
божественной Кложе отладчик не нужен. Это вообще за гранью.

И наконец, главное: карго-культ. Так писали до нас, так пишем и мы. Кривой
нейминг? Дурацкая организация кода? Ничего, консистенси важнее. И хотя прошлый
разраб видел Кложу второй раз в жизни, все следуют его бредовым начинаниям.

Все это я видел даже когда писал на Дельфи и 1С до прихода в промышленную
разработку. Меняются лица, а косяки остаются с нами. Конечно, я их тоже
совершал, но будучи записанными, они переживаются легче.

Все, отпустило, работаем дальше.