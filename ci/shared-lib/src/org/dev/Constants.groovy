package org.dev

class Constants {
    // OCI Configuration
    static final String OCI_REGION = 'ap-mumbai-1'
    static final String OCIR_URL = "${OCI_REGION}.ocir.io"
    static final String PROJECT_NAME = 'diksha-dev'
    static final String OCIR_NAMESPACE = 'diksha'
    static final String OCIR_REPO = "${OCIR_URL}/${OCIR_NAMESPACE}/${PROJECT_NAME}"

    // Jenkins Credential IDs
    static final String OCIR_CREDENTIALS_ID = 'ocir-credentials'
    static final String SLACK_WEBHOOK_CREDENTIAL = 'slack-webhook-url'
    static final String TEAMS_WEBHOOK_CREDENTIAL = 'teams-webhook-url'
    static final String GITHUB_TOKEN_CREDENTIAL = 'github-token'
    static final String SONAR_TOKEN_CREDENTIAL = 'sonar-token'

    // Trivy Configuration
    static final String TRIVY_SEVERITY_CRITICAL = 'CRITICAL'
    static final String TRIVY_SEVERITY_HIGH = 'HIGH'
    static final String TRIVY_SEVERITY_DEFAULT = 'CRITICAL,HIGH'
    static final int TRIVY_CRITICAL_THRESHOLD = 0
    static final int TRIVY_HIGH_THRESHOLD = 5

    // Standard Labels
    static final Map<String, String> STANDARD_LABELS = [
        'app.kubernetes.io/managed-by': 'jenkins',
        'app.kubernetes.io/part-of'   : PROJECT_NAME,
        'org.opencontainers.image.source': 'https://github.com/diksha/dev'
    ]

    // Notification Channels
    static final String SLACK_CHANNEL = '#ci-cd-notifications'
    static final String SLACK_CHANNEL_ALERTS = '#ci-cd-alerts'
}
