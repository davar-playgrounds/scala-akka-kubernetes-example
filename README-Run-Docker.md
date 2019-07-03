## Running image on Docker

#### Running on single Docker node

To run the image directly on one Docker node (each of the below 3 ways of invocation will produce the same results):

```
$ docker image ls
REPOSITORY                      TAG                                 IMAGE ID            CREATED             SIZE
scala-akka-kubernetes-example   0.3                                 b4a6931c3a43        18 minutes ago      526MB
krzsam/examples                 scala-akka-kubernetes-example-0.3   b4a6931c3a43        18 minutes ago      526MB

$ docker run -p 9090:9090 b4a6931c3a43
...

$ docker run -p 9090:9090 scala-akka-kubernetes-example:0.3
...

$ docker run -p 9090:9090 scala-akka-kubernetes-example:0.3
2019-07-02 18:13:24 INFO  Main$ - Starting up Akka Docker container ...
[INFO] [07/02/2019 18:13:28.806] [DockerSystem-akka.actor.default-dispatcher-4] [akka://DockerSystem/user/ExampleActor] Count is now 0
2019-07-02 18:13:29 INFO  Main$ - Binding HTTP service to F69ED712BE27:9090 on interface '0.0.0.0'
[INFO] [07/02/2019 18:13:31.772] [DockerSystem-akka.actor.default-dispatcher-4] [akka://DockerSystem/user/ExampleActor] Count is now 1
[INFO] [07/02/2019 18:13:34.786] [DockerSystem-akka.actor.default-dispatcher-4] [akka://DockerSystem/user/ExampleActor] Count is now 2
[INFO] [07/02/2019 18:13:37.799] [DockerSystem-akka.actor.default-dispatcher-2] [akka://DockerSystem/user/ExampleActor] Count is now 3
[INFO] [07/02/2019 18:13:39.694] [DockerSystem-akka.actor.default-dispatcher-6] [akka://DockerSystem/user/ExampleActor] Returning count: 3 from F69ED712BE27
[INFO] [07/02/2019 18:13:40.822] [DockerSystem-akka.actor.default-dispatcher-6] [akka://DockerSystem/user/ExampleActor] Count is now 4
...
...
```

To verify that the application replies to requests, run the below in console window. 
The hostname in the below example _F69ED712BE27_ is the same as the _Container ID_, and it will be different each time a new container is run for the same image.

* GET

```
> docker-machine ip
192.168.99.100

> curl 192.168.99.100:9090/get
{ "count": 394 , "hostname": "F69ED712BE27"  }

> curl 192.168.99.100:9090/get?id=7
{ "count": 396 , "hostname": "F69ED712BE27"  , "id": 7  }
```

* POST

```
> curl -d " [ { \"id\": 1, \"param_1\": 3, \"param_2\": 4 } , { \"id\": 2, \"param_1\": 4, \"param_2\": 5 } ]  " -H "Content-Type: application/json" -X POST 192.168.99.100:9090/calculate
[{"id":1,"result":12},{"id":2,"result":20}]
```

If the container is run on Windows, _Ctrl-C_ will __not__ stop it, and it needs to be stopped explicitly, otherwise any consecutive run
will fail as there will be already a process bound to 9090 port. 

```
$ docker container ls
CONTAINER ID        IMAGE                               COMMAND                  CREATED             STATUS              PORTS                    NAMES
f69ed712be27        scala-akka-kubernetes-example:0.3   "/opt/docker/bin/sca…"   19 minutes ago      Up 19 minutes       0.0.0.0:9090->9090/tcp   festive_hamilton

$ docker container stop f69ed712be27
f69ed712be27

docker container ls
$ docker container ls
CONTAINER ID        IMAGE               COMMAND             CREATED             STATUS              PORTS               NAMES
```

####  Running _Stack_ Docker Swarm

