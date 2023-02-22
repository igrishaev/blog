
PORT := 4001

all: clear build deploy

git-prepare:
	git clone -b gh-pages --single-branch git@github.com:igrishaev/interview.git gh-pages

docker-build:
	docker build --no-cache -t blog .

DOCKER_BASE=docker run -it --rm -p $(PORT):$(PORT) -v $(CURDIR):/blog blog

run:
	$(DOCKER_BASE)

build:
	$(DOCKER_BASE) jekyll build

serve:
	$(DOCKER_BASE) jekyll serve --watch --incremental


.PHONY: deploy
deploy:
	cd gh-pages && git pull
	cp -r _site/* gh-pages
	cd gh-pages && git add -A && git commit -m "updated" && git push

static:
	open "http://127.0.0.1:8000"
	cd _site && python -m SimpleHTTPServer 8000

.PHONY: clear
clear:
	rm -rf _site

gh-pages-init:
	git clone --branch gh-pages git@github.com:igrishaev/blog.git gh-pages


aws-upload:
	aws s3 sync aws s3://igrishaev.public --acl public-read --exclude '*.DS_Store'

aws-download:
	aws s3 sync s3://igrishaev.public aws

aws-rm:
	aws s3 rm s3://igrishaev.public/foo --recursive

grep-github:
	grep --no-filename -r --include="*.md" -o -E -i '(https://user-images.githubusercontent.com.+?(\.\w+))' . > urls.txt

wget-github:
	wget -i urls.txt -x

# !\[(.*)\]\((https://user-images.githubusercontent.com/(.*?))\)
# {% include static.html path="$3" title="$1" %}


slug ?= $(error Please specify the slug=... argument)
post_date = $(shell date +'%Y-%m-%d')

new-post:
	touch _posts/${post_date}-${slug}.md
