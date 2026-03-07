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
		stage('🔍 查案：到底发生了什么') {
			steps {
				script {
					// 1. 打印当前 Jenkins 工作区在哪里
					def workDir = sh(script: 'pwd', returnStdout: true).trim()
					echo "📍 Jenkins 当前工作区绝对路径: ${workDir}"

					// 2. 【关键】在启动 Docker 之前，先在 Jenkins 环境里列出文件
					echo "📂 Jenkins 环境下的文件列表:"
					sh "ls -la ${workDir}"

					// 3. 【关键】尝试启动一个临时容器，只为了列出文件，不运行 Maven
					echo "🐳 测试 Docker 挂载后的文件列表:"
					sh """
                docker run --rm -v ${workDir}:/test-check maven:3.8-eclipse-temurin-17 ls -la /test-check
            """

					// 如果上面一步列出的文件里没有 pom.xml，那就实锤是挂载问题或拉取问题了
					sh """
                if docker run --rm -v ${workDir}:/test-check maven:3.8-eclipse-temurin-17 test -f /test-check/pom.xml; then
                    echo "✅ 确认：Docker 容器内能看到 pom.xml"
                else
                    echo "❌ 致命发现：Docker 容器内看不到 pom.xml！"
                    echo "   这说明要么代码没拉下来，要么 Docker 挂载路径错了。"
                    exit 1
                fi
            """
				}
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
					sh '''
                    echo "当前目录: $(pwd)"
                    docker run --rm -v $(pwd):/app maven:3.8-eclipse-temurin-17 mvn clean package -DskipTests
                '''
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