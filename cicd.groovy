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
	parameters {
		string(name: 'TAG_NAME', defaultValue: 'latest', description: 'Tag name for the Docker image')
	}
	stages {
		stage('Clone Repository') {
			steps {
				git branch: 'main', url: 'https://github.com/DmitryLosk/2test.git'
			}
		}
		stage('Install Helm') {
			steps {
				sh '''
curl https://baltocdn.com/helm/signing.asc | gpg --dearmor | sudo tee /usr/share/keyrings/helm.gpg > /dev/null
sudo apt-get install apt-transport-https --yes
echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/helm.gpg] https://baltocdn.com/helm/stable/debian/ all main" | sudo tee /etc/apt/sources.list.d/helm-stable-debian.list
sudo apt-get update
sudo apt-get install -y helm
'''
			}
		}
		stage('Build Docker Image') {
			steps {
				script {
					echo "http"
					docker.build("${DOCKER_IMAGE}:${params.TAG_NAME}")
					docker.withRegistry("", "${DOCKER_CREDENTIALS_ID}") {
						docker.image("${DOCKER_IMAGE}:${params.TAG_NAME}").push()
					}
				}
			}
		}

		stage('Deploy with Helm') {
			steps {
				script {
					sh """
helm upgrade --install ${HELM_RELEASE_NAME} ./helm-chart \
--namespace ${HELM_NAMESPACE} \
--set image.repository=${DOCKER_IMAGE} \
--set image.tag=${params.TAG_NAME}
"""
				}
			}
		}
	}

	triggers {
		githubPush()
	}
}
