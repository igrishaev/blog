---
layout: post
title: "Зачем OpenAPI?"
permalink: /why-openapi/
tags: programming openapi web rpc
---

Чего не могу понять, так это одержимость OpenAPI. Казалось бы, нужна апишка на
сайте — ну, сделай как тебе удобно. Но люди берут OpenAPI, крафтят спеку,
генерят по ней контроллеры, схемы, тесты. Превозмогают, потеют и потом
рассказывают: смотрите, наша апишка по стандарту OpenAPI.

А кого волнует этот ваш OpenAPI? Расстрою: никому не интересно, какая у вас
апишка. Пользователю все равно, что гоняетя под капотом. Для программистов на
Питоне, как правило, пишут клиентские библиотеки. Вызывая метод
`client.get_user(id=42)`, программист в гробу видал, что там у вас — `GET`,
`POST`, джейсон или XML. Никто на это не смотрит.

Если точнее, на это смотрят только кложуристы, потому что для них клиентских
библиотек никто не пишет. Но кого интересуют проблемы кложуристов? Они сами
напишут клиент поверх чего нужно.

За много лет я не припомню, чтобы от OpenAPI была какая-то польза. А вот проблем
— целый мешок. Это стандарт, которому нужно следовать; это определенные
инструменты, которые навязывают игру. Инструмент X написан на Руби, ставь его и
миллион пакетов. Инструмент Y написан на Ноде, ставь ее тоже и качай половину
npm. Я неделю настраивал swagger в докере, чтобы он показывал веб-страничку со
спекой. Команда привязала гирю к ноге и удивляется: почему разработка идет так
медленно?

Когда мне нужна апишка, я делаю простой RPC: команда-параметры,
команда-параметры. Все в теле запроса, а не как в REST, где один параметр в
заголовке, второй в адресной строке, третий черт знает где. В теле гоняю либо
JSON, либо message pack в зависимости от content type.

Это просто, это быстро, это прозрачно. В коде большая мапа вида

~~~
{action {doc ...
         schema-in
         schema-out
         handler ...}}
~~~

По текущей команде я вынимаю схему, проверяю вход, вызываю функцию `handler` с
параметрами. Если дебаг, то проверяю выходные данные. Один раз настроил этот
словарь и потом только наращиваешь.

Если нужна документация, пишется код, который пробегает по словарю и рендерит
markdown-файл. В нем список команд, описание из поля `doc` и схемы
ввода-вывода. Если нужно, md-файлик рендерится в HTML или PDF.

Но серьезным людям этого не понять. Им нужна OpenAPI-спека, чтобы что-то
генерить и чему-то соответсвовать. Пишутся запредельные объемы тулинга под
OpenAPI. Бывает, в Кложу приходит бывший рубист и заводит песню: мол, в моих
Рельсах есть библиотека, которая по спеке сгенерит контроллер и модели, напишет
тесты, а у вас в Кложе ничего нет... блин, потому я и довольный, что нет.

На самом деле я был разок в проекте на Кложе, где по OpenAPI-спеке генерили
код. Два слова: это ужасно. Ни при каких обстоятельствах не сяду за это
снова. Генерация — это стремно, это хрупко, это километровые диффы. Духота,
трение и тошнота.

И никому не прихоит в голову спросить — зачем? Какую проблему ты решаешь своим
OpenAPI? Зачем соответствовать чужому стандарту, который не контролируешь? Чтобы
что?