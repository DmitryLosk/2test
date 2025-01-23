pipeline {
	agent any

	environment {
		TAG_NAME = ''
		DOCKER_CREDENTIALS_ID = 'docker-cred'
		DOCKER_IMAGE = "dmitrylosk/tutorial-app"
		HELM_RELEASE_NAME = 'myapp'
		HELM_NAMESPACE = 'dplm'
		KUBECONFIG = credentials('kubeconfig')
	}
	triggers {
		githubPush()
	}
	stages {
		stage('Checkout') {
			steps {
				script {
					latestTag = sh(returnStdout: true, script: "git tag --sort=-creatordate | head -n 1").trim()
					echo latestTag
					TAG_NAME = latestTag
					checkout scm: [$class: 'GitSCM', userRemoteConfigs: [[url: 'https://github.com/DmitryLosk/2test.git']], branches: [[name: "refs/tags/${latestTag}"]]], poll: false

				}
			}
		}
		stage('Build Docker Image') {
			steps {
				script {
					echo TAG_NAME
					docker.withRegistry("", "${DOCKER_CREDENTIALS_ID}") {
						docker.build("${DOCKER_IMAGE}:${TAG_NAME}")
						docker.image("${DOCKER_IMAGE}:${TAG_NAME}").push()
						sh 'docker system prune -af'
					}
					echo "Docker image successfully built and pushed with tag: ${TAG_NAME}"
				}
			}
		}
		stage('Deploy with Helm') {
			steps {
				script {
					echo "Helm"
					sh """
helm upgrade --install ${HELM_RELEASE_NAME} ./myapp \
--namespace ${HELM_NAMESPACE} \
--set image.repository=${DOCKER_IMAGE} \
--set image.tag=${TAG_NAME} \
--kubeconfig $KUBECONFIG
"""
				cleanWs()
				}
			}
		}
	}
	post {
		success {
			echo "Successfully built and pushed "
		}
		failure {
			echo "Build failed."
		}
	}
}
