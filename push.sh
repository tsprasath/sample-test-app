#!/bin/bash
# Push to GitHub and trigger Jenkins build
git push "$@" && \
curl -s -u admin:admin123 -X POST "http://localhost:8081/job/ai-devops/build" -o /dev/null -w "Jenkins build triggered: HTTP %{http_code}\n"
