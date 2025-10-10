Splunk integration options:
1) Run Splunk Enterprise (or Splunk Free) on your Windows host.
2) Enable HTTP Event Collector (HEC) token in Splunk.
3) Configure your apps / Fluentd / Logstash to forward logs to HEC:
   - Example HEC endpoint: https://<windows-host-ip>:8088/services/collector
4) Correlation ID:
   - Generate a correlation_id per HTTP request (Spring: Filter that reads X-Correlation-Id header or generates one)
   - Include correlation_id in logs and as a field when sending to Splunk.
Example Fluentd / Logstash -> Splunk config is provided below (example).
