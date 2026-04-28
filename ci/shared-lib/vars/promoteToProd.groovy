import org.dev.Constants

def call(Map config = [:]) {
    def gitopsRepo = config.get('gitopsRepo', '')
    def gitopsBranch = config.get('gitopsBranch', 'main')
    def imageTag = config.get('imageTag', '')
    def appName = config.get('appName', Constants.PROJECT_NAME)
    def smokeTestUrl = config.get('smokeTestUrl', '')
    def smokeTestCmd = config.get('smokeTestCmd', '')
    def approvers = config.get('approvers', '')
    def timeout_minutes = config.get('timeoutMinutes', 30)

    echo "=== Production Promotion for ${appName}:${imageTag} ==="

    // Manual approval gate
    def approverParam = approvers ? ", submitter: '${approvers}'" : ''
    timeout(time: timeout_minutes, unit: 'MINUTES') {
        input message: "Promote ${appName}:${imageTag} to PRODUCTION?",
              ok: 'Deploy to Production',
              submitter: approvers ?: null
    }

    // Run smoke tests against staging
    if (smokeTestUrl || smokeTestCmd) {
        echo "Running smoke tests against staging..."
        def smokeExit
        if (smokeTestCmd) {
            smokeExit = sh(script: smokeTestCmd, returnStatus: true)
        } else {
            smokeExit = sh(
                script: """
                    for i in 1 2 3 4 5; do
                        STATUS=\$(curl -s -o /dev/null -w '%{http_code}' ${smokeTestUrl}/health)
                        if [ "\$STATUS" = "200" ]; then
                            echo "Smoke test passed (attempt \$i)"
                            exit 0
                        fi
                        echo "Attempt \$i: got \$STATUS, retrying..."
                        sleep 5
                    done
                    echo "Smoke tests failed"
                    exit 1
                """,
                returnStatus: true
            )
        }
        if (smokeExit != 0) {
            notifyTeam("FAILED", "Smoke tests failed for ${appName}:${imageTag} - promotion aborted")
            error "Smoke tests failed - production promotion aborted"
        }
    }

    // Update prod GitOps repo
    echo "Updating production GitOps repository..."
    dir('gitops-prod') {
        git branch: gitopsBranch, credentialsId: Constants.GITHUB_TOKEN_CREDENTIAL, url: gitopsRepo

        sh """
            cd overlays/prod || cd environments/prod || true
            if [ -f kustomization.yaml ]; then
                sed -i 's|newTag:.*|newTag: ${imageTag}|' kustomization.yaml
            fi
            if [ -f values.yaml ]; then
                sed -i 's|tag:.*|tag: "${imageTag}"|' values.yaml
            fi
            git add -A
            git commit -m "prod: promote ${appName} to ${imageTag}" || true
            git push origin ${gitopsBranch}
        """
    }

    // Create semver git tag
    def semverTag = "v${imageTag}"
    sh """
        git tag -a ${semverTag} -m "Production release ${semverTag}" || true
        git push origin ${semverTag} || true
    """

    notifyTeam("SUCCESS", "Production promotion complete: ${appName}:${imageTag}")
    echo "Production promotion complete: ${appName}:${imageTag}"

    return [status: 'promoted', tag: imageTag, semver: semverTag]
}

private void notifyTeam(String status, String message) {
    try {
        withCredentials([string(credentialsId: Constants.SLACK_WEBHOOK_CREDENTIAL, variable: 'SLACK_URL')]) {
            def color = status == 'SUCCESS' ? 'good' : 'danger'
            sh """
                curl -s -X POST \$SLACK_URL \
                    -H 'Content-type: application/json' \
                    -d '{"channel":"${Constants.SLACK_CHANNEL}","attachments":[{"color":"${color}","text":"${message}"}]}'
            """
        }
    } catch (Exception e) {
        echo "Notification failed: ${e.message}"
    }
}
