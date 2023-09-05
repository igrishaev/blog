---
layout: post
title:  "Запуск InDesign"
permalink: /indesign-startup/
tags: video adobe indesign
---

Готовлю книжку к печати и поэтому вынужден иметь дело с Адобом. Избежать этого
шага невозможно — некоторые программы заменить нечем. Несмотря на то, что в
адобовских программах я работаю буквально 20 лет, поминутно матерюсь из-за их
косяков. Покажу серьезный прокол, который заслуживает отдельного поста.

Перед вами видео, где я запускаю Индизайн. Обратите внимание, что приложение
стартует быстро: буквально пару секунд занимает сплеш-скрин, а дальше
распахивается окно. Это, кстати, не заслуга Адоба: просто в процессе записи я
несколько раз открывал и закрывал его, и поэтому оно прогрето. Холодный запуск
будет медленней.

<video controls>
  <source src="/assets/static/aws/indesign/1.mp4" type="video/mp4">
</video>

Но дело не в этом. После того, как окно распахнется на весь экран, программа
вступает во вторую фазу: загружает внутренний экран с последними файлами и
рекламой новых фич. Эта загрузка длится десять
секунд. **Целых. Десять. Секунд.** Пока он не просрется, не видно последних
файлов и кнопок создания нового документа.

В итоге сидишь и тупишь в пустоту, пока что-то там не загрузится. Однако если
нажать Ctrl+O, откроется диалог выбора файла. Выходит, программа прекрасно
работает, даже если главный экран не загрузился! Если выбрать в диалоге файл с
проектом, он откроется мгновенно. Можно не ждать эти 10 секунд, а открыть файл
ручками и работать. Это подтверждает второе видео:

<video width="1280" height="720" controls>
  <source src="/assets/static/aws/indesign/2.mp4" type="video/mp4">
</video>

Такие вот интересные программы Адоба.

Чем-то это напоминает современные веб-приложения: программа загрузилась, но это
присказка, не сказка. Мы только загрузили скрипты, далее они выгребут свое
дерьмо с серверов и только затем покажут приложение. На телевизорах такое
постоянно: первый прогрес-бар показывает загрузку приложения на уровне ОС, а
приложение запускает второй прогресс-бар для загрузки своего барахла.

Печально, что эту ерунду нельзя объединить в один шаг, потому что как
пользователю мне эти фазы до одного места: один хрен программа не
работает. Вдвойне печальней, что это зараза переходит на десктоп.

Подозреваю, что упомянутый главный экран — ничто иное, как Хром + Node.js +
React. Это видно по косвенным признакам: он похож на Слак и прочие поделки,
которые не могут быть отрисованы частично, а только целиком. Возможно, скрипты
ломятся в сеть, но блокируются из-за санкций или бог знает чего. Ожидание в 10
секунд похоже на таймаут, заданный при отправке HTTP-запроса. Спасибо, что хотя
бы выставили его: по умолчанию он равен 30 секунд.

Странно, что последние файлы отображаются внутри браузера, хотя никак не связаны
с ним. Это банальная глупость, деградация разработки. Надо сказать, я не
удивлен. В Адобе много что делают на Node.js, например дашборд, из которого
ставятся приложения. Я как-то шарился по папкам и нашел ворох скриптов. Плюс
часто выскакивает диалог с текстом "приложение node хочет доступ к такому-то
сертификату."

В сотый раз повторю тезис: сапожник без сапог. Фирма, которая тридцать лет пишет
настольные программы под Винду и Мак, не может нормально показать последние
файлы. Для загрузки приложений делается поделка па Хроме и Js, глючная и
тормозная. Деградация софта в угоду менеджменту: сделать тяп-ляп, но зато быстро
и получить повышение.

Хочется верить, что индустрия печати пересядет с иглы Адоба на что-то другое, но
пока что просвета не видно.