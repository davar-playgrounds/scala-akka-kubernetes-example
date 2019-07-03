# scala-akka-kubernetes-example
Example application in Scala and Akka which is deployed as a image to Docker Hub and can be run on Docker Swarm or Kubernetes (K3S) platforms.

## Internal application structure

#### Akka Actors

* ExampleActor - example actor to provide a couple of simple services to be invoked via GET and POST requests sent to the exposed HTTP endpoint
  * Events
    * EventIncrement - event which the actor sends to itself periodically to increment an internal counter
    * EventGet - message to query the internal counter of the actor
    * EventGetResponse - response containing current value of the counter
    * EventCalculate - event with multiple calculation requests. Contains an id which could be used for corelation of requests and responses, and two integer values which will be used by the calculation - in this particular example, 
      these two number are simply multiplied
    * EventCalculateResponse - response containing calculation result
    
#### HTTP endpoints

* GET
  * __/get?id=x__ - querying the HTTP server on this endpoint will result in _EventGet_ being sent to the actor and reply from _EventGetResponse_ will be sent back. Parameter _id_ is optional.
* POST
  * __/calculate__ - calculation results are sent as JSON in the body of POST request and then sent to the actor via _EventCalculate_ event. The actor will reply with _EventCalculateResponse_ and the results
    contained in the event will be converted to JSON representation and sent back.
    
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

## Running image on Kubernetes (K3S)

#### Environment preparation

Install K3S on each of the nodes:
```
root@ip-172-31-33-90:~$ curl -sfL https://get.k3s.io | sh -
...

root@ip-172-31-33-90:~# k3s kubectl get node
NAME              STATUS     ROLES    AGE   VERSION
ip-172-31-33-90   Ready      master   27h   v1.14.3-k3s.1

root@ip-172-31-36-57:~$ curl -sfL https://get.k3s.io | sh -
...

root@ip-172-31-36-57:~# k3s kubectl get node
NAME              STATUS   ROLES    AGE   VERSION
ip-172-31-36-57   Ready    master   27h   v1.14.3-k3s.1

root@ip-172-31-43-15:~$ curl -sfL https://get.k3s.io | sh -
...

root@ip-172-31-43-15:~# k3s kubectl get node
NAME              STATUS   ROLES    AGE   VERSION
ip-172-31-43-15   Ready    master   27h   v1.14.3-k3s.1
```

After installing _K3S_ on each host, they form 3 separate clusters with 3 separate _Master_ nodes. To change roles of two nodes into agents, it is necessary to modify the service configuration on two of the nodes.
First, an authentication token from _Master_ node is needed - it will be used by worker node to join the cluster.

_It is possible to direct the K3S installer to configure the node as Worker during installation, but this method is not provided explicitly in K3S documentation and was discovered later and is not described here_

```
root@ip-172-31-33-90:~# cat /var/lib/rancher/k3s/server/node-token
K1079aa3a38a668d93b24492021b2b1084350bfedd143f53c4ac01e4a4e83db3b71::node:5204bad6c8bdf5cc1d509b4d83f60e0c
```

The same change needs to be applied on both _Worker_ nodes:

```
root@ip-172-31-36-57:~# systemctl stop k3s

root@ip-172-31-43-15:~# systemctl stop k3s

root@ip-172-31-36-57:~# vi /etc/systemd/system/k3s.service

root@ip-172-31-43-15:~# vi /etc/systemd/system/k3s.service
```

The service configuration file on both nodes needs to be changed as below:

```
#ExecStart=/usr/local/bin/k3s server   <-- this line needs to be commented out and replaced by the below line
ExecStart=/usr/local/bin/k3s agent --server https://ip-172-31-33-90:6443 --token K1079aa3a38a668d93b24492021b2b1084350bfedd143f53c4ac01e4a4e83db3b71::node:5204bad6c8bdf5cc1d509b4d83f60e0c
```

After the file is modified, K3S service needs to be started up again on both nodes:

