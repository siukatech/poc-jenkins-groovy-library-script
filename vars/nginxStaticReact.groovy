// Reference:
// https://www.tutorialworks.com/jenkins-shared-library

def call(Map config = [:]) {

/* Requires the Docker Pipeline plugin */
pipeline {
     agent {
        docker {
            image 'node:20.12.2-slim'
        }
    }
//     agent any

    options {
        // Timeout counter starts AFTER agent is allocated
        // timeout(time: 1, unit: 'SECONDS')
        disableConcurrentBuilds()
        disableResume()
        preserveStashes(buildCount: 5)
    }

    stages {
        stage("hello-world") {
            steps {
                sh '''
                    printenv
                '''

                sh "npm --version"
            }
        }
        stage('parameter') {
            steps {
                echo "[${STAGE_NAME}] config: ${config}"
                script {
                    properties([
                        parameters([
                            string(
                                defaultValue: "${config.repoName}", 
                                name: 'PROJECT_NAME', 
                                description: 'Project Name',
                                trim: true
                            ),
                            string(
                                defaultValue: "${config.repoHttp}://${config.repoHostname}/${config.repoUsername}/${config.repoName}.git",
                                name: 'PROJECT_URL', 
                                description: 'Project Url',
                                trim: true
                            )
                        ])
                    ])
                    // Save to variables. Default to empty string if not found.
                    env.GITHUB_PROJECT_NAME = params.PROJECT_NAME
                    env.GITHUB_PROJECT_URL = params.PROJECT_URL
                }
                echo "[${STAGE_NAME}] GITHUB_PROJECT_NAME: ${GITHUB_PROJECT_NAME}"
                echo "[${STAGE_NAME}] GITHUB_PROJECT_URL: ${GITHUB_PROJECT_URL}"
            }
        }

        stage('set-up') {
            steps {
                sh "npm install --save-dev"
                sh "npm install"
            }
        }

        stage('build') {
            steps {
                def skipTest = config.skipTest
        //         // if ( skipTest ) {
        //         //     sh "npm run build"
        //         // }
        //         // else {
        //         //     sh "npm run build"
        //         // }

        //         // sh "ls -la build/*"
            }
        }
    }
  
    // https://www.jenkins.io/doc/pipeline/tour/post/
    post {
        always {
            echo 'One way or another, I have finished'
            deleteDir() /* clean up our workspace */
        }
        success {
            echo 'I succeeded!'
        }
    }

}

}
