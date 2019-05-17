posts:
	/usr/local/bin/python scripts/migrate.py

serve:
	jekyll serve

docker-build:
	docker build -t blog .

blog-run:
	docker run -it --rm -p 4000:4000 -v $(CURDIR):/blog blog

blog-build:
	docker run -it --rm -p 4000:4000 -v $(CURDIR):/blog blog jekyll build

.PHONY: deploy
deploy: blog-build
	cd _site && git add -A && git commit -m "updated" && git push