```
root@ip-172-31-36-57:~# systemctl daemon-reload
root@ip-172-31-36-57:~# systemctl start k3s
root@ip-172-31-36-57:~# systemctl status k3s
● k3s.service - Lightweight Kubernetes
   Loaded: loaded (/etc/systemd/system/k3s.service; enabled; vendor preset: enabled)
   Active: active (running) since Tue 2019-07-02 20:29:41 UTC; 13s ago
     Docs: https://k3s.io
  Process: 7382 ExecStartPre=/sbin/modprobe overlay (code=exited, status=0/SUCCESS)
  Process: 7380 ExecStartPre=/sbin/modprobe br_netfilter (code=exited, status=0/SUCCESS)
 Main PID: 7384 (k3s-agent)

root@ip-172-31-43-15:~# systemctl daemon-reload
root@ip-172-31-43-15:~# systemctl start k3s
root@ip-172-31-43-15:~# systemctl status k3s
● k3s.service - Lightweight Kubernetes
   Loaded: loaded (/etc/systemd/system/k3s.service; enabled; vendor preset: enabled)
   Active: active (running) since Tue 2019-07-02 20:29:50 UTC; 12s ago
     Docs: https://k3s.io
  Process: 7150 ExecStartPre=/sbin/modprobe overlay (code=exited, status=0/SUCCESS)
  Process: 7148 ExecStartPre=/sbin/modprobe br_netfilter (code=exited, status=0/SUCCESS)
 Main PID: 7152 (k3s-agent)

```

Once both services are up and running, it can be checked if both nodes joined the cluster correctly as _Workers_:

```
root@ip-172-31-33-90:~# k3s kubectl get node
NAME              STATUS   ROLES    AGE   VERSION
ip-172-31-33-90   Ready    master   27h   v1.14.3-k3s.1
ip-172-31-36-57   Ready    worker   26h   v1.14.3-k3s.1
ip-172-31-43-15   Ready    worker   26h   v1.14.3-k3s.1
```

#### Running the image as Deployment

On master node, create a deployment file _scala-akka-kubernetes-example.yaml_ or use the file already provided in the project (_scp_ the file from the project to the host with _Master_ node):

```
root@ip-172-31-33-90:~# vi scala-akka-kubernetes-example.yaml
```

Once the file is available on the node, the image can be started as Deployment across all the nodes of the cluster:

```
root@ip-172-31-33-90:~# k3s kubectl apply -f ./scala-akka-kubernetes-example.yaml
deployment.apps/scala-akka-kubernetes-example-deployment created

root@ip-172-31-33-90:~# k3s kubectl get deployments -o wide
NAME                                       READY   UP-TO-DATE   AVAILABLE   AGE   CONTAINERS                      IMAGES                                              SELECTOR
scala-akka-kubernetes-example-deployment   3/3     3            3           81s   scala-akka-kubernetes-example   krzsam/examples:scala-akka-kubernetes-example-0.3   app=scala-akka-kubernetes-example

root@ip-172-31-33-90:~# k3s kubectl get pods -o wide
NAME                                                      READY   STATUS    RESTARTS   AGE   IP           NODE              NOMINATED NODE   READINESS GATES
scala-akka-kubernetes-example-deployment-7c88ff54-79c6c   1/1     Running   0          49s   10.42.2.19   ip-172-31-36-57   <none>           <none>
scala-akka-kubernetes-example-deployment-7c88ff54-8tqjh   1/1     Running   0          49s   10.42.1.20   ip-172-31-43-15   <none>           <none>
scala-akka-kubernetes-example-deployment-7c88ff54-b4fwc   1/1     Running   0          49s   10.42.0.14   ip-172-31-33-90   <none>           <none>
```

Each of the nodes where the image is running can reply to requests on its assigned IP. The hostname as seen from within the deployment is same as the corresponding _Pod name_:

* GET
```
root@ip-172-31-33-90:~# curl http://10.42.2.19:9090/get
{ "count": 52 , "hostname": "SCALA-AKKA-KUBERNETES-EXAMPLE-DEPLOYMENT-7C88FF54-79C6C"  }

root@ip-172-31-33-90:~# curl http://10.42.1.20:9090/get
{ "count": 61 , "hostname": "SCALA-AKKA-KUBERNETES-EXAMPLE-DEPLOYMENT-7C88FF54-8TQJH"  }

root@ip-172-31-33-90:~# curl http://10.42.0.14:9090/get
{ "count": 71 , "hostname": "SCALA-AKKA-KUBERNETES-EXAMPLE-DEPLOYMENT-7C88FF54-B4FWC"  }
```

