---
layout: post
title: "Список через запятую"
permalink: /comma-separated/
tags: programming comma csv list
---

Одна из самых дурацких вещей в айти – это список через запятую, например:

~~~
(1, 2, 3)
["test", "foo", "hello"]
[{:id 1}, {:id 2}, {:id 3}]
~~~

Каждый, кто работал с таким форматом, знает, какой геморрой учитывать
запятые. Элементы нельзя просто записать в цикле. Нужно собрать их в массив, а
потом join-ить запятой. Это сводит на нет стриминг элементов, когда их много. А
чтобы работал стриминг, нужно завести флажок "запятая уже была", выставить его в
первый раз и постоянно проверять: была или не была?

То есть на каждом шаге из миллиона нужно делать эту проверку, которая сработала
один раз. Из-за какой-то никчемной запятой.

Какие проблемы возникнут, если запятые убрать?

~~~
(1 2 3)
["test" "foo" "hello"]
[{:id 1} {:id 2} {:id 3}]
~~~

Не вижу причины, по которой любой из списков не может быть распаршен. Все три
читаются и подлежат парсингу на ура.

В одном скобочном языке запятые вообще считаются пробелами: они уничтожаются
парсером, словно их нет. И это никак не влияет работу: все читается, и даже
лучше: меньше визуального шума.

Список через запятую – рудимент, от которого пора избавиться. Случись вам
изобретать свой формат данных – откажитесь от запятых как значимых символов.