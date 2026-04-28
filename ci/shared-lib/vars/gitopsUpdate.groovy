def call(Map config = [:]) {
    def gitopsRepo  = config.gitopsRepo ?: error("gitopsRepo is required")
    def imageTag    = config.imageTag   ?: error("imageTag is required")
    def valuesFile  = config.valuesFile ?: 'environments/dev/values.yaml'
    def yamlPath    = config.yamlPath   ?: '.image.tag'
    def branch      = config.branch     ?: 'main'
    def credId      = config.credentialsId ?: 'git-credentials'
    def serviceName = config.serviceName ?: 'auth-service'

    echo "Updating GitOps repo with image tag: ${imageTag}"

    withCredentials([usernamePassword(credentialsId: credId, usernameVariable: 'GIT_USER', passwordVariable: 'GIT_TOKEN')]) {
        sh """
            rm -rf gitops-workdir
            git clone https://${GIT_USER}:${GIT_TOKEN}@${gitopsRepo} gitops-workdir
            cd gitops-workdir
            git checkout ${branch}
            yq e '${yamlPath} = "${imageTag}"' -i ${valuesFile}
            git config user.email "jenkins@ci.local"
            git config user.name "Jenkins CI"
            git add .
            git commit -m "chore: update ${serviceName} image to ${imageTag}" || echo "No changes to commit"
            git push origin ${branch}
        """
    }

    echo "GitOps repo updated successfully"
}
