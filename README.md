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
    
## [Deploying application image to Docker](https://github.com/krzsam/scala-akka-kubernetes-example/tree/master/README-Deploy-Docker.md)

## [Running image on Docker](https://github.com/krzsam/scala-akka-kubernetes-example/tree/master/README-Run-Docker.md)

## [Running image on Kubernetes (K3S)](https://github.com/krzsam/scala-akka-kubernetes-example/tree/master/README-Run-K3S.md)

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
