posts:
	/usr/local/bin/python scripts/migrate.py

serve:
	jekyll serve

docker-build:
	docker build -t blog .

docker-run:
	docker run -it --rm -p 4000:4000 -v $(CURDIR):/blog blog
