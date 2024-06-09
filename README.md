# AviaSlaves
The thing ended up using a dozen of libraries and technologies, let's just state that we use Kotlin, Maven, Ktor, Exposed, PostgreSQL, Apache Kafka, Traefik.

App's architecture is not straightforward too, but to sum up I would say that we have 2 independent microservices, 2 independent databases, 1 load balancer and 1 kafka instance (with zookeeper) for communication.

To run the application, please use:
```shell
docker-compose up -d
```
It might take a while. I really do not recommend to run in attached mode due to overflowing number of Kafka logs.

If you get warned with something about keys, make sure to run `rotate.sh` in `Auth/src/main/resources/keys`.

Make sure that you have ports 80 and 8080 open. If not, edit Traefik ports in docker-compose file.

Use this Postman collection to entertain yourself:

[<img src="https://run.pstmn.io/button.svg" alt="Run In Postman" style="width: 128px; height: 32px;">](https://www.postman.com/Ambassador4ik/workspace/aviaslaves/collection/26267076-dfb6f4c7-c0cc-4e56-a99b-f30ebeb0de7c)