pipeline {
	agent any

	environment {
		REGISTRY_URL = "192.168.23.128:80"
		PROJECT_NAME = "library"
		IMAGE_NAME = "blog-backend"
		DOCKER_IMAGE = "${REGISTRY_URL}/${PROJECT_NAME}/${IMAGE_NAME}"
		DOCKER_TAG = "${BUILD_NUMBER}"

		MAVEN_VERSION = "3.9.6"
		MAVEN_HOME = "/tmp/apache-maven-${MAVEN_VERSION}"
	}

	stages {
		stage('1. 拉取代码') {
			steps {
				echo '📥 正在从 Git 拉取代码...'
				checkout scm
			}
		}

		stage('2. 准备环境 (强制检查并安装 Maven)') {
			steps {
				script {
					echo '🔍 强制检查 Maven 环境...'
					sh '''
                        MAVEN_BIN="${MAVEN_HOME}/bin/mvn"

                        # 检查逻辑保持不变
                        if [ -x "$MAVEN_BIN" ]; then
                            echo "✅ 发现已下载的 Maven，直接使用。"
                            $MAVEN_BIN -version
                        elif command -v mvn >/dev/null 2>&1 && mvn -version >/dev/null 2>&1; then
                            echo "✅ 发现系统安装的 Maven，直接使用。"
                            mvn -version
                        else
                            echo "⚠️ 未检测到 Maven，开始下载安装..."
                            FORCE_INSTALL=true
                        fi

                        if [ "$FORCE_INSTALL" = true ] || [ ! -x "$MAVEN_BIN" ]; then
                            echo "🚀 正在下载 Apache Maven ${MAVEN_VERSION} (阿里云镜像) ..."

                            rm -rf ${MAVEN_HOME} /tmp/maven.tar.gz

                            # 【关键修改】使用阿里云镜像源，稳定不 404
                            ALIYUN_URL="https://maven.aliyun.com/repository/public/org/apache/maven/apache-maven/${MAVEN_VERSION}/apache-maven-${MAVEN_VERSION}-bin.tar.gz"

                            echo "下载链接: ${ALIYUN_URL}"
                            curl -fsSL "${ALIYUN_URL}" -o /tmp/maven.tar.gz

                            if [ $? -ne 0 ]; then
                                echo "❌ 下载失败，请检查网络或 Maven 版本号。"
                                exit 1
                            fi

                            echo "📦 正在解压..."
                            tar -xzf /tmp/maven.tar.gz -C /tmp/

                            if [ -x "$MAVEN_BIN" ]; then
                                echo "✅ Maven 安装成功！"
                                $MAVEN_BIN -version
                            else
                                echo "❌ 解压后未找到可执行文件。"
                                ls -la /tmp/ | grep maven
                                exit 1
                            fi
                        fi
                    '''
				}
			}
		}

		stage('3. Maven 编译打包') {
			steps {
				echo '🔨 开始编译 Java 项目...'
				sh '''
                    MAVEN_CMD="${MAVEN_HOME}/bin/mvn"
                    if [ ! -x "$MAVEN_CMD" ]; then
                        MAVEN_CMD="mvn"
                    fi
                    echo "使用 Maven: $MAVEN_CMD"
                    $MAVEN_CMD clean package -DskipTests
                    ls -lh target/*.jar
                '''
			}
		}

		stage('4. 构建 Docker 镜像') {
			steps {
				echo '🐳 构建 Docker 镜像...'
				sh "docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} ."
			}
		}

		stage('5. 推送镜像到 Harbor') {
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
			sh 'rm -f /tmp/maven.tar.gz || true'
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