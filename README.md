# Learning01 — On-Prem Demo (k3s / Docker) with Monitoring & Splunk integration

Contents:
- react-app/  — minimal React frontend (static served by nginx)
- spring-api/ — minimal Spring Boot app (Maven)
- vertx-service/ — minimal Vert.x consumer (Maven)
- docker/ — Dockerfiles for each component
- k8s/ — Kubernetes manifests optimized for low resources (k3s)
- monitoring/ — Prometheus, Grafana, Loki manifest snippets + Splunk HEC example
- github-actions/ — CI workflows to build and push images (GitHub Actions)
- docs/ — instructions for provisioning VirtualBox VM, k3s, and Splunk guidance

This scaffold is intentionally minimal: it contains buildable skeletons and configuration examples.
