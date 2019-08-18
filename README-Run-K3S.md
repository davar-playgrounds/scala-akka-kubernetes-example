## Running image on Kubernetes/K3S on AWS

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
