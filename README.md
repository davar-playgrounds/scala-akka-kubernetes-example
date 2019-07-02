# scala-akka-kubernetes-example
Example container written in Scala and Akka to be deployed into Kubernetes

## Akka and HTTP structure

### Actors

* ExampleActor - example actor to provide a couple of services to be invoked via GET and POST requests sent to the exposed HTTP endpoint
  * Events
    * EventIncrement
    * EventGet
    * EventGetResponse
    * EventCalculate
    * EventCalculateResponse
    
### HTTP endpoints

* GET
  * /get
* POST
  * /calculate

### Deploying container to Docker

To publish container image to local Docker runtime running in Docker Toolbox - use _docker:publishLocal_ in Sbt console 
and then verify in Docker that the image exists:
```
> docker:publishLocal
.
.
.
[success] Total time: 37 s, completed 02-Jul-2019 15:43:15
[IJ]sbt:scala-akka-kubernetes-example>

$ docker image ls
REPOSITORY                      TAG                                 IMAGE ID            CREATED             SIZE
scala-akka-kubernetes-example   0.3                                 b4a6931c3a43        10 minutes ago      526MB
```

To publish container image to Docker Hub first it needs to be tagged with the correct repository
```
$ docker tag c566afdc9319 krzsam/examples:scala-akka-kubernetes-example-0.3

$ docker image ls
REPOSITORY                      TAG                                 IMAGE ID            CREATED             SIZE
krzsam/examples                 scala-akka-kubernetes-example-0.3   b4a6931c3a43        11 minutes ago      526MB
scala-akka-kubernetes-example   0.3                                 b4a6931c3a43        11 minutes ago      526MB
```

Once the image has been tagged, it can now be pushed to main Docker HUb (it requires Docker to have credentials for the repository, as below).
The credentials provided need to be correct for the repository which the image was tagged with - for this example project: _krzsam/examples_ 

```
$ docker login
Login with your Docker ID to push and pull images from Docker Hub. If you don't have a Docker ID, head over to https://hub.docker.com to create one.
Username (krzsam):
Password:
Login Succeeded

$ docker push krzsam/examples:scala-akka-kubernetes-example-0.3
The push refers to repository [docker.io/krzsam/examples]
624d2b246a54: Pushed
58d1caf73492: Layer already exists
d00f1eab6209: Layer already exists
51566e3f832b: Layer already exists
51774d97c868: Layer already exists
ea20c4bf3aae: Layer already exists
2c8d31157b81: Layer already exists
7b76d801397d: Layer already exists
f32868cde90b: Layer already exists
0db06dff9d9a: Layer already exists
scala-akka-kubernetes-example-0.3: digest: sha256:3308e69679a0a757b7a350b1a87aa99ac754369527f8eb96fe6a6ca5194050dd size: 2422
```

