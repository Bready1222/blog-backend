pipeline {
	agent any

	environment {
		// Harbor 配置
		REGISTRY_URL = "192.168.23.128:80"
		PROJECT_NAME = "library"
		IMAGE_NAME = "blog-backend"
		DOCKER_IMAGE = "${REGISTRY_URL}/${PROJECT_NAME}/${IMAGE_NAME}"
		DOCKER_TAG = "${BUILD_NUMBER}"
	}

	stages {
		// 1. 拉取代码
		stage('Checkout Code') {
			steps {
				echo '🚀 Step 1: Pulling code from Git...'
				checkout scm
			}
		}

		// 2. 编译代码 (直接使用 Jenkins 环境中的 Maven)
		stage('Compile with Maven') {
			steps {
				echo '🔨 Step 2: Compiling Java code (Native)...'

				// 先检查环境，确保 mvn 可用
				sh 'mvn -version'

				// 执行编译
				// 不需要 docker run，直接在当前容器运行，文件直接生成在 workspace 中
				sh 'mvn clean package -DskipTests'

				// 验证产物
				sh 'ls -la target/*.jar'
			}
		}

		// 3. 构建 Docker 镜像
		stage('Build Docker Image') {
			steps {
				echo '🐳 Step 3: Building Docker image...'
				// 此时 target 目录下已经有 jar 包了，直接构建
				sh "docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} ."
			}
		}

		// 4. 推送到 Harbor
		stage('Push to Harbor') {
			steps {
				echo '📤 Step 4: Pushing to Harbor...'
				withCredentials([usernamePassword(
					credentialsId: 'harbor-cred',
					usernameVariable: 'HARBOR_USER',
					passwordVariable: 'HARBOR_PASS'
				)]) {
					sh """
                        echo \${HARBOR_PASS} | docker login ${REGISTRY_URL} -u \${HARBOR_USER} --password-stdin
                        docker push ${DOCKER_IMAGE}:${DOCKER_TAG}
                    """
				}
			}
		}
	}

	post {
		always {
			echo '🧹 Cleaning up local images...'
			// 清理构建产生的镜像，节省空间
			sh "docker rmi ${DOCKER_IMAGE}:${DOCKER_TAG} || true"
		}
		failure {
			echo '❌ Build failed! Check console output for details.'
			// 可选：发送通知邮件或钉钉消息
		}
		success {
			echo '✅ Build successful! Image pushed to Harbor.'
			echo "Image: ${DOCKER_IMAGE}:${DOCKER_TAG}"
		}
	}
}