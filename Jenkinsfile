pipeline {
	agent any

	environment {
		REGISTRY_URL = "192.168.23.128:80"
		PROJECT_NAME = "blog-backend"
		IMAGE_NAME = "blog-backend"
		DOCKER_IMAGE = "${REGISTRY_URL}/${PROJECT_NAME}/${IMAGE_NAME}"
		DOCKER_TAG = "1.0.${BUILD_NUMBER}"
	}

	stages {
		stage('1. 拉取代码') {
			steps {
				echo '📥 正在从 Git 拉取代码...'
				checkout scm
			}
		}

		stage('2. Maven 编译打包') {
			steps {
				echo '🔨 开始编译 Java 项目...'
				sh '''
                    echo "✅ 使用系统内置 Maven:"
                    mvn -version

                    echo "🚀 开始构建..."
                    # 直接使用 mvn 命令，无需指定绝对路径
                    mvn clean package -DskipTests

                    echo "📦 检查构建产物:"
                    ls -lh target/*.jar
                '''
			}
		}

		stage('3. 构建 Docker 镜像') {
			steps {
				echo '🐳 构建 Docker 镜像...'
				sh "docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} ."
			}
		}

		stage('4. 推送镜像到 Harbor') {
			steps {
				echo '📤 推送镜像到私有仓库...'
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
			echo '🧹 清理临时文件...'
			// ❌ 删除了 rm -f /tmp/maven.tar.gz，因为不再下载了
			sh "docker rmi ${DOCKER_IMAGE}:${DOCKER_TAG} || true"
		}
		success {
			echo '🎉 构建成功！'
		}
		failure {
			echo '💥 构建失败。'
		}
	}
}