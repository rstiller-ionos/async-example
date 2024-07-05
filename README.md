# async IO example

1000+ tasks submitted and processed within less than 5s, while a single task can take up to 3 seconds.

## start test server

```
docker run --rm -it -v ./default.conf:/etc/nginx/conf.d/default.conf -p 8080:80 openresty/openresty:alpine
```

## start application

```
mvn clean install && java -cp target/async-0.0.1-SNAPSHOT.jar async.Main
```
