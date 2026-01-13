# Kubernetes Deployment Guide

This directory contains Kubernetes manifests for deploying the Kafka Spring Boot application in two different configurations.

## Files

- **deployment.yaml**: Deployment configuration with 3 pods, all returning pod number `67`
- **statefulset.yaml**: StatefulSet configuration with 3 pods returning `0`, `1`, or `2` based on pod ordinal
- **service-deployment.yaml**: Service for load balancing Deployment pods
- **service-statefulset.yaml**: Service for load balancing StatefulSet pods (random routing)

## Prerequisites

1. A running Kubernetes cluster (minikube, kind, or cloud provider)
2. Docker image `kafka-app:latest` built and available to your cluster
3. `kubectl` configured to access your cluster

## Building the Docker Image

From the project root directory:

```bash
# Build the Spring Boot application
./mvnw clean package -DskipTests

# Build the Docker image
docker build -t kafka-app:latest .

# For minikube, load the image
minikube image load kafka-app:latest

# For kind, load the image
kind load docker-image kafka-app:latest
```

## Deploying the Deployment Configuration

The Deployment will create 3 pods, all returning pod number **67**.

```bash
# Apply the deployment
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service-deployment.yaml

# Check pod status
kubectl get pods -l app=kafka-app

# Test the endpoint (should always return 67)
kubectl port-forward service/kafka-service-deployment 8080:8080

# In another terminal
curl http://localhost:8080/api/messages/pod-number
# Expected output: {"podNumber":67}
```

## Deploying the StatefulSet Configuration

The StatefulSet will create 3 pods returning **0**, **1**, or **2** randomly via load balancing.

```bash
# Apply the statefulset
kubectl apply -f k8s/statefulset.yaml
kubectl apply -f k8s/service-statefulset.yaml

# Check pod status
kubectl get pods -l app=kafka-app-stateful

# Verify each pod's number individually
kubectl exec kafka-app-statefulset-0 -- curl -s localhost:8080/api/messages/pod-number
kubectl exec kafka-app-statefulset-1 -- curl -s localhost:8080/api/messages/pod-number
kubectl exec kafka-app-statefulset-2 -- curl -s localhost:8080/api/messages/pod-number

# Test random load balancing
kubectl port-forward service/kafka-service-stateful 8080:8080

# In another terminal, run multiple times
for i in {1..10}; do curl http://localhost:8080/api/messages/pod-number; echo; done
# Expected output: Random distribution of {"podNumber":0}, {"podNumber":1}, {"podNumber":2}
```

## Cleaning Up

```bash
# Delete Deployment resources
kubectl delete -f k8s/deployment.yaml
kubectl delete -f k8s/service-deployment.yaml

# Delete StatefulSet resources
kubectl delete -f k8s/statefulset.yaml
kubectl delete -f k8s/service-statefulset.yaml
```

## How It Works

### Deployment (POD_NUMBER=67)
- All 3 pods have the environment variable `POD_NUMBER` set to `67`
- No matter which pod handles the request, it always returns `67`

### StatefulSet (POD_NUMBER=0,1,2)
- Uses an init container to extract the pod ordinal from the hostname
- Writes the ordinal to a shared volume
- Main container reads the ordinal and sets it as `POD_NUMBER`
- The service distributes requests randomly across all 3 pods
- Each request may hit a different pod, returning a different number

## Troubleshooting

### Pods not starting
```bash
# Check pod logs
kubectl logs <pod-name>

# Describe pod for events
kubectl describe pod <pod-name>
```

### Image pull errors
Make sure the image is loaded into your cluster:
- For minikube: `minikube image load kafka-app:latest`
- For kind: `kind load docker-image kafka-app:latest`

### Health checks failing
The manifests assume Spring Boot Actuator is enabled. If not available, remove or modify the `livenessProbe` and `readinessProbe` sections.
