package kubernetes.admission

import future.keywords.in

required_labels := {
    "app.kubernetes.io/name",
    "app.kubernetes.io/version",
}

deny[msg] {
    input.request.kind.kind in ["Deployment", "StatefulSet", "DaemonSet", "Job", "CronJob", "Pod"]
    provided := {label | input.request.object.metadata.labels[label]}
    missing := required_labels - provided
    count(missing) > 0
    msg := sprintf("Resource %s/%s is missing required labels: %v", [
        input.request.object.kind,
        input.request.object.metadata.name,
        missing,
    ])
}

deny[msg] {
    input.request.kind.kind in ["Deployment", "StatefulSet", "DaemonSet"]
    provided := {label | input.request.object.spec.template.metadata.labels[label]}
    missing := required_labels - provided
    count(missing) > 0
    msg := sprintf("Pod template in %s/%s is missing required labels: %v", [
        input.request.object.kind,
        input.request.object.metadata.name,
        missing,
    ])
}
