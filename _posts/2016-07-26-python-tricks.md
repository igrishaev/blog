---
layout: post
title:  "Питонячьи хитрости"
permalink: /python-tricks/
tags: python tricks
---

Расскажу о всяких хитростях Питона, с которыми сталкивался в работе. Долгое
время записывал их на бумажку, а теперь решил скомпоновать. Думаю, будет
любопытно опытным питонщикам. Сказанное ниже носит познавательный характер. Не
призываю использовать в проде.

### Многоточие

Неожиданно, при определенных условиях многоточие `...` становится правильной
лексемой! Внутри оператора `[]` многоточие вырождается в объект `Ellipsis`:

~~~ python
>>> d = {(1, Ellipsis, 2): 42}
>>> d[1, ..., 2]
42
~~~

Если переопределить метод `__getitem__` и проверять аргументы на многоточие,
можно добиться интересных результатов. Так работает библиотека
[hask](https://github.com/billpmurphy/hask). В ней с помощью многоточия
определяются бесконечные списки:

~~~ python
# бесконечный ленивый список от единицы
List[1, ...]

# ленивый список от 1 до 100
List[1, ..., 100]

# аналогично, но с шагом в 2
List[1, 3, ...]
~~~

В другом месте многоточие вызовет ошибку синтаксиса.

### Срезы в аргументах

В оператор индекса `[]` можно передать синтаксис словаря: `key: value,
...`. Тогда в методе `__getitem__` мы получим кортеж объектов `slice`. Ключ и
значение хранятся в полях `.start` и `.stop`. Выглядит так:

~~~ python
    def __getitem__(cls, slices):

        if isinstance(slices, tuple):
            slice_tuple = slices
        else:
            slice_tuple = (slices, )

        keys = (sl.start for sl in slice_tuple)
        vals = (sl.stop for sl in slice_tuple)

        return dict(zip(keys, vals))

YourClass["foo": 42, "bar": "test"]
>>> {"foo": 42, "bar": "test"}
~~~

### Деструктивный синтаксис

Внезапно, деструктивный синтаксис, о котором я [уже писал](/destructuring),
выпилен в тройке: не проходит проверку синтаксиса. Печаль моя не знает границ.

Это объективный минус третьего Питона. Зачем лишать разработчиков удобной
возможности? Я постоянно работаю с парами. Синтаксиса `def action((key, val)):`
теперь будет не хватать.

### Погружение в словарь

Предположим, есть вложенные словари, ответ какого-то микросервиса. Нужно взять
поле из глубины. Структура ответа меняется. Часто разработчики пишут следующее:

~~~ python
data.get('result', {}).get('user', {}).get('name', 'unknown')
~~~

Минусы в том, что код плохо читается и созданы 2 лишних словаря. Аргументы
вычисляются до шага в функцию, поэтому словари будут созданы даже когда ключи
есть.

Неочевидный минус, из-за которого придется чинить прод в пятницу, кроется в
методе `.get`. Он возвращает дефолт только если ключа нет. А если ключ есть и
равен `None`, то вернется `None` вне зависимости от того, что передано в дефолт.

~~~ python
data = {"result": {"user": None}}
data.get('result', {}).get('user', {}).get('name', 'unknown')
>>> AttributeError: 'NoneType' object has no attribute 'get'
~~~

В [библиотеке f](https://github.com/igrishaev/f) я предложил нормальный способ
работы со вложенными словарями:

~~~ python
f.ichain(data, 'result', 'user', 'name')
>>> None
~~~

### Конечное приведение типов

Приводить типы в конце каждой операции -- здравая мысль. Идея в том, чтобы
добавлять к концу вычислений кусочек `... or <default>`, например:

~~~ python
get_users_count() or 0
>>> 0

get_account_list(id) or []
>>> []

f.ichain(data, 'result', 'user', 'name') or 'dunno'
>>> dunno
~~~

Этот прием спасает злополучного `None`, который лезет изо всех щелей. Беда в
том, что в скриптовых языках нет четких правил для пустых значений. Например,
функция может отдать или пустой список, или `None`. Запомнить невозможно. Рано
или поздно `None` провалится туда, где ожидают список или число.

Решение -- выводить типы самостоятельно. Поскольку оператор `or` ленив во всех
языках, выражение справа не будет вычислено без необходимости. Следующий код:

~~~ python
data = {"user": None}
(data.get("user") or {}).get("name") or "dunno"
>>> "dunno"
~~~

вернет ожидаемый дефолт, даже когда ключ есть и равен `None`. При этом если в
юзере лежат актуальные данные, пустой словарь не будет создан.

### Правые методы

Еще одна особенность классов в Питоне -- правые волшебные методы. Они начинаются
с префикса `r`:` __radd__`, `__rsub__` и т.д. Как следует из названия, эти
методы вызываются для правого операнда в тех случаях, когда для левого операнда
метод не определен.

Другими словами, рассмотрим выражение `a + b`. Сперва Питон попытается сделать
так: `a.__add__(b)`. Если для `a` метод `__add__` неопределен, будет предпринята
другая попытка: `b.__radd__(a)`. И если `__radd__` тоже не определен для `b`,
вылезет ошибка.

Правые методы появились в третьей версии Питона. Попробуем реализовать следующее
поведение: к списку добавляем число. Если прибавляем слева, число становится в
начало списка. Если справа -- в конец.

~~~ python
class List(list):

    def __add__(self, other):
        if isinstance(other, list):
            return super(List, self).__add__(other)
        else:
            return self + [other]

    def __radd__(self, other):
        if isinstance(other, list):
            return other.__add__(self)
        else:
            return [other].__add__(self)

>>> 99 + List([1, 2, 3])
[99, 1, 2, 3]

>>> List([1, 2, 3]) + 99
[1, 2, 3, 99]
~~~

### Особенности среза в тройке

Во втором Питоне у классов был особый метод `__getslice__` для получения
среза. В тройке его объединили с `__getitem__`. Теперь интерпретатор проверяет,
что передано в `__getitem__`. Если написать `foo[1:2]`, будет передан экземпляр
класса `slice`, а если `foo[1, 2]` -- кортеж.

Этот пункт я привожу к тому, что переопределив метод `__getitem__`, мы по запаре
можем сломать операцию среза. Приходится добавлять условие: если передан срез --
вызывать предка, если нет -- своя логика:

~~~ python
    def __getitem__(self, item):
        if isinstance(item, slice):
            return super(MyClass, self).__getitem__(item)
        else:
            ...
~~~