To run _Stack_ on Docker Swarm you need to navigate (inside Docker Toolbox shell) to the main project directory where _docker-compose.yaml_ file is located.
Additional parameter _--with-registry-auth_ may be required to propagate Docker Hub credentials to other nodes in case the repository used is marked as private in Docker Hub,
so the application image can be pulled on other cluster nodes (provided as example here, this is not the case for this application). 
If the above parameter is not used but the repository is private, the issue may show up as "no such image" and rejected on nodes, as described 
[here](https://stackoverflow.com/questions/47470115/docker-stack-deploy-results-in-no-such-image-error). To rectify, use the above parameter or login in to Docker Hub explicitly on each node via _docker-machine ssh ..._

```
cd <Project Directory>

$ docker stack deploy -c docker-compose.yaml scala-akka-kubernetes-example-service
...

$ docker stack deploy --with-registry-auth -c docker-compose.yaml scala-akka-kubernetes-example-service
Creating network scala-akka-kubernetes-example-service_webnet
Creating service scala-akka-kubernetes-example-service_visualizer
Creating service scala-akka-kubernetes-example-service_web

$ docker stack ls
NAME                                    SERVICES
scala-akka-kubernetes-example-service   2

$ docker stack ps scala-akka-kubernetes-example-service
ID                  NAME                                                 IMAGE                                               NODE                DESIRED STATE       CURRENT STATE           ERROR               PORTS
u0mr8ekdhq2i        scala-akka-kubernetes-example-service_web.1          krzsam/examples:scala-akka-kubernetes-example-0.3   node2               Running             Running 3 minutes ago
td6rnvapf3kg        scala-akka-kubernetes-example-service_visualizer.1   dockersamples/visualizer:stable                     default             Running             Running 3 minutes ago
wcc1pms8er40        scala-akka-kubernetes-example-service_web.2          krzsam/examples:scala-akka-kubernetes-example-0.3   node1               Running             Running 3 minutes ago
tz68ox5ize32        scala-akka-kubernetes-example-service_web.3          krzsam/examples:scala-akka-kubernetes-example-0.3   default             Running             Running 5 minutes ago
```

As can be seen above, one instance of _krzsam/examples:scala-akka-kubernetes-example-0.3_ is running on each of _deafult_, _node1_ and _node2_ nodes.

The container is configured in such way that the requests sent to each of the nodes are load balanced in Round Robin fashion across all the nodes of the cluster:

* GET
```
> curl 192.168.99.100:9090/get
{ "count": 113 , "hostname": "8358C78C790B"  }
> curl 192.168.99.100:9090/get
{ "count": 113 , "hostname": "C0E84F8200B2"  }
> curl 192.168.99.100:9090/get
{ "count": 155 , "hostname": "B2A197111291"  }
> curl 192.168.99.100:9090/get
{ "count": 114 , "hostname": "8358C78C790B"  }
> curl 192.168.99.100:9090/get
{ "count": 115 , "hostname": "C0E84F8200B2"  }
> curl 192.168.99.100:9090/get
{ "count": 156 , "hostname": "B2A197111291"  }
```

* POST

```
> curl -d " [ { \"id\": 1, \"param_1\": 3, \"param_2\": 4 } , { \"id\": 2, \"param_1\": 4, \"param_2\": 5 } ]  " -H "Content-Type: application/json" -X POST 192.168.99.100:9090/calculate
[{"id":1,"result":12},{"id":2,"result":20}]
```

Additionally, _docker-compose.yaml_ contains standard service called _visualizer_ which can be accessed via browser on _http://192.168.99.100:8080_ . 
The services provides a simple graphical presentation of nodes and containers.

The below command stops the deployed _Stack_:

```
$ docker stack rm scala-akka-kubernetes-example-service
Removing service scala-akka-kubernetes-example-service_visualizer
Removing service scala-akka-kubernetes-example-service_web
Removing network scala-akka-kubernetes-example-service_webnet
```
