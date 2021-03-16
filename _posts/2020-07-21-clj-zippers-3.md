---
layout: post
title:  "Зипперы в Clojure (часть 3). XML-зипперы"
permalink: /clj-zippers-3/
tags: clojure zippers
---

{% include zipper-toc.md %}

Мощь зипперов раскрывается в полной мере при работе с XML. От других форматов он
отличается тем, что задан рекурсивно. Например, JSON, YAML и другие форматы
предлагают типы — числа, строки, коллекции, — у которых разный синтаксис и
структура. В XML, где бы мы ни находились, текущий узел состоит из трёх
компонентов: тега, атрибутов и содержимого. Содержимое -- это набор строк или
других узлов. Рекурсивная запись на псевдокоде:

~~~
XML = [Tag, Attrs, [String|XML]]
~~~

Чтобы убедиться в однородности XML, рассмотрим условный файл с товарами
поставщиков:

<!-- more -->

~~~xml
<?xml version="1.0" encoding="UTF-8"?>
<catalog>
  <organization name="re-Store">
    <product type="iphone">iPhone 11 Pro</product>
    <product type="iphone">iPhone SE</product>
  </organization>
  <organization name="DNS">
    <product type="tablet">iPad 3</product>
    <product type="notebook">Macbook Pro</product>
  </organization>
</catalog>
~~~

На вершине XML находится узел `catalog`. Это просто группировочный тег; он
неоходим, потому что на вершине не может быть несколько тегов. Потомки каталога
— организации. В атрибуте `name` организации указано её имя. Под организацией
следуют товары. Товар — это узел с тегом `product` и описанием типа
товара. Вместо потомков у него текстовое содержимое -- описание. Ниже товара
спуститься уже нельзя.

Clojure предлагает XML-парсер, который вернет структуру, похожую на схему `[Tag,
Attrs, Content]` выше. Каждый узел станет словарем с ключами `:tag`, `:attrs` и
`:content`. Последний хранит вектор, где элемент либо строка, либо вложенный
словарь.

Поместим XML-данные с товарами в файл `resources/products.xml`. Напишем функцию,
чтобы считать файл в XML-зиппер. Добавьте импорты модулей:

~~~clojure
(:require
 [clojure.java.io :as io]
 [clojure.xml :as xml])
~~~

Оба входят в поставку Clojure и поэтому не требуют зависимостей. Чтобы получить
зиппер, пропустим параметр `path` через серию функций:

~~~clojure
(defn ->xml-zipper [path]
  (-> path
      io/resource
      io/file
      xml/parse
      zip/xml-zip))
~~~

Функция `xml/parse` вернёт вложенную структуру из словарей с ключами `:tag`,
`:attrs` и `:content`. Обратите внимание, что текстовое содержимое, например,
название товара, это тоже вектор с одной строкой. Тем самым достигается
однородность каждого узла.

Вот что получим после вызова `xml/parse`:

~~~clojure
{:tag :catalog
 :attrs nil
 :content
 [{:tag :organization
   :attrs {:name "re-Store"}
   :content
   [{:tag :product
     :attrs {:type "iphone"}
     :content ["iPhone 11 Pro"]}
    {:tag :product :attrs {:type "iphone"} :content ["iPhone SE"]}]}
  {:tag :organization
   :attrs {:name "DNS"}
   :content
   [{:tag :product :attrs {:type "tablet"} :content ["iPad 3"]}
    {:tag :product
     :attrs {:type "notebook"}
     :content ["Macbook Pro"]}]}]}
~~~

Вызов `(->xml-zipper "products.xml")` порождает начальную локацию XML-зиппера из
данных выше. Прежде чем работать с ним, заглянем в определение `xml-zip`, чтобы
понять, что происходит. Приведём код из `clojure.zip` в сокращении:

~~~clojure
(defn xml-zip
  [root]
  (zipper (complement string?)
          (comp seq :content)
          ...
          root))
~~~

Очевидно, потомки узла — это его содержимое `:content`, дополнительно обёрнутое
в `seq`. У строки не может быть потомков, поэтому `(complement string?)`
означает: искать потомков только в тех узлах, что отличны от строки.

Рассмотрим, как бы мы нашли все товары из заданного XML. Для начала получим
ленивую итерацию по его зипперу. Напомним, что на каждом шаге мы получим не
словарь с полями `:tag` и другими, а zip-локацию с указателем на него. Останется
только отфильтровать локации, чьи ноды содержат тег `product`. Для этого напишем
предикат:

