---
layout: post
title:  "Ввод по маске"
permalink: /masked-input/
tags: ui frontend jquery
---

Каждый мамкин фронтендер знает: если в форме есть поле телефона, ставим плагин
jQuery под названием Masked Input и не паримся. С ним нельзя ввести буквы,
скобки, дефисы и прочий мусор.

{% include static.html path="masked-input/1.jpeg" %}

Но мамкин фронтендер не знает: если вставить в поле "89623289677" (скажем, из
Экселя или менеджера паролей), то всратый плагин поместит восьмерку после +7 и
отбросит последнюю цифру. Результат будет как на второй картинке.

{% include static.html path="masked-input/2.jpeg" %}

То есть всего-то испортили данные, но никого это не волнует. Подумаешь, не
придет смс, не дозвонится курьер, не выдадут посылку. Фронтендеру не до этих
мелочей.

Недаром говорят: благие намерения ведут в ад. Хотели сделать удобно на фронте, в
итоге добавили баг.
