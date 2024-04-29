// Reference:
// https://www.tutorialworks.com/jenkins-shared-library

def call(Map config = [:]) {

/* Requires the Docker Pipeline plugin */
pipeline {
     agent {
        docker {
//             image 'alpine/git:2.43.0'
//             image 'openjdk:17-ea-oraclelinux8'
//             image 'gradle:8-jdk17-focal'
            image 'gradle:8.7-jdk17-focal'
        }
    }
//     agent any

    tools {
        // Install the Gradle version configured as "gradle-8.0.2" and add it to the path.
//         gradle "gradle-8.0.2"
        gradle "gradle-8.7"
    }

    options {
        // Timeout counter starts AFTER agent is allocated
        // timeout(time: 1, unit: 'SECONDS')
        disableConcurrentBuilds()
        disableResume()
        preserveStashes(buildCount: 5)
    }

    // https://e.printstacktrace.blog/jenkins-pipeline-environment-variables-the-definitive-guide/
    // https://www.cnblogs.com/bymo/p/7601519.html
    // https://www.unix.com/shell-programming-and-scripting/98834-search-replace-using-awk-variables.html#2
//     environment {
//         // some variables have been put to Jenkins -> System -> Environment variables
// //         NEXUS_CREDENTIALS_ID = 'nexus3-deployer-maven'
//         // NEXUS_CREDENTIALS_ID = 'nexus3-admin'
//
//         // NEXUS_HOSTNAME = 'localhost:38081'
// //         NEXUS_HOSTNAME = 'nexus3:8081'
//
// //         GITHUB_CREDENTIALS_ID = 'github-siukatech'
//         GITHUB_PROJECT_NAME = 'poc-react-backend-parent'
//         GITHUB_PROJECT_URL = "https://github.com/siukatech/poc-react-backend-parent.git"
// //         GITHUB_PROJECT_URL = "${GIT_URL}"
//
// //         GIT_USERNAME = 'siukatech'
// //         GIT_REPO_NAME = 'poc-react-backend-parent'
//
//     }

    stages {
        // Reference: 
        // https://devopscube.com/declarative-pipeline-parameters/
        stage('parameter') {
            steps {
                echo "[${STAGE_NAME}] config: ${config}"
                script {
                    properties([
                        parameters([
                            string(
                                // defaultValue: "poc-react-backend-parent", 
                                defaultValue: "${config.repoName}", 
                                name: 'PROJECT_NAME', 
                                description: 'Project Name',
                                trim: true
                            ),
                            string(
                                // defaultValue: 'https://github.com/siukatech/poc-react-backend-parent.git',
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
                // script {
                //     def userInput = input(
                //         id: 'userInput', message: 'Github Project Name and Url',
                //         parameters: [
                //                 string(defaultValue: 'poc-react-backend-parent',
                //                         description: 'Project Name',
                //                         name: 'ProjectName'),
                //                 string(defaultValue: 'https://github.com/siukatech/poc-react-backend-parent.git',
                //                         description: 'Project Url',
                //                         name: 'ProjectUrl'),
                //         ])

                //     // Save to variables. Default to empty string if not found.
                //     env.GITHUB_PROJECT_NAME = userInput.ProjectName?:''
                //     env.GITHUB_PROJECT_URL = userInput.ProjectUrl?:''
                // }

                sh '''
                    printenv
                '''

                echo "[${STAGE_NAME}] WORKSPACE: ${WORKSPACE}"
                echo "[${STAGE_NAME}] BUILD_NUMBER: ${BUILD_NUMBER}"

                // Get some code from a GitHub repository
                git(
                    branch: 'main',
                    changelog: false,
                    credentialsId: GITHUB_CREDENTIALS_ID,
                    poll: false,
                    url: GITHUB_PROJECT_URL
//                     url: GIT_URL
                )

                script {
                    def versionInfo = sh(script: "./gradlew properties | grep 'version' | head -1 | awk '{print \$2}'",
                        returnStdout: true)?.trim()
                    env.versionInfo = versionInfo
                    // echo "versionInfo: ${versionInfo}"

                    def artifactId = sh(script: "./gradlew properties | grep 'name' | head -1 | awk '{print \$2}'",
                        returnStdout: true)?.trim()
                    env.artifactId = artifactId

                    env.groupId = sh(script: "./gradlew properties | grep 'group' | head -1 | awk '{print \$2}'",
                        returnStdout: true)?.trim()
                    // env.groupId = groupId

                    env.tagSuffix = sh(script: '''
                        date +"%Y%m%d-%H%M%S-%Z"
                    ''', returnStdout: true)?.trim()

//                     env.projectName = sh(
// //                         script: "./gradlew properties | grep 'projectDir' | awk '{n=split(\$2,a,\'/\'); print a[n]}'",
//                         script: '''
// //                             gradle properties | grep 'projectDir' | awk '{n=split($2,a,"/"); print a[n]}'
//                             echo "${GITHUB_PROJECT_URL}" | awk '{n=split($2,a,"/"); print a[n]}'
//                         ''',
//                         returnStdout: true)?.trim()

                    withCredentials([
                        usernamePassword(
                            credentialsId: GITHUB_CREDENTIALS_ID,
                            passwordVariable: 'password',
                            usernameVariable: 'username'
                        )
                    ]) {
                        env.gitUsername = username
                        env.gitPassword = password
                    }

                    withCredentials([
                        usernamePassword(
                            credentialsId: NEXUS_CREDENTIALS_ID,
                            passwordVariable: 'password',
                            usernameVariable: 'username'
                        )
                    ]) {
                        env.nexusUsername = username
                        env.nexusPassword = password
                    }

//                     env.projectUrl = "${GITHUB_HOSTNAME}/${gitUsername}/${projectName}.git"

                }

                echo "[${STAGE_NAME}] currentStage: ${STAGE_NAME}"
                echo "[${STAGE_NAME}] groupId: ${groupId}"
                echo "[${STAGE_NAME}] artifactId: ${artifactId}"
                echo "[${STAGE_NAME}] versionInfo: ${versionInfo}"
                echo "[${STAGE_NAME}] tagSuffix: ${tagSuffix}"
                echo "[${STAGE_NAME}] gitUsername: ${gitUsername}"
//                 echo "[${STAGE_NAME}] projectName: ${projectName}"
//                 echo "[${STAGE_NAME}] projectUrl: ${projectUrl}"
//                 echo "[${STAGE_NAME}] nexusUsername: ${nexusUsername}"
//                 echo "[${STAGE_NAME}] nexusPassword: ${nexusPassword}"
            }
        }

        stage('build') {
            steps {
                script {
                    // echo "[${STAGE_NAME}] currentStage: ${STAGE_NAME}"
                    // echo "[${STAGE_NAME}] groupId: ${groupId}"
                    // echo "[${STAGE_NAME}] artifactId: ${artifactId}"
                    // echo "[${STAGE_NAME}] versionInfo: ${versionInfo}"

                    // Run gradle on a Unix agent.
                    // sh "./gradlew build"
                    // // sh "./gradlew clean build"
                    // Reference:
                    // https://stackoverflow.com/a/44231270
                    // https://www.baeldung.com/ops/jenkins-conditional-constructs
                    // https://www.baeldung.com/gradle-skip-tests
                    def skipTest = config.skipTest
                    if ( skipTest ) {
                        sh "./gradlew build -x test"
                    }
                    else {
                        sh "./gradlew build"
                    }

                    // sh "ls -la build/**/*.jar"

                    sh "cp \$(ls build/**/*.jar) build/"

                    sh "ls -la build/*.jar"
                    
                }
            }

//             post {
//                 // If Maven was able to run the tests, even if some of the test
//                 // failed, record the test results and archive the jar file.
//                 success {
//                     junit '**/target/surefire-reports/TEST-*.xml'
//                     archiveArtifacts 'target/*.jar'
//                 }
//             }
        }

//         stage('archive') {
//             steps {
//                 sh "ls -la build/**/*.jar"
//                 sh "ls -la ${WORKSPACE}/build/**/*.jar"
//
//                 archiveArtifacts artifacts: 'build/**/*.jar', onlyIfSuccessful: true
//             }
//         }

        stage('publish') {
            steps {
                sh "ls -la ${WORKSPACE}/build/*.jar"
                // sh "ls -la ${WORKSPACE}/build/**/*.jar"

                echo "[${STAGE_NAME}] currentStage: ${STAGE_NAME}"
                echo "[${STAGE_NAME}] groupId: ${groupId}"
                echo "[${STAGE_NAME}] artifactId: ${artifactId}"
                echo "[${STAGE_NAME}] versionInfo: ${versionInfo}"

                // script {
                //     // Find built artifact under target folder
                //     def filesByGlob = findFiles(glob: "build/**/*.jar");
                //     // Print some info from the artifact found
                //     echo "[${STAGE_NAME}] ${filesByGlob[0].name} ${filesByGlob[0].path} ${filesByGlob[0].directory} ${filesByGlob[0].length} ${filesByGlob[0].lastModified}"
                //     // Extract the path from the File found
                //     env.artifactPath = filesByGlob[0].path;
                //     // Assign to a boolean response verifying If the artifact name exists
                //     env.artifactExists = fileExists artifactPath;
                // }
                // echo "[${STAGE_NAME}] artifactPath: ${artifactPath}"
                // echo "[${STAGE_NAME}] artifactExists: ${artifactExists}"

                nexusArtifactUploader (
                    artifacts: [
                        [artifactId: artifactId,
                        classifier: '',
                        file: 'build/' + artifactId + '-' + versionInfo + '.jar',
                        // file: artifactPath,
                        type: 'jar'],
                        [artifactId: artifactId,
                        classifier: 'test-fixtures',
                        file: 'build/' + artifactId + '-' + versionInfo + '-' + 'test-fixtures' + '.jar',
                        type: 'jar']
                    ],
                    credentialsId: NEXUS_CREDENTIALS_ID,
                    groupId: groupId,
                    nexusUrl: NEXUS_HOSTNAME,
                    nexusVersion: 'nexus3',
                    protocol: 'http',
                    repository: 'maven-snapshots',
                    version: versionInfo
                )
            }
        }

        stage('tag') {
            // Reference:
            // https://stackoverflow.com/a/44231270
            when {
                branch 'main'
            }
            steps {
                script {
                    echo "[${STAGE_NAME}] GITHUB_PROJECT_NAME: ${GITHUB_PROJECT_NAME}"
                    echo "[${STAGE_NAME}] GITHUB_PROJECT_URL: ${GITHUB_PROJECT_URL}"

                    env.tagName = 'release-' + "${versionInfo}" + '-' + "${tagSuffix}" + '-' + "${BUILD_NUMBER}"
                    echo "[${STAGE_NAME}] tagName: ${tagName}"

                    withCredentials([
                        usernamePassword(
                            credentialsId: GITHUB_CREDENTIALS_ID,
                            passwordVariable: 'password',
                            usernameVariable: 'username'
                        )
                    ]) {

                    env.pushUrl = "${config.repoHttp}://${username}:${password}@${config.repoHostname}/${username}/${GITHUB_PROJECT_NAME}.git"
                    env.pushUrlWithoutPassword = "${config.repoHttp}://${username}:xxxxxx@${config.repoHostname}/${username}/${GITHUB_PROJECT_NAME}.git"
                    echo "[${STAGE_NAME}] pushUrlWithoutPassword: ${pushUrlWithoutPassword}"

                        // https://stackoverflow.com/a/52492319
//                     echo "USERNAME:PASSWORD@aaa" | awk -v srch="USERNAME" -v repl="$COMMAND_MODE" -v pw="${TERM}" '{sub(srch,repl,$0); sub(/PASSWORD/,pw,$0); print $0}'
//                         remoteUrl = sh(
//                             script: "echo \"${GITHUB_REMOTE_URL}/${gitUsername}/${GITHUB_PROJECT_NAME}.git\" | awk -v un=\"${gitUsername}\" -v pw=\"${gitPassword}\" '{sub(/USERNAME/,un,\$0); sub(/PASSWORD/,pw,\$0);print \$0}'",
//                             returnStdout: true)?.trim()

//                             git config --global user.email "user.email is not required"
//                             git push https://${username}:${password}@github.com/${username}/${GITHUB_PROJECT_NAME}.git --tags
//                             git push origin ${tagName}

                            // git push https://${username}:${password}@github.com/${username}/${GITHUB_PROJECT_NAME}.git --tags
                        sh('''
                            git config --global user.email "${GITHUB_CREDENTIALS_EMAIL}"
                            git config --global user.name "${username}"
                            git tag -a ${tagName} -m "Release version ${tagName}"
                            git push ${pushUrl} --tags
                        ''')

                    }

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
