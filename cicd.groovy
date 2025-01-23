pipeline {
	agent any

	environment {
		DOCKER_CREDENTIALS_ID = 'docker-cred'
		DOCKER_IMAGE = "dmitrylosk/tutorial-app"
		NEXUS_REPO = 'http://158.160.46.211:5005'
		HELM_CHART_PATH = './helm-chart'
		HELM_RELEASE_NAME = 'app'
		HELM_NAMESPACE = 'dplm'
		KUBE_CONFIG = '~/.kube/config' // Убедитесь, что путь правильный
	}
	triggers {
		githubPush()
	}
	/*parameters {
		string(name: 'TAG_NAME', defaultValue: 'latest', description: 'Tag name for the Docker image')
	}*/
	stages {
		stage('Clone Repository') {
			steps {
				git branch: 'main', url: 'https://github.com/DmitryLosk/2test.git'
			}
		}
		stage('Build Docker Image') {
			steps {
				script {
					echo "http"
					def tag = env.GIT_TAG ?: 'latest'

					docker.withRegistry("", "${DOCKER_CREDENTIALS_ID}") {
						docker.build("${DOCKER_IMAGE}:${tag}")
						docker.image("${DOCKER_IMAGE}:${tag}").push()
					}
					echo "Docker image successfully built and pushed with tag: ${tag}"
				}
			}
		}

		stage('Deploy with Helm') {
			steps {
				script {
					def tag = env.GIT_TAG ?: 'latest'
					sh """
helm upgrade --install ${HELM_RELEASE_NAME} ./helm-chart \
--namespace ${HELM_NAMESPACE} \
--set image.repository=${DOCKER_IMAGE} \
--set image.tag=${tag}
"""
				}
			}
		}
	}
	post {
		success {
			echo "Docker image successfully built and pushed with tag: ${tag}"
		}
		failure {
			echo "Build failed."
		}
	}
}
