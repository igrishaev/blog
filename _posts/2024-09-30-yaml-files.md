---
layout: post
title: "Файлы Yaml"
permalink: /yaml-files/
tags: programming yaml aws
---

Пару дней назад я славно покувыркался с yaml-файлами.

Значит, есть два репозитория с плейбуками Ansible. Нужно добавить в каждый файл
новую таску. Копирую и вставляю в первый — все в порядке. Копирую во второй, и
на CI лезут ошибки парсера.

Смотрю, а дело вот в чем: в первом файле были отступы в два пробела. В том
куске, что я копировал, было тоже два, поэтому он сел без проблем. А во втором
файле кто-то использовал четыре пробела. Вставил таску где-то посередине, и
пожалуйста: ParserException on line 63, position 15. Тупил над этим десять
минут, еще десять минут заняла вторая неудачная итерация, в итоге через полчаса
починил.

Ну и вообще, ямл — он такой ямл. Когда плоская мапа, выглядит красиво, вопросов
нет. Но когда у тебя вектор мап векторов мап, это просто ад. Все эти минусики и
двоеточия сливаются в общую массу, и если нужно перенести кусок с одного уровня
на другой — все, тушите свет. А бородатым девопсам с километрами ямла это
нормально: у них ансиблы и кубернетисы всякие.

Более общая мысль — значимые отступы были ошибкой. Идея, что мы уберем фигурные
скобки и заменим пробелами, выглядит красиво только на первый взгляд. За красоту
нужно платить — чаще всего, падающими билдами или скрытыми багами, когда мапа
уехала не туда.

Разумеется, вы готовы предъявить: в Питоне отступы и он популярен. Съел? Все
равно считаю, что пробелы — это ошибка. Когда я занимался Питоном, у нас были
свои кулстори на эту тему. Например, оператор return уехал из-под условия или в
критический патч закрался таб — и наложение этого патча положило прод.

Я как-то уже писал: хорошо, когда форма имеет явные начало и конец. Чтобы не
было контекста, в рамках которого нужно гадать, закончилась форма или нет. Для
обозначения границ можно взять скобки. И получится..., стойте, это же... о
щи-и-и..!
