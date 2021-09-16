@Library("sym-pipeline") _


import com.symphony.cicd.SymphonyCICDUtils

def util = new SymphonyCICDUtils()

node {
    def symCliRepo = env.PROJECT_REPO ?: "workflow-bot"
    def symCliOrg = env.PROJECT_ORG ?: "SymphonyOSF"
    def symCliBranch = env.BRANCH_NAME ?: "master"

    withEnv(["PROJECT_TYPE=java",
             "GIT_REPO=${symCliRepo}",
             "GIT_ORG=${symCliOrg}",
             "GIT_BRANCH=${symCliBranch}"
    ]) {

        stage('Git Checkout') {
            gitCheckout()
        }

        stage('Build Project') {
            try {
                sh './gradlew check'
            } finally {
                util.archiveJunitTestArtifacts("**/build/test-results/test/*.xml")
            }
        }

    }
}