~~~clojure
(defn loc-product? [loc]
  (-> loc zip/node :tag (= :product)))
~~~

И выборку с преобразованием:

~~~clojure
(->> "products.xml"
     ->xml-zipper
     iter-zip
     (filter loc-product?)
     (map loc->product))

("iPhone 11 Pro" "iPhone SE" "iPad 3" "Macbook Pro")
~~~

На первый взгляд, здесь ничего особенного. Структура XML известна заранее,
поэтому можно обойтись без зиппера. Для этого выберем потомков каталога и
получим организации, а из них — тоже потомков (получим товары). Получится
простой код:

~~~clojure
(def xml-data
  (-> "products.xml"
      io/resource
      io/file
      xml/parse))

(def orgs
  (:content xml-data))

(def products
  (mapcat :content orgs))

(def product-names
  (mapcat :content products))
~~~

Для краткости можно убрать переменные и свести код к одной форме:

~~~clojure
(->> "products.xml"
     io/resource
     io/file
     xml/parse
     :content
     (mapcat :content)
     (mapcat :content))
~~~

На практике структура XML меняется. Предположим, особо крупный поставщик
разбивает товары по филиалам. В его случае XML выглядит так (фрагмент):

~~~xml
<organization name="DNS">
  <branch name="Office 1">
    <product type="tablet">iPad 3</product>
    <product type="notebook">Macbook Pro</product>
  </branch>
  <branch name="Office 2">
    <product type="tablet">iPad 4</product>
    <product type="phone">Samsung A6+</product>
  </branch>
</organization>
~~~

Код выше, где мы слепо выбираем данные по уровню, сработает неправильно. В
списке товаров окажется филиал:

~~~clojure
("iPhone 11 Pro"
 "iPhone SE"
 {:tag :product, :attrs {:type "tablet"}, :content ["iPad 3"]} ...)
~~~

В то время как зиппер вернет **только** товары, в том числе из филиала:

~~~clojure
(->> "products-branch.xml"
     ->xml-zipper
     iter-zip
     (filter loc-product?)
     (map loc->product))

("iPhone 11 Pro" "iPhone SE" "iPad 3" "Macbook Pro" "iPad 4" "Samsung A6+")
~~~

Очевидно, выгодно пользоваться кодом, который работает с двумя XML, а не
поддерживать отдельную версию для крупного поставщика. В противном случае нужно
где-то хранить признак, какой поставщик обычный, а какой крупный, и оперативно
обновлять его.

Однако и этот пример не раскрывает всю мощь зипперов. Для обхода XML также
служит функция `xml-seq` из корневого модуля Clojure. Она возвращает ленивую
цепочку XML-узлов в том же виде (словарь с `:tag`, `:attr` и
`:content`). `Xml-seq` -- это частный случай более абстрактной функции
`tree-seq`. Последняя похожа на зиппер тем, что принимает аналогичные функции,
чтобы определить, подходит ли узел на роль ветки и как извлечь из него
потомков. Как видно из кода, определение `xml-seq` близко к `xml-zip`:

~~~clojure
(defn xml-seq
  [root]
  (tree-seq
    (complement string?)
    (comp seq :content)
    root))
~~~

Разница между зиппером и `tree-seq` в том, что при итерации зиппер возвращает
локацию — более абстрактный элемент, который несет больше сведений. Наоборот,
`tree-seq` предлагает итерацию по чистым данным без оберток. Для обычного поиска
`tree-seq` даже предпочтительней, поскольку не порождает лишних абстракций. Вот
как выглядит сбор товаров с учетом филиалов:

~~~clojure
(defn node-product? [node]
  (some-> node :tag (= :product)))

(->> "products-branch.xml"
     io/resource
     io/file
     xml/parse
     xml-seq
     (filter node-product?)
     (mapcat :content))

("iPhone 11 Pro" "iPhone SE" "iPad 3" "Macbook Pro" "iPad 4" "Samsung A6+")
~~~

Чтобы вернуться к зипперам, подберем такую задачу, где `tree-seq` теряет
преимущество. На эту роль подойдет поиск с ручным передвижением.

(Продолжение следует)

{% include zipper-toc.md %}
