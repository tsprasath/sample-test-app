package kubernetes.admission

import future.keywords.in

deny[msg] {
    input.request.kind.kind in ["Pod"]
    container := input.request.object.spec.containers[_]
    container.securityContext.privileged == true
    msg := sprintf("Privileged container '%s' is not allowed in Pod '%s'", [
        container.name,
        input.request.object.metadata.name,
    ])
}

deny[msg] {
    input.request.kind.kind in ["Deployment", "StatefulSet", "DaemonSet"]
    container := input.request.object.spec.template.spec.containers[_]
    container.securityContext.privileged == true
    msg := sprintf("Privileged container '%s' is not allowed in %s '%s'", [
        container.name,
        input.request.object.kind,
        input.request.object.metadata.name,
    ])
}

deny[msg] {
    input.request.kind.kind in ["Pod"]
    container := input.request.object.spec.initContainers[_]
    container.securityContext.privileged == true
    msg := sprintf("Privileged init container '%s' is not allowed in Pod '%s'", [
        container.name,
        input.request.object.metadata.name,
    ])
}
