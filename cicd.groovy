pipeline {
	agent any

	environment {
		TAG_NAME = "${env.GIT_TAG_NAME}"
		DOCKER_CREDENTIALS_ID = 'docker-cred'
		DOCKER_IMAGE = "dmitrylosk/tutorial-app"
		HELM_RELEASE_NAME = 'myapp'
		HELM_NAMESPACE = 'dplm'
		KUBECONFIG = credentials('kubeconfig')
	}
	triggers {
		githubPush()
	}
	/*parameters {
		string(name: 'TAG_NAME', defaultValue: 'latest', description: 'Tag name for the Docker image')
	}*/
	stages {
		stage('Checkout') {
			steps {
				script {
					git tag-- sort = -creatordate | head - n 1
					latestTag = sh(returnStdout: true, script: "git tag --sort=-creatordate | head -n 1").trim()
					echo latestTag
					checkout scm: [$class: 'GitSCM', userRemoteConfigs: [[url: 'https://github.com/DmitryLosk/2test.git']], branches: [[name: "refs/tags/${env.GIT_TAG}"]]], poll: false
				}
			}
		}
		/*stage('Clone Repository') {
			steps {
				git branch: 'main', url: 'https://github.com/DmitryLosk/2test.git'
			}
		}*/
		stage('Build Docker Image') {
			steps {
				script {

					def TAG_NAME = env.GIT_TAG ?: 'latest'
					echo TAG_NAME
					docker.withRegistry("", "${DOCKER_CREDENTIALS_ID}") {
						docker.build("${DOCKER_IMAGE}:${TAG_NAME}")
						docker.image("${DOCKER_IMAGE}:${TAG_NAME}").push()
					}
					echo "Docker image successfully built and pushed with tag: ${TAG_NAME}"
				}
			}
		}

		stage('Deploy with Helm') {
			steps {
				script {
					echo "Helm"
					def TAG_NAME = env.GIT_TAG ?: 'latest'
					sh """
helm upgrade --install ${HELM_RELEASE_NAME} ./myapp \
--namespace ${HELM_NAMESPACE} \
--set image.repository=${DOCKER_IMAGE} \
--set image.tag=${TAG_NAME} \
--kubeconfig $KUBECONFIG
"""
				}
			}
		}
	}
	post {
		success {
			echo "Docker image successfully built and pushed with tag "
		}
		failure {
			echo "Build failed."
		}
	}
}
