// SiteWatch CI/CD pipeline (pipeline as code).
// Needs on the Jenkins agent: JDK 17, Maven 3.6+, Docker with the compose plugin.
// The jenkins/ folder contains a ready-made Jenkins image that has all of these.
pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    // Fires when the GitHub webhook (http://<jenkins>/github-webhook/) reports a push.
    triggers { githubPush() }

    parameters {
        booleanParam(name: 'PUSH_IMAGE', defaultValue: false, description: 'Push the Docker image to Docker Hub')
        string(name: 'DOCKERHUB_USER', defaultValue: 'your-dockerhub-username', description: 'Docker Hub username')
    }

    environment {
        IMAGE = 'sitewatch'
        TAG   = "${env.BUILD_NUMBER}"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                sh 'git log -1 --pretty=format:"%h %an: %s"'
            }
        }

        stage('Compile') {
            steps { sh 'mvn -B -ntp clean compile' }
        }

        stage('Test') {
            steps { sh 'mvn -B -ntp test' }
            post { always { junit 'target/surefire-reports/*.xml' } }
        }

        stage('Package') {
            steps { sh 'mvn -B -ntp -DskipTests package' }
            post { success { archiveArtifacts artifacts: 'target/sitewatch.jar', fingerprint: true } }
        }

        stage('Docker Build') {
            steps { sh 'docker build -t ${IMAGE}:${TAG} -t ${IMAGE}:latest .' }
        }

        stage('Push Image') {
            when { expression { return params.PUSH_IMAGE } }
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub', usernameVariable: 'DH_USER', passwordVariable: 'DH_PASS')]) {
                    sh '''
                        echo "$DH_PASS" | docker login -u "$DH_USER" --password-stdin
                        docker tag ${IMAGE}:${TAG} ${DOCKERHUB_USER}/${IMAGE}:${TAG}
                        docker tag ${IMAGE}:${TAG} ${DOCKERHUB_USER}/${IMAGE}:latest
                        docker push ${DOCKERHUB_USER}/${IMAGE}:${TAG}
                        docker push ${DOCKERHUB_USER}/${IMAGE}:latest
                    '''
                }
            }
        }

        // Only the main branch is deployed; develop and feature branches stop after the image build.
        stage('Deploy') {
            when { expression { return env.GIT_BRANCH ==~ /(origin\/)?main/ } }
            steps { sh 'docker compose up -d --build' }
        }
    }

    post {
        success { echo "Build #${env.BUILD_NUMBER} succeeded." }
        failure { echo "Build #${env.BUILD_NUMBER} FAILED. Check the stage that turned red." }
        always  { sh 'docker image prune -f || true' }
    }
}
