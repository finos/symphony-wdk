@Library("sym-pipeline") _

import com.symphony.cicd.SymphonyCICDUtils
import com.symphony.cicd.deploy.K8sUtils

def util = new SymphonyCICDUtils()
def isPullRequest = util.isPullRequest()
def k8sUtils = new K8sUtils()

node {
    def symCliRepo = env.PROJECT_REPO ?: "workflow-bot"
    def symCliOrg = env.PROJECT_ORG ?: "SymphonyOSF"
    def symCliBranch = env.BRANCH_NAME ?: "main"

    withEnv(["PROJECT_TYPE=java",
             "GIT_REPO=${symCliRepo}",
             "GIT_ORG=${symCliOrg}",
             "GIT_BRANCH=${symCliBranch}"
    ]) {

        stage('Git Checkout') {
            gitCheckout()
        }

        stage('Build Project') {
            sh './gradlew check'
        }

        stage("Publish and deploy") {
            if (isPullRequest) {
                println "Nothing to deploy, this is a pull request"
            } else {
                k8sUtils.activateGceCreds("/usr/share/service-account-for-sym-dev-plat", "sym-dev-plat")
                sh "./build-image.sh"
            }
        }
    }
}


