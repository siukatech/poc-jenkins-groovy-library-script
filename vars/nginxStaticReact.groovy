// Reference:
// https://www.tutorialworks.com/jenkins-shared-library

def call(Map config = [:]) {

/* Requires the Docker Pipeline plugin */
pipeline {
    //  agent {
    //     docker {
    //         image 'node:20.12.2-slim'
    //     }
    // }
    agent any

    // tools {
    //     'org.jenkinsci.plugins.docker.commons.tools.DockerTool' '18.09'
    // }

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

                sh "node --version"
                sh "npm --version"
                // sh "docker version" // DOCKER_CERT_PATH is automatically picked up by the Docker client
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

        stage('build-project') {
            steps {
                script {
                    def skipTest = config.skipTest
                    if ( skipTest ) {
                        sh "npm run build"
                    }
                    else {
                        sh "npm run build"
                    }

                    sh "ls -la build/*"
                }
            }
        }

        stage('build-image') {
            steps {
                script {
                    def projImage = docker.build("nginx-react:${env.BUILD_ID}")
                }
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
