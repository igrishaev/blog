---
layout: post
title:  "Не смешивать языки"
permalink: /dont-mix-langs/
tags: programming
---

Пришел к выводу из заголовка: не следует смешивать языки. Не должно быть такого, что эта часть на одном языке, а та на другой. Бек на Питоне, фронт на JS — отстой. Бек на Кложе, CLI на Golang — отстой. Сервис логики на Джаве, сервис авторизации на Расте — отстой.

Этот языковой цирк усложняет и без того сложную реальность. Раньше я думал, что протоколы и схемы решают проблемы. Не все ли равно, что на том конце провода, если данные в JSON? Оказалось, не все равно.

Даже если данные в JSON, некоторые языки не могут работать с ними так, как это делают другие. Например, Гоферы и прочие ребята со статической типизацией не могут просто распарсить JSON. Они объявляют три экрана вложенных структур и натягивают на них JSON. Малейшее расхождение, какое-то поле nil — и все упало. Опять созвон и новая задача.

Когда читал исходники проекта на Golang, который забирает JSON от Кложи, сто раз удивился, зачем добровольно садиться на кактус. Совершенно разные языки и идиомы, подход к коллекциям и обработке данных. На ровном месте впендюрили проблему: случись беда, я не смогу поправить у гоферов, а они у меня.

Два и более языков — это постоянное переключение контекста. Это свои погремушки: пакетные менеджеры, зависимости, подводные камни. Это разные идиомы. Это неполное погружение в каждую область. Это удел "фулстек-разработчика" — посредственности, который одинаково плохо знает обе среды.

Стоит ли говорить, что с одним языком проще управлять командами? Удобней перекидывать людей, наполнять общую базу знаний. Вести библиотеки с контрибом.

На стыке языков всегда будут трения. Вечно будут задачи в духе "этот словарь мы не распарсим, наши убер-классы не позволят, переделай".

Команда не может Кложу? Научите их. Несколько дней мастер-классов. Подсадной человек, который поведет за собой начинающих. Заготовка проекта, который будут дорабатывать. Учиться не так уж сложно, если знать как обучать, и в перспективе выгодней.

Всячески сокращайте языковое многообразие.
