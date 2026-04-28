// setup-pipeline.groovy — Run via Jenkins CLI to create shared lib, pipeline job, and credentials
// Usage: java -jar jenkins-cli.jar -s http://localhost:8081 -auth admin:admin123 groovy = < ci/setup-pipeline.groovy

import jenkins.model.*
import org.jenkinsci.plugins.workflow.job.*
import org.jenkinsci.plugins.workflow.cps.*
import hudson.plugins.git.*
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.domains.*
import com.cloudbees.plugins.credentials.impl.*
import org.jenkinsci.plugins.workflow.libs.*
import jenkins.plugins.git.*

def instance = Jenkins.getInstance()

// ── 1. Shared Library: diksha-dev-lib ──
def libConfig = instance.getDescriptor("org.jenkinsci.plugins.workflow.libs.GlobalLibraries")
def scm = new GitSCMSource("diksha-dev-lib", "https://github.com/tsprasath/ai-devops.git", "", "*", "", true)
def retriever = new SCMSourceRetriever(scm)
def lib = new LibraryConfiguration("diksha-dev-lib", retriever)
lib.setDefaultVersion("main")
lib.setImplicit(false)
lib.setAllowVersionOverride(true)
libConfig.get().setLibraries([lib])
libConfig.get().save()
println "[setup] Shared library 'diksha-dev-lib' configured"

// ── 2. Pipeline Job: ai-devops ──
def jobName = "ai-devops"
def existingJob = instance.getItem(jobName)
if (existingJob != null) {
    existingJob.delete()
    println "[setup] Deleted existing job '${jobName}'"
}

def job = instance.createProject(WorkflowJob, jobName)
def scmDef = new CpsScmFlowDefinition(
    new GitSCM(
        [new UserRemoteConfig("https://github.com/tsprasath/ai-devops.git", null, null, null)],
        [new BranchSpec("*/main")],
        null, null, []
    ),
    "ci/Jenkinsfile.local"
)
scmDef.setLightweight(true)
job.setDefinition(scmDef)
job.save()
println "[setup] Pipeline job '${jobName}' created (SCM: ci/Jenkinsfile.local)"

// ── 3. Credentials (placeholders — replace with real values) ──
def domain = Domain.global()
def store = SystemCredentialsProvider.getInstance().getStore()

def ocir = new UsernamePasswordCredentialsImpl(
    CredentialsScope.GLOBAL, "ocir-credentials", "OCI Container Registry",
    "bmzbbujw9kal/oracleidentitycloudservice/user@example.com", "placeholder-token"
)
store.addCredentials(domain, ocir)
println "[setup] Credential 'ocir-credentials' added (placeholder — update with real values)"

def teamsWebhook = new UsernamePasswordCredentialsImpl(
    CredentialsScope.GLOBAL, "teams-webhook-url", "Teams Webhook",
    "webhook", "https://placeholder.webhook.office.com"
)
store.addCredentials(domain, teamsWebhook)
println "[setup] Credential 'teams-webhook-url' added (placeholder — update with real values)"

instance.save()
println "[setup] ✅ ALL DONE — shared lib + job + credentials configured"