Once the image is pushed, it can be seen on the repository page in the Docker Hub [here](https://cloud.docker.com/u/krzsam/repository/docker/krzsam/examples)

### Environment preparation

To run in Docker I used locally [Docker Toolbox](https://docs.docker.com/toolbox/overview/) run on Windows 10 Home.

```
docker-machine create node1
docker-machine create node2
docker-machine start node1
docker machine start node2
```

From this point there are 3 nodes running, but they are yet not connected to form a _swarm_ to be deployed into

```
$ docker-machine ls
NAME      ACTIVE   DRIVER       STATE     URL                         SWARM   DOCKER     ERRORS
default   *        virtualbox   Running   tcp://192.168.99.100:2376           v18.09.6
node1     -        virtualbox   Running   tcp://192.168.99.101:2376           v18.09.6
node2     -        virtualbox   Running   tcp://192.168.99.102:2376           v18.09.6

$ docker node ls
Error response from daemon: This node is not a swarm manager. Use "docker swarm init" or "docker swarm join" to connect this node to swarm and try again.
```

##### Creating swarm manager node

```
$ docker swarm init --advertise-addr 192.168.99.100
Swarm initialized: current node (lanctyx8jt0gx11o1i91glh6b) is now a manager.

To add a worker to this swarm, run the following command:

    docker swarm join --token SWMTKN-1-5revufn1bgdyczraenytolswe1u14p6cv66goouvgj0yll21dv-8ixdkmw0a59jl3ph99vcszcbi 192.168.99.100:2377

To add a manager to this swarm, run 'docker swarm join-token manager' and follow the instructions.

$ docker node ls
ID                            HOSTNAME            STATUS              AVAILABILITY        MANAGER STATUS      ENGINE VERSION
lanctyx8jt0gx11o1i91glh6b *   default             Ready               Active              Leader              18.09.6
```

##### Connecting nodes

```
$ docker-machine ssh node1
   ( '>')
  /) TC (\   Core is distributed with ABSOLUTELY NO WARRANTY.
 (/-_--_-\)           www.tinycorelinux.net

docker@node1:~$ docker swarm join --token SWMTKN-1-5revufn1bgdyczraenytolswe1u14p6cv66goouvgj0yll21dv-8ixdkmw0a59jl3ph99vcszcbi 192.168.99.100:2377

This node joined a swarm as a worker.

docker@node1:~$ exit
logout

$ docker node ls
ID                            HOSTNAME            STATUS              AVAILABILITY        MANAGER STATUS      ENGINE VERSION
lanctyx8jt0gx11o1i91glh6b *   default             Ready               Active              Leader              18.09.6
w8foz9d95cvqop7pjeh9ftcqy     node1               Ready               Active                                  18.09.6
```

```
$ docker-machine ssh node2
   ( '>')
  /) TC (\   Core is distributed with ABSOLUTELY NO WARRANTY.
 (/-_--_-\)           www.tinycorelinux.net

docker@node2:~$ docker swarm join --token SWMTKN-1-5revufn1bgdyczraenytolswe1u14p6cv66goouvgj0yll21dv-8ixdkmw0a59jl3ph99vcszcbi 192.168.99.100:2377

This node joined a swarm as a worker.

docker@node2:~$ exit
logout

$ docker node ls
ID                            HOSTNAME            STATUS              AVAILABILITY        MANAGER STATUS      ENGINE VERSION
lanctyx8jt0gx11o1i91glh6b *   default             Ready               Active              Leader              18.09.6
w8foz9d95cvqop7pjeh9ftcqy     node1               Ready               Active                                  18.09.6
jtbze07tnh623r585hyx51nqg     node2               Ready               Active                                  18.09.6
```

#### Running on single Docker node

## Running directly using Docker

If the container is running on Windows, _Ctrl-C_ will __not__ stop the container, and it needs to be stopped explicitly, otherwise any consecutive run
will fail as there will be already process bound to 9090 port. 

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

To run the container directly on one Docker node (each of the below 3 ways of invocation will produce the same results)

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

To verify that the application replies to requests, run the below in console window

* GET

The hostname, in the below example _F69ED712BE27_, is the same as the container id, and it will be different each time a new container is run for the same image.

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

####  Running _Stack_ in local Docker Swarm

Checking if any container is running on _Swarm_

```
$ docker node ls
ID                            HOSTNAME            STATUS              AVAILABILITY        MANAGER STATUS      ENGINE VERSION
lanctyx8jt0gx11o1i91glh6b *   default             Ready               Active              Leader              18.09.6
w8foz9d95cvqop7pjeh9ftcqy     node1               Ready               Active                                  18.09.6
jtbze07tnh623r585hyx51nqg     node2               Ready               Active                                  18.09.6

$ docker node ps
ID                  NAME                IMAGE               NODE                DESIRED STATE       CURRENT STATE       ERROR               PORTS
```

To run _Stack_ on Docker Swarm, you need to change current directory to the main project directory where _docker-compose.yaml_ file is located 
(inside Docker Toolbox shell)
You may need additional _--with-registry-auth_ parameter if the repository is marked as private in Docker Hub 
- otherwise you may get "no such image" rejected on nodes described [here](https://stackoverflow.com/questions/47470115/docker-stack-deploy-results-in-no-such-image-error)
You can also login to Docker Hub explicitly on each node via _docker-machine ssh ..._

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

As can be seen above, one instance of _krzsam/examples:scala-akka-kubernetes-example-0.3_ is running on each of _deafult_, _node1_ and _node2_ nodes

To test again if the application is replying to requests. As can be seen, the requests are load-balanced between all 3 nodes, and the hostnames 
represent container ids running on each of the nodes.

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

Also, the _docker-compose.yaml_ contains additional standard service called _visualizer_ which can be accessed via browser on _http://192.168.99.100:8080_ 
which provides graphical presentation of nodes.

To stop _Stack_

```
$ docker stack rm scala-akka-kubernetes-example-service
Removing service scala-akka-kubernetes-example-service_visualizer
Removing service scala-akka-kubernetes-example-service_web
Removing network scala-akka-kubernetes-example-service_webnet
```

## Running in Kubernetes

### Environment preparation

#### Running _Service_ 

## Other notes

* This example project provides only container for _amd64_ processor architecture - this can be run either directly on Linux, or on Windows via Docker Toolbox/Console.
  On any other processor architecture you will most likely see [this problem](https://forums.docker.com/t/standard-init-linux-go-190-exec-user-process-caused-exec-format-error/49368/5)  

## Links
* Docker
  * [Docker Toolbox](https://docs.docker.com/toolbox/overview/) 
  * [Docker Docs](https://docs.docker.com/get-started/)
  * [Docker Compose](https://docs.docker.com/compose/compose-file/)
  * [Docker repository with examples](https://cloud.docker.com/u/krzsam/repository/docker/krzsam/examples)
* Kubernetes
  * [K3S](https://k3s.io/)
  * [K3S Documentation](https://github.com/rancher/k3s/blob/master/README.md)
* Akka 
  * Actor, Streams: 2.5.23
  * [Http](https://doc.akka.io/docs/akka-http/current/introduction.html): 10.1.8
* [Sbt Native Packager](https://github.com/sbt/sbt-native-packager): 1.3.23
* Scala: 2.12.8
* Infrastructure (for K3S)
  * AWS, 3 nodes _t3a.xlarge_ (4 processors, 16GB memory): A1, A2, A3
  * For simplicity, all network traffic on all TCP and UDP ports is enabled in between A1, A2 and A3



