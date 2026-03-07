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
                        # 定义 Maven 路径
                        MAVEN_BIN="${MAVEN_HOME}/bin/mvn"

                        # 策略：先检查自定义路径是否存在且可用，如果不行，再检查全局 mvn
                        # 这样避免 command -v 的误判

                        if [ -x "$MAVEN_BIN" ]; then
                            echo "✅ 发现已下载的 Maven (位于 $MAVEN_HOME)，直接使用。"
                            $MAVEN_BIN -version
                        elif command -v mvn >/dev/null 2>&1; then
                            # 双重确认：不仅 command -v 要找到，还要能运行
                            if mvn -version >/dev/null 2>&1; then
                                echo "✅ 发现系统安装的 Maven，直接使用。"
                                mvn -version
                            else
                                echo "⚠️ 检测到 mvn 命令但运行失败，将重新下载。"
                                FORCE_INSTALL=true
                            fi
                        else
                            echo "⚠️ 未检测到 Maven，开始下载安装..."
                            FORCE_INSTALL=true
                        fi

                        # 如果需要安装 (FORCE_INSTALL 被设置)
                        if [ "$FORCE_INSTALL" = true ] || [ ! -x "$MAVEN_BIN" ]; then
                            echo "🚀 正在下载 Apache Maven ${MAVEN_VERSION} ..."

                            # 清理旧文件以防万一
                            rm -rf ${MAVEN_HOME} /tmp/maven.tar.gz

                            # 下载 (如果官方源慢，取消下面注释使用阿里云源)
                            # curl -fsSL https://maven.aliyun.com/repository/public/org/apache/maven/apache-maven/${MAVEN_VERSION}/apache-maven-${MAVEN_VERSION}-bin.tar.gz -o /tmp/maven.tar.gz
                            curl -fsSL https://dlcdn.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz -o /tmp/maven.tar.gz

                            echo "📦 正在解压..."
                            tar -xzf /tmp/maven.tar.gz -C /tmp/

                            # 最终验证
                            if [ -x "$MAVEN_BIN" ]; then
                                echo "✅ Maven 安装成功！"
                                $MAVEN_BIN -version
                            else
                                echo "❌ Maven 安装失败，无法找到可执行文件。"
                                ls -la /tmp/apache-maven*/bin/ || true
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
                    # 确定使用的 mvn 命令路径
                    MAVEN_CMD="mvn"
                    MAVEN_BIN="${MAVEN_HOME}/bin/mvn"

                    # 优先使用我们下载的 Maven，因为它肯定在
                    if [ -x "$MAVEN_BIN" ]; then
                        MAVEN_CMD="$MAVEN_BIN"
                        echo "使用指定路径的 Maven: $MAVEN_CMD"
                    else
                        #  fallback 到系统 mvn (虽然理论上不会走到这，因为上一步保证了)
                        echo "使用系统 Maven: $MAVEN_CMD"
                    fi

                    # 执行编译
                    $MAVEN_CMD clean package -DskipTests

                    # 验证产物
                    echo "📂 检查构建产物..."
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