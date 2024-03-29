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
элементов: тега, атрибутов и содержимого.

Тег -- это короткое имя узла, например `name` или `description`. Атрибуты --
словарь свойств и их значений. Наиболее интересно содержимое: это набор строк
или других узлов. Вот как выглядит XML на псевдокоде:

~~~
XML = [Tag, Attrs, [String|XML]]
~~~

Чтобы убедиться в однородности XML, рассмотрим файл с товарами поставщиков:

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

На вершине XML находится узел `catalog`. Это группировочный тег: он необходим,
потому что на вершине не может быть несколько тегов. Потомки каталога —
организации. В атрибуте `name` организации указано её имя. Под организацией идут
товары — узлы с тегом `product` и атрибутом `type`. Вместо потомков товар
содержит текст -- подробное описание. Ниже него спуститься уже нельзя.

Clojure предлагает XML-парсер, который вернет структуру, похожую на схему `[Tag,
Attrs, Content]` выше. Каждый узел станет словарем с ключами `:tag`, `:attrs` и
`:content`. Последний хранит вектор, где элемент либо строка, либо вложенный
словарь.

Поместим XML с товарами в файл `resources/products.xml`. Напишем функцию, чтобы
считать файл в XML-зиппер. Добавьте модули `xml` и `io`:

~~~clojure
(:require
 [clojure.java.io :as io]
 [clojure.xml :as xml])
~~~

Оба входят в поставку Clojure и не требуют зависимостей. Чтобы получить зиппер,
пропустим параметр `path` через серию функций:

~~~clojure
(defn ->xml-zipper [path]
  (-> path
      io/resource
      io/file
      xml/parse
      zip/xml-zip))
~~~

Функция `xml/parse` вернёт структуру словарей с ключами `:tag`, `:attrs` и
`:content`. Обратите внимание, что текстовое содержимое, например, название
товара, это тоже вектор с одной строкой. Тем самым достигается однородность
каждого узла.

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

Вызов `(->xml-zipper "products.xml")` вернет первую локацию зиппера XML. Прежде
чем работать с ним, заглянем в определение `xml-zip`, чтобы понять, что
происходит. Приведём код из `clojure.zip` в сокращении:

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
означает: искать потомков в узлах, отличных от строки.

Рассмотрим, как бы мы нашли все товары из заданного XML. Для начала получим
ленивую итерацию по зипперу. Напомним, что на каждом шаге мы получим не словарь
с полями `:tag` и другими, а локацию с указателем на него. Останется
отфильтровать локации, чьи узлы содержат тег `product`. Для этого напишем
предикат:

~~~clojure
(defn loc-product? [loc]
  (-> loc zip/node :tag (= :product)))
~~~

и выборку с преобразованием:

~~~clojure
(->> "products.xml"
     ->xml-zipper
     iter-zip
     (filter loc-product?)
     (map loc->product))

;; ("iPhone 11 Pro" "iPhone SE" "iPad 3" "Macbook Pro")
~~~

На первый взгляд здесь ничего особенного. Структура XML известна заранее,
поэтому можно обойтись без зиппера. Для этого выберем потомков каталога и
получим организации; из потомков организаций получим товары. Вместе получится
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

Для краткости уберем переменные и сведем код к одной форме:

~~~clojure
(->> "products.xml"
     io/resource
     io/file
     xml/parse
     :content
     (mapcat :content)
     (mapcat :content))

;; ("iPhone 11 Pro" "iPhone SE" "iPad 3" "Macbook Pro")
~~~

На практике структура XML неоднородна. Предположим, крупный поставщик разбивает
товары по филиалам. В его случае XML выглядит так (фрагмент):

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

Код выше, где мы слепо выбираем данные по уровню, сработает неверно. В списке
товаров окажется филиал:

~~~clojure
("iPhone 11 Pro"
 "iPhone SE"
 {:tag :product, :attrs {:type "tablet"}, :content ["iPad 3"]} ...)
~~~

В то время как зиппер вернёт **только** товары, в том числе из филиала:

~~~clojure
(->> "products-branch.xml"
     ->xml-zipper
     iter-zip
     (filter loc-product?)
     (map loc->product))

("iPhone 11 Pro" "iPhone SE" "iPad 3" "Macbook Pro" "iPad 4" "Samsung A6+")
~~~

Очевидно, выгодно пользоваться кодом, который работает с обоими XML, а не
поддерживать отдельную версию для крупного поставщика. В противном случае нужно
где-то хранить признак и делать по нему `if/else`, что усложнит проект.

Однако и этот пример не раскрывает всю мощь зипперов. Для обхода XML служит
функция `xml-seq` из главного модуля Clojure. Она возвращает ленивую цепочку
XML-узлов в том же виде (словарь с ключами `:tag`, `:attr` и
`:content`). `Xml-seq` -- это частный случай более абстрактной функции
`tree-seq`. Последняя похожа на зиппер тем, что принимает функции `branch?` и
`children`, чтобы определить, подходит ли узел на роль ветки и как извлечь
потомков. Определение `xml-seq` напоминает `xml-zip`:

~~~clojure
(defn xml-seq
  [root]
  (tree-seq
    (complement string?)
    (comp seq :content)
    root))
~~~

Разница между зиппером и `tree-seq` в том, что при итерации зиппер возвращает
локацию — элемент, который несёт больше сведений. Кроме данных он содержит
ссылки на элементы по всем четырем направлениям. Наоборот, `tree-seq` итерирует
данные без обёрток. Для обычного поиска `tree-seq` даже предпочтительней,
поскольку не порождает лишних абстракций. Вот как выглядит сбор товаров с учётом
филиалов:

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

Чтобы показать мощь зипперов, подберём такую задачу, где возможностей `tree-seq`
не хватает. На эту роль подойдёт поиск с переходом между локациями.

(Продолжение следует)

{% include zipper-toc.md %}
