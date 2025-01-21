pipeline {
	agent any

	environment {
		DOCKER_CREDENTIALS_ID = 'docker-cred'
		DOCKER_IMAGE = "158.160.46.211:5005/tutorial-app:001"
		HELM_CHART_PATH = './helm-chart'
		KUBE_CONFIG = '~/.kube/config' // Убедитесь, что путь правильный
	}

	stages {
		stage('Clone Repository') {
			steps {
				git branch: 'main', url: 'https://github.com/DmitryLosk/2test.git'
			}
		}
		stage('Build Docker Image') {
			steps {
				script {
					echo http
					docker.withRegistry('http://158.160.46.211:5005', DOCKER_CREDENTIALS_ID) {
						def customImage = docker.build(DOCKER_IMAGE)
						customImage.push()
					}
				}
			}
		}

		stage('Deploy with Helm') {
			steps {
				script {
					sh "helm upgrade --install myapp ${HELM_CHART_PATH} --set image.tag=${GIT_COMMIT}"
				}
			}
		}
	}

	triggers {
		githubPush()
	}
}
