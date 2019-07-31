
all: build deploy

git-prepare:
	git clone -b gh-pages --single-branch git@github.com:igrishaev/interview.git gh-pages

docker-build:
	docker build -t blog .

DOCKER_BASE=docker run -it --rm -p 4000:4000 -v $(CURDIR):/blog blog

run:
	$(DOCKER_BASE)

build:
	$(DOCKER_BASE) jekyll build

serve:
	$(DOCKER_BASE) jekyll serve --watch --incremental


.PHONY: deploy
deploy:
	cp -r _site/* gh-pages
	cd gh-pages && git add -A && git commit -m "updated" && git push

static:
	open "http://127.0.0.1:8000"
	cd _site && python -m SimpleHTTPServer 8000
