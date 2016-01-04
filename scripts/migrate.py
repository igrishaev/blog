#coding=utf-8

import os
from datetime import datetime
from collections import defaultdict

import MySQLdb
from jinja2 import Template

db = MySQLdb.connect(
    host='localhost',
    user='root',
    # passwd='dunno',
    db='blog'
)

template = Template(u'''---
layout: post
title:  "{{ title }}"
date:   "{{ datetime }}"
permalink: {{ url }}
categories: {{ tags|join(' ') }}
---

{{ content }}


{% if comments %}
#### Комментарии из старого блога
{% for comment in comments %}

**{{ comment.Stamp.strftime('%x') }} {{ comment.AuthorName }}:** {{ comment.Text }}

{% if comment.Reply%}
**{{ comment.ReplyStamp.strftime('%x') }} Иван Гришаев:** {{ comment.Reply }}
{% endif %}

{% endfor %}
{% endif %}
''')

URLS = defaultdict(int)

def create_post(note):
    (
        id,
        title,
        url_name,
        text,
        formatter_id,
        is_published,
        is_issue,
        is_commentable,
        is_visible,
        is_favourite,
        stamp,
        last_modified,
        offset,
        is_dst,
        ip,
    ) = note

    if not is_published:
        return

    # import ipdb; ipdb.set_trace()
    created_at = datetime.utcfromtimestamp(stamp)

    # form url
    base_url = created_at.strftime('%Y/%m/%d')
    URLS[base_url] += 1
    slug = URLS[base_url]
    url = '/%s/%s/' % (base_url, slug)

    # form tags
    cur = db.cursor()
    cur.execute('''
select * from __0Db_NotesKeywords nk
inner join __0Db_Keywords as k
on nk.KeywordID = k.ID
where nk.NoteID = %s;
    ''', (id, ))
    tags = [row[5].decode('utf-8') for row in cur.fetchall()]

    # form comments
    cur.execute('select * from __0Db_Comments where NoteID = %s', (id, ))
    comments = []
    for row in cur.fetchall():
        (
            ID, NoteID, AuthorName, AuthorEmail, Text, Reply,
            IsVisible, IsFavourite, IsReplyVisible, IsReplyFavourite,
            IsAnswerAware, IsSubscriber, IsSpamSuspect, IsNew, Stamp,
            LastModified, ReplyStamp, ReplyLastModified, IP,
        ) = row
        if not IsVisible:
            continue
        comments.append({
            'AuthorName': AuthorName.decode('utf-8'),
            'AuthorEmail': AuthorEmail.decode('utf-8'),
            'Stamp': datetime.utcfromtimestamp(Stamp),
            'ReplyStamp': datetime.utcfromtimestamp(ReplyStamp),
            'Text': Text.decode('utf-8'),
            'Reply': Reply.decode('utf-8'),
        })
        comments.sort(key=lambda item: item['Stamp'], reverse=False)

    # render and save
    context = {
        'title': title.replace('"', '').decode('utf-8'),
        'datetime': created_at.strftime('%Y-%m-%d %H:%M:%S'),
        'url': url,
        'tags': tags,
        'content': text.decode('utf-8'),
        'comments': comments,
    }
    content = template.render(context)
    filename = '%s-%s.md' % (
        created_at.strftime('%Y-%m-%d'),
        # title.decode('utf-8').encode('trans'),
        id,
    )
    filepath = os.path.join(
        os.path.dirname(__file__),
        os.pardir,
        '_posts',
        filename
    )
    with open(filepath, 'w') as f:
        f.write(content.encode('utf-8').replace('\r\n', '\n'))
    print filepath

cur = db.cursor()

cur.execute('select * from __0Db_Notes order by stamp;')

for row in cur.fetchall():
    create_post(row)

db.close()