* POST
```
root@ip-172-31-33-90:~# curl -d " [ { \"id\": 1, \"param_1\": 3, \"param_2\": 4 } , { \"id\": 2, \"param_1\": 4, \"param_2\": 5 } ]  " -H "Content-Type: application/json" -X POST 10.42.2.19:9090/calculate
[{"id":1,"result":12},{"id":2,"result":20}]

root@ip-172-31-33-90:~# curl -d " [ { \"id\": 1, \"param_1\": 3, \"param_2\": 4 } , { \"id\": 2, \"param_1\": 4, \"param_2\": 5 } ]  " -H "Content-Type: application/json" -X POST 10.42.1.20:9090/calculate
[{"id":1,"result":12},{"id":2,"result":20}]

root@ip-172-31-33-90:~# curl -d " [ { \"id\": 1, \"param_1\": 3, \"param_2\": 4 } , { \"id\": 2, \"param_1\": 4, \"param_2\": 5 } ]  " -H "Content-Type: application/json" -X POST 10.42.0.14:9090/calculate
[{"id":1,"result":12},{"id":2,"result":20}]
```

At this point, there is no overlying load balancing functionality available yet, which would bring all the Pods together, and the service is only available to be queried on each _Pod IP_ separately.

#### Exposing the Deployment as a Service

To expose the deployment as a service _scala-akka-kubernetes-service.yaml_ needs to be applied to the cluster. Service layer is independent and additional to the deployment layer, and can be added or removed
without affecting the deployment.

```
root@ip-172-31-33-90:~# k3s kubectl apply -f scala-akka-kubernetes-service.yaml
service/scala-akka-kubernetes-example-service created

root@ip-172-31-33-90:~# k3s kubectl get services -o wide
NAME                                    TYPE           CLUSTER-IP      EXTERNAL-IP                              PORT(S)          AGE     SELECTOR
kubernetes                              ClusterIP      10.43.0.1       <none>                                   443/TCP          29h     <none>
scala-akka-kubernetes-example-service   LoadBalancer   10.43.148.205   172.31.33.90,172.31.36.57,172.31.43.15   8080:32140/TCP   7m38s   app=scala-akka-kubernetes-example

root@ip-172-31-33-90:~# k3s kubectl describe service scala-akka-kubernetes-example-service
Name:                     scala-akka-kubernetes-example-service
Namespace:                default
Labels:                   <none>
Annotations:              kubectl.kubernetes.io/last-applied-configuration:
                            {"apiVersion":"v1","kind":"Service","metadata":{"annotations":{},"name":"scala-akka-kubernetes-example-service","namespace":"default"},"sp...
Selector:                 app=scala-akka-kubernetes-example
Type:                     LoadBalancer
IP:                       10.43.148.205
LoadBalancer Ingress:     172.31.33.90, 172.31.36.57, 172.31.43.15
Port:                     <unset>  8080/TCP
TargetPort:               9090/TCP
NodePort:                 <unset>  32140/TCP
Endpoints:                10.42.0.14:9090,10.42.1.20:9090,10.42.2.19:9090
Session Affinity:         None
External Traffic Policy:  Cluster
Events:                   <none>

root@ip-172-31-33-90:~# k3s kubectl get pods -o wide
NAME                                                      READY   STATUS    RESTARTS   AGE     IP           NODE              NOMINATED NODE   READINESS GATES
scala-akka-kubernetes-example-deployment-7c88ff54-79c6c   1/1     Running   0          39m     10.42.2.19   ip-172-31-36-57   <none>           <none>
scala-akka-kubernetes-example-deployment-7c88ff54-8tqjh   1/1     Running   0          39m     10.42.1.20   ip-172-31-43-15   <none>           <none>
scala-akka-kubernetes-example-deployment-7c88ff54-b4fwc   1/1     Running   0          39m     10.42.0.14   ip-172-31-33-90   <none>           <none>
svclb-scala-akka-kubernetes-example-service-hcjgn         1/1     Running   0          6m25s   10.42.1.21   ip-172-31-43-15   <none>           <none>
svclb-scala-akka-kubernetes-example-service-t94sh         1/1     Running   0          6m25s   10.42.2.20   ip-172-31-36-57   <none>           <none>
svclb-scala-akka-kubernetes-example-service-vjdwn         1/1     Running   0          6m25s   10.42.0.15   ip-172-31-33-90   <none>           <none>
```

