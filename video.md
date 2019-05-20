---
layout: page
title:  "Video"
permalink: /video/
---

<style>

    .video-list {
        width: 100%;
        display: flex;
        flex-direction: row;
        flex-wrap: wrap;
    }

    .video-item {
        width: 200px;
        margin-right: 15px;
    }

</style>

[my-channel]: https://www.youtube.com/channel/UCeg__Hf-lzDamDu_CY9ibHQ/videos
[deep-ref]: http://deeprefactoring.ru/speakers/ivan-grishaev

Some of my talks, streams and presentations.

<div class="video-list">

{% for post in site.posts %}
{% if post.youtube %}

<div class="video-item">
  <a href="{{ post.url }}">
    <img src="http://i3.ytimg.com/vi/{{ post.youtube }}/maxresdefault.jpg">
  </a>
  <p>
      <a href="{{ post.url }}">{{ post.title }}</a>
      <br>
      <small>{{ post.date | date_to_string }}</small>
  </p>
</div>

{% endif %}
{% endfor %}

</div>
