# Enabling AWS X-Ray Tracing using OpenTelemetry (Hybrid Approach)

This document describes how to enable distributed tracing in this service using OpenTelemetry with AWS X-Ray. The setup follows a hybrid approach using the OpenTelemetry Java Agent along with an OpenTelemetry Collector sidecar.

---

## 📌 Overview

Tracing is enabled using:

* OpenTelemetry Java Agent (auto-instrumentation)
* OpenTelemetry Collector (sidecar container)
* AWS X-Ray as the tracing backend

---

## 🐳 Docker Changes

The application Dockerfile has already been updated to include the OpenTelemetry Java agent.

---

## 🧩 Sidecar Container Configuration (Mandatory)

A sidecar container is required to collect and export traces to AWS X-Ray.

Add the following container to your ECS Task Definition:

```json
{
  "name": "otel-collector",
  "image": "public.ecr.aws/aws-observability/aws-otel-collector:v0.41.0",
  "essential": false,
  "logConfiguration": {
    "logDriver": "awslogs",
    "options": {
      "awslogs-group": "${otel_log_group_name}",
      "awslogs-region": "${region}",
      "awslogs-stream-prefix": "otel"
    }
  },
  "portMappings": [
    {
      "containerPort": 4317,
      "hostPort": 4317,
      "protocol": "tcp"
    }
  ],
  "secrets": ${otel_secret_env_variables}
}
```

---

## 🔐 Collector Configuration (via Secrets)

In `otel_secret_env_variables`, add the following key:

### Key:

`AOT_CONFIG_CONTENT`

### Value:

```yaml
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317

exporters:
  awsxray:
    region: ap-south-1

service:
  pipelines:
    traces:
      receivers: [otlp]
      exporters: [awsxray]
```

---

## ⚙️ Required Environment Variables

Add the following environment variables to your application container:

```hcl
{
  name  = "OTEL_SERVICE_NAME",
  value = local.resource_name
},
{
  name  = "OTEL_TRACES_EXPORTER",
  value = "otlp"
},
{
  name  = "OTEL_EXPORTER_OTLP_ENDPOINT",
  value = "http://localhost:4317"
},
{
  name  = "OTEL_PROPAGATORS",
  value = "tracecontext,baggage,xray"
},
{
  name  = "OTEL_RESOURCE_ATTRIBUTES",
  value = "deployment.environment=${var.environment}"
},
{
  name  = "OTEL_EXPORTER_OTLP_PROTOCOL",
  value = "grpc"
}
```

---

## 🔑 IAM Permissions

Attach the following policy to the ECS Task Role to allow publishing traces to AWS X-Ray:

```hcl
resource "aws_iam_role_policy_attachment" "ecs_task_role_xray_policy_attachment" {
  role       = aws_iam_role.ecs_task_role.name
  policy_arn = "arn:aws:iam::aws:policy/AWSXrayWriteOnlyAccess"
}
```

---

## 🔁 How It Works

```text
Application (Java Agent)
        ↓
OTLP (localhost:4317)
        ↓
OpenTelemetry Collector (Sidecar)
        ↓
AWS X-Ray
```

---

## ✅ Summary

* OpenTelemetry Java Agent enables automatic tracing
* Sidecar collector is required to export traces
* Environment variables configure trace behavior
* IAM permissions allow communication with AWS X-Ray

---

## 📌 Notes

* No code changes are required for basic tracing
* Custom spans can be added in the service layer for enhanced observability
* Ensure both containers (app + collector) run in the same ECS task

---
