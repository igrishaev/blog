---
layout: post
title:  "Бесчеловечность Java"
permalink: /java-inhumanity/
tags: java programming
---

Джавное ООП отличает особая бесчеловечность. Ощущение, что код писали не
люди, а роботы, которые хотят обратить кожаных мешков в рабство. Именно такие
ассоциации приходят в голову.

Пример — джавный SDK для S3. Возникла задача — явно указать пару ключей и
регион. Как это делается в дружелюбных языках? Передается мапка с тремя
ключами или kwargs. По умолчанию берутся переменные среды, но считается, что
пользователь не идиот и в случае нужды укажет свои ключи.

Джавный SDK, напротив, считает пользователя идиотом. Креды берутся из
переменных среды и системных файлов. Если хочешь указать свои, то нужно
написать такой код:

{% include static.html path="java-inhumanity/1.jpeg" %}

Я рожал его почти два часа. Понимаете ли, инициировать `BasicAWSCredentials`
недостаточно. Нужно обернуть его в `AWSStaticCredentialsProvider` и передать в
`AmazonS3ClientBuilder`. Так очевидно и просто!

Ясное дело, в джавадоке об этом ни слова. Каждый класс подробно знает о своих
геттерах и сеттарах, но не то, как их скомпоновать. Гугление выдает ссылки на
StackOverflow и Baeldung. Все спонтанно, что-то уже deprecated, разброд и
шатание.  В официальных доках только те примеры, что считаются правильными со
стороны AWS.

Все это топорно и бездушно. А некоторым с этим жить.
