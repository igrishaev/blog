---
layout: post
title: "Мышление лямбдами"
permalink: /lambda-thinking/
tags: aws programming lambda
---

В проекте, который плотно сидит на Амазоне, я познакомился с новой для себя
вещью: мышлению лямбдами. Это когда на каждый чих создается лямбда, и кто-то ее
вызывает: бекенд, фронтенд, очередь или хук, повешенный на бакет.

Раньше я об этом не думал, но оказалось, что лямбда — самый сложный продукт
Амазона. В экосистеме AWS лямбды повсюду. Их можно приделать к любому
сервису. Загрузил файл в S3 — дернулась лямбда. Поднялся инстанс EC2 — дернулась
лямбда. Лямбда может быть обработчиком очередей SNS и SQS. Это универсальный
клей, которым можно соединить что угодно.

Лямбда может создана на любой технологии: на Джаве, на Го, на баше. Она может
быть голым бинарником или скриптом на Питоне или Ноде. В последних случаях ее
код можно поместить прямо в yaml-конфиг Cloud Formation.

Ситуация, когда лямбда вызывает лямбду, та вызывает лямбду, та вызывает лямбду,
которая пишет файл в S3, и на это событие вызывается лямбда, уже не кажется
абсурдом. Поначалу шокирует, но привыкаешь.

Другой случай: как поднять инстанс EC2 на заданное количество времени? А вот
как: дернуть лямбду и передать ей число секунд, скажем, 7200 (два часа). Лямбда
запустит инстанс из образа, после чего запишет в S3 файл с числом оставшихся
секунд. В облаке работает шедулер, который каждые 5 минут запускает другую
лямбду. Эта лямбда читает число секунд из бакета, вычитает из него 300 секунд и
записывает обратно. Если время истекло, она дергает третью лямбду, которая
убивает инстанс. Вот такие многоходовочки.

Сперва у меня было чувство, словно я рассматриваю картины Сальвадора
Дали. Первая реакция — что, так можно было? Оказалось да, можно. С этим живут,
это поддерживают, на этом зарабатывают деньги. Такие схемы даже работают. Тех,
кто их проектирует, называют AWS-архитекторами.

Все это я понимаю и принимаю. И все же малодушно хочу, чтобы такого было меньше.