---
layout: page
title:  "Bookshelf"
permalink: /bookshelf/
---

На этой странице собираю прочитанные книги с короткими аннотациями.

{% for book in site.books reversed %}

<div class="book-item">
    <div class="book-thumb">
        <img src="/assets/static/bookshelf/{{ book.image }}">
    </div>
    <div class="book-text">
        <p>
            <b>{{ book.title }}</b>
            <br>
            <em>{{ book.author }}</em>
        </p>

        <p>
            {{ book.content }}
        </p>

    </div>
</div>

{% endfor %}

{% include comments.html permalink=page.permalink %}
