pipeline {
	agent any

	environment {
		// Harbor 配置
		REGISTRY_URL = "192.168.23.128:80"
		PROJECT_NAME = "library"
		IMAGE_NAME = "blog-backend"
		DOCKER_IMAGE = "${REGISTRY_URL}/${PROJECT_NAME}/${IMAGE_NAME}"
		DOCKER_TAG = "${BUILD_NUMBER}"

		// Maven 镜像版本
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

		// 🔧【修改重点】这里不再使用 Groovy 变量拼接路径，而是全用 Shell 动态执行
		stage('🔍 查案：到底发生了什么') {
			steps {
				script {
					echo "📍 准备检查 Docker 挂载情况..."

					// 【修正】直接使用三引号，让 Shell 自己去执行 $(pwd)
					// 这样 Docker 客户端接收到的就是动态解析后的路径，通常能解决 DinD 挂载问题
					sh '''
                        echo "📂 Jenkins 环境下的文件列表:"
                        ls -la

                        echo "🐳 测试 Docker 挂载后的文件列表 (使用 $(pwd)):"
                        docker run --rm -v $(pwd):/test-check maven:3.8-eclipse-temurin-17 ls -la /test-check

                        echo "🔎 验证 pom.xml 是否存在:"
                        if docker run --rm -v $(pwd):/test-check maven:3.8-eclipse-temurin-17 test -f /test-check/pom.xml; then
                            echo "✅ 成功！Docker 容器内看到了 pom.xml"
                        else
                            echo "❌ 失败！Docker 容器内依然是空的"
                            echo "   当前 Shell pwd 是: $(pwd)"
                            exit 1
                        fi
                    '''
				}
			}
		}

		// 2. 编译代码
		stage('Compile with Maven') {
			steps {
				echo '🔨 Step 2: Compiling Java code...'
				// 这里的写法是正确的，保留即可
				sh '''
                    echo "当前目录: $(pwd)"
                    docker run --rm -v $(pwd):/app maven:3.8-eclipse-temurin-17 mvn clean package -DskipTests
                '''
			}
		}

		// 3. 构建 Docker 镜像
		stage('Build Docker Image') {
			steps {
				echo '🐳 Step 3: Building Docker image...'
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