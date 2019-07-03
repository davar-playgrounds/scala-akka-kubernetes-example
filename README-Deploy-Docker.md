## Deploying application image to Docker

First, the application needs to be packaged and deployed to local Docker runtime as an image. For this purpose _sbt-native-packager_ Sbt plugin is used. 
To publish the image, invoke _docker:publishLocal_ command in Sbt console as below and the verify it exists: 

```
> docker:publishLocal
...
...
...
[success] Total time: 37 s, completed 02-Jul-2019 15:43:15
[IJ]sbt:scala-akka-kubernetes-example>

$ docker image ls
REPOSITORY                      TAG                                 IMAGE ID            CREATED             SIZE
scala-akka-kubernetes-example   0.3                                 b4a6931c3a43        10 minutes ago      526MB
```

To publish the image to Docker Hub, it needs to be tagged with the correct repository:

```
$ docker tag b4a6931c3a43 krzsam/examples:scala-akka-kubernetes-example-0.3

$ docker image ls
REPOSITORY                      TAG                                 IMAGE ID            CREATED             SIZE
krzsam/examples                 scala-akka-kubernetes-example-0.3   b4a6931c3a43        11 minutes ago      526MB
scala-akka-kubernetes-example   0.3                                 b4a6931c3a43        11 minutes ago      526MB
```

Once the image has been tagged, it can now be pushed to main Docker Hub - for this to work it requires Docker to have credentials for the repository, as below.
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

### Docker environment preparation

All examples below for running the image on Docker were run using [Docker Toolbox](https://docs.docker.com/toolbox/overview/) installed on Windows 10 Home.
In this environment, additionally to already existing _default_ node, two additional nodes were instantiated:

```
docker-machine create node1
docker-machine create node2
docker-machine start node1
docker machine start node2
```

At this point there are 3 Docker nodes running, but they are not yet connected with each other to form a cluster - in Docker terminology, a _Swarm_:

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

First, one of the nodes needs to be chosen as a _Swarm Manager_ node, and then the other two will connect to it as worker nodes. The _default_ node was chosen to be _Master_ node:

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

##### Connecting worker nodes to form the cluster

Once the _Master_ node is established, it is now possible to connect the other two nodes as _Worker_ nodes. First, connecting _node1_:

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

And then connecting _node2_:

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
