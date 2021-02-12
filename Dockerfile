FROM ubuntu:20.04

RUN apt-get update
RUN apt-get install -y bundler

RUN mkdir /blog
WORKDIR /blog
COPY Gemfile* ./
RUN bundle install
RUN rm Gemfile*

CMD jekyll serve --watch
