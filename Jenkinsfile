// Thin Jenkinsfile — delegates to shared pipeline in ai-devops
// Jenkins job is configured via JCasC in the devops repo

@Library('diksha-shared-lib') _

pipeline {
    agent any

    environment {
        SERVICE_NAME = 'auth-service'
    }

    stages {
        stage('Build & Deploy') {
            steps {
                buildAndPush(
                    service: env.SERVICE_NAME,
                    registry: env.OCIR_REGISTRY,
                    namespace: env.OCIR_NAMESPACE
                )
            }
        }
    }

    post {
        failure {
            notifyTeam(channel: 'builds', status: 'FAILURE')
        }
    }
}
