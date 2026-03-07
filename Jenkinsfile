pipeline {
	agent any

	environment {
		// 【修改点 1】你的 Harbor 地址（IP:端口）
		REGISTRY_URL = "192.168.23.128:80"
		PROJECT_NAME = "library"
		IMAGE_NAME = "blog-backend"
		DOCKER_IMAGE = "${REGISTRY_URL}/${PROJECT_NAME}/${IMAGE_NAME}"
		DOCKER_TAG = "${BUILD_NUMBER}"
	}

	stages {
		stage('Checkout Code') {
			steps {
				echo '🚀 Pulling code from Git...'
				checkout scm
			}
		}

		stage('Build Docker Image') {
			steps {
				echo '🔨 Building Docker image...'
				sh "docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} ."
			}
		}

		stage('Push to Harbor') {
			steps {
				echo '📤 Logging in and pushing to Harbor...'
				withCredentials([usernamePassword(
					credentialsId: 'harbor-cred',  // 【修改点 2】必须和你之前在 Jenkins 设置的凭据 ID 一致
					usernameVariable: 'HARBOR_USER',
					passwordVariable: 'HARBOR_PASS'
				)]) {
					sh """
                        echo ${HARBOR_PASS} | docker login ${REGISTRY_URL} -u ${HARBOR_USER} --password-stdin
                        docker push ${DOCKER_IMAGE}:${DOCKER_TAG}
                    """
				}
			}
		}
	}

	post {
		always {
			echo '🧹 Cleaning up local images...'
			sh "docker rmi ${DOCKER_IMAGE}:${DOCKER_TAG} || true"
		}
	}
}