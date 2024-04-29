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