Once the service is up and running on the cluster, it provides load balancing functionality across all pods of the deployment. To access the application, service own IP address (assigned - 10.43.148.205) 
and port (as defined - 8080) need to be used. Multiple requests will be load balanced and routed to different Pods within the service. 

* GET
```
root@ip-172-31-33-90:~# curl http://10.43.148.205:8080/get
{ "count": 729 , "hostname": "SCALA-AKKA-KUBERNETES-EXAMPLE-DEPLOYMENT-7C88FF54-8TQJH"  }
root@ip-172-31-33-90:~# curl http://10.43.148.205:8080/get
{ "count": 730 , "hostname": "SCALA-AKKA-KUBERNETES-EXAMPLE-DEPLOYMENT-7C88FF54-79C6C"  }
root@ip-172-31-33-90:~# curl http://10.43.148.205:8080/get
{ "count": 734 , "hostname": "SCALA-AKKA-KUBERNETES-EXAMPLE-DEPLOYMENT-7C88FF54-B4FWC"  }
root@ip-172-31-33-90:~# curl http://10.43.148.205:8080/get
{ "count": 734 , "hostname": "SCALA-AKKA-KUBERNETES-EXAMPLE-DEPLOYMENT-7C88FF54-B4FWC"  }
root@ip-172-31-33-90:~# curl http://10.43.148.205:8080/get
{ "count": 731 , "hostname": "SCALA-AKKA-KUBERNETES-EXAMPLE-DEPLOYMENT-7C88FF54-79C6C"  }
```

* POST
```
root@ip-172-31-33-90:~# curl -d " [ { \"id\": 1, \"param_1\": 5, \"param_2\": 9 } , { \"id\": 2, \"param_1\": 8, \"param_2\": 5 } ]  " -H "Content-Type: application/json" -X POST 10.43.148.205:8080/calculate
[{"id":1,"result":45},{"id":2,"result":40}]
```

The service can be configured with different types depending on requirements - the service type used in the example, _LoadBalancer_, also allows load balancing and exposing the service externally to be 
accessible from outside of the cluster. 

## Other notes

* This example project provides only image for _amd64_ processor architecture - this can be run either directly on Linux, or on Windows via Docker Toolbox/Console.
  When run on any other processor architecture you will most likely see [this problem](https://forums.docker.com/t/standard-init-linux-go-190-exec-user-process-caused-exec-format-error/49368/5)  

## Links
* Docker
  * [Docker Toolbox](https://docs.docker.com/toolbox/overview/) 
  * [Docker Docs](https://docs.docker.com/get-started/)
  * [Docker Compose](https://docs.docker.com/compose/compose-file/)
  * [Docker repository with examples](https://cloud.docker.com/u/krzsam/repository/docker/krzsam/examples)
* Kubernetes
  * [K3S](https://k3s.io/)
  * [K3S Documentation](https://github.com/rancher/k3s/blob/master/README.md)
  * This version of K3S was affected by an issue described [here](https://github.com/rancher/k3s/issues/478) and [here](https://github.com/rancher/k3s/issues/497) 
    which causes Pods to be stuck in _ContainerCreating_ state when the agent node is started via _systemctl_
  * [Kubernetes Deployment Docs](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/)
  * [Kubernetes Service Docs](https://kubernetes.io/docs/concepts/services-networking/service/)
* Akka 
  * Actor, Streams: 2.5.23
  * [Http](https://doc.akka.io/docs/akka-http/current/introduction.html): 10.1.8
* [Sbt Native Packager](https://github.com/sbt/sbt-native-packager): 1.3.23
* Scala: 2.12.8
* Infrastructure (for K3S)
  * AWS, 3 nodes _t3a.xlarge_ (4 processors, 16GB memory)
  * For simplicity, all network traffic on all TCP and UDP ports is enabled in between each of the nodes
    * ip-172-31-33-90 : _Master_ (also serves as _Worker_ node)
    * ip-172-31-43-15 : _Worker_ node
    * ip-172-31-36-57 : _Worker_ node
  