pipeline {
	agent any

	environment {
		// Harbor 配置
		REGISTRY_URL = "192.168.23.128:80"
		PROJECT_NAME = "library"
		IMAGE_NAME = "blog-backend"
		DOCKER_IMAGE = "${REGISTRY_URL}/${PROJECT_NAME}/${IMAGE_NAME}"
		DOCKER_TAG = "${BUILD_NUMBER}"

		// Maven 镜像版本 (使用包含 JDK 的镜像以便编译)
		MAVEN_IMAGE = "maven:3.8-eclipse-temurin-17"
	}

	stages {
		// 1. 拉取代码
		stage('Checkout Code') {
			steps {
				echo '🚀 Step 1: Pulling code from Git...'
				checkout scm
			}
		}

		// 👇👇👇 【新增】关键步骤：执行 Maven 打包 👇👇👇
		stage('Compile with Maven') {
			steps {
				echo '🔨 Step 2: Compiling Java code to generate .jar file...'
				script {
					// 使用 Docker 运行 Maven，避免在 Jenkins 宿主机安装 Java/Maven
					// -v $WORKSPACE:/app : 将当前工作区挂载到容器内，这样生成的 jar 包会留在宿主机上供下一步使用
					// -w /app : 设置工作目录
					sh """
                        docker run --rm \
                            -v $WORKSPACE:/app \
                            -w /app \
                            ${MAVEN_IMAGE} \
                            mvn clean package -DskipTests
                    """
				}
			}
		}
		// 👆👆👆 新增结束 👆👆👆

		// 2. 构建 Docker 镜像 (此时 target 目录下已经有 jar 包了)
		stage('Build Docker Image') {
			steps {
				echo '🐳 Step 3: Building Docker image...'
				sh "docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} ."
			}
		}

		// 3. 推送到 Harbor
		stage('Push to Harbor') {
			steps {
				echo '📤 Step 4: Logging in and pushing to Harbor...'
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
			sh "docker rmi ${DOCKER_IMAGE}:${DOCKER_TAG} || true"
		}
		failure {
			echo '❌ Build failed! Check console output.'
		}
		success {
			echo '✅ Build successful! Image pushed to Harbor.'
		}
	}
}