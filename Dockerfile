FROM ubuntu:16.04

RUN apt-get update
RUN apt-get install -y jekyll bundler

RUN mkdir /blog
WORKDIR /blog
COPY Gemfile* ./
RUN bundle install
RUN rm Gemfile*

CMD jekyll serve --watch --limit_posts 5
