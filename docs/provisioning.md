# Provisioning guide (resource-conscious for 8GB host)

Assumptions:
- Host: Windows with VirtualBox, 8GB RAM total, 199GB free disk.
- We'll create one Ubuntu Server VM (recommended: 4 vCPU, 6GB RAM) — if you need host UI + Splunk on same Windows, allocate accordingly. If you want Splunk on Windows host install Splunk Enterprise for Windows there.

Steps (brief):
1. Create an Ubuntu 22.04 LTS VM in VirtualBox:
   - RAM: 6 GB (6144 MB)
   - CPUs: 2
   - Disk: 50 GB
   - Network: Bridged (or host-only + NAT)
2. Install Ubuntu server, enable SSH.
3. Install Docker and k3s:
   - Install Docker: apt update && apt install -y docker.io
   - Install k3s: curl -sfL https://get.k3s.io | sh -
   - Verify: kubectl get nodes
4. Deploy namespace and manifests:
   - kubectl apply -f k8s/namespace.yaml
   - kubectl apply -f k8s/deploy-*.yaml
5. Install Prometheus/Grafana (use Helm charts) or apply minimal manifests in monitoring/.
6. Splunk on Windows host:
   - Install Splunk Enterprise Windows: https://www.splunk.com
   - Enable HEC and create token: Settings → Data Inputs → HTTP Event Collector
   - From k8s, forward logs/metrics to Splunk HEC using Fluentd/Fluent Bit or Prometheus remote_write (for metrics).
7. Monitoring from Windows:
   - Splunk Web UI runs on Windows host: http://localhost:8000 (default)
   - Add dashboards searching by sourcetype, host, and correlation_id.
Memory tips:
- Use single replicas and low resource requests/limits in k8s manifests.
- Use lightweight images (alpine-based) where possible.
- Consider running Kafka and Couchbase as hosted services if memory is insufficient. Running both in a single 6GB VM is likely to exhaust RAM.
