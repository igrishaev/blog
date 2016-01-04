---
layout: post
title:  "Edge Cases"
date:   2015-02-17 11:04:01
categories: post
---
Some edge cases and cautionary examples on using Markdown for writing content using this theme. In particular, list syntax can really knot things up.
<!--more-->

### Mathjax improperly parsing greater and less than and ampersands inside blocks

The mathjax plugin has been modified to contain all the block style mathjax inside a ```<div class="mathblock">..</div>``` tag wrapper pair
which fixes many of the issues with conflicts with the Kramdown parser. Some examples sent to me by Quxiaofeng are now parsing correctly, I believe.

This code:


However, a problem still exists for inline matrix notation, from an example [here](https://en.wikibooks.org/wiki/LaTeX/Mathematics#Matrices_in_running_text):

## Edge Case 1 from Quxiaofeng:

### No blank lines between Markdown list items

The issue arises when sidenotes and marginnotes are put into list items. For example:

### Related algorithms


+ Proximal point algorithm applied to the dual
+ Numerous applications in statistics and machine learning: lasso, gen. lasso, graphical lasso, (overlapping) group lasso, ...


Notice how the sidenotes display properly.

*In summary*: Take out any blank lines between your list items.

Okay, this is a really strange thing about the Jekyll Markdown engine I have never noticed before. If you have a list, and you put a blank line between the items like this:

```
    + list item 1

     + list item 2
```

It will create an html tag structure like this:

```
<ul>
   <li>
      <p>list item 1</p>
  </li>
  <li>
      <p>list item 2</p>
   </li>
</ul>
```
Which *totally* goofs up the layout CSS.

However, if your Markdown is this:

```
    + list item 1
     + list item 2
```

It will create a tag structure like this:

```
<ul>
   <li>list item 1</li>
   <li>list item 2</li>
</ul>
```

Here is the same content as above, with a blank line separating the list items. Notice how the sidenotes get squashed into the main content area:


### Remarks on ADMM version 2 - one blank line between Markdown list items

Related algorithms


### Liquid tag parsing strangeness

Example of the proper way to write an url inside a Liquid full-width image tag.
