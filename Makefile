
git-prepare:
	git clone -b gh-pages --single-branch git@github.com:igrishaev/interview.git gh-pages

docker-build:
	docker build -t blog .

blog-run:
	docker run -it --rm -p 4000:4000 -v $(CURDIR):/blog blog

blog-build:
	docker run -it --rm -p 4000:4000 -v $(CURDIR):/blog blog jekyll build

.PHONY: deploy
deploy: blog-build
	cp -r _site/* gh-pages
	cd _site && git add -A && git commit -m "updated" && git push
