pipeline {
	agent any

	environment {
		// --- 配置区域 (请确认这些和你之前的一致) ---
		REGISTRY_URL = "192.168.23.128:80"
		PROJECT_NAME = "library"
		IMAGE_NAME = "blog-backend"
		DOCKER_IMAGE = "${REGISTRY_URL}/${PROJECT_NAME}/${IMAGE_NAME}"
		DOCKER_TAG = "${BUILD_NUMBER}"

		// Maven 版本配置
		MAVEN_VERSION = "3.9.6"
		MAVEN_HOME = "/tmp/apache-maven-${MAVEN_VERSION}"
		// ------------------------------------------
	}

	stages {
		stage('1. 拉取代码') {
			steps {
				echo '📥 正在从 Git 拉取代码...'
				checkout scm
			}
		}

		stage('2. 准备环境 (自动安装 Maven)') {
			steps {
				script {
					echo '🔍 检查 Java 和 Maven...'
					sh '''
                        # 1. 显示 Java 版本
                        java -version

                        # 2. 检查 Maven 是否存在
                        if command -v mvn &> /dev/null; then
                            echo "✅ Maven 已存在，跳过下载。"
                            mvn -version
                        else
                            echo "⚠️ 未检测到 Maven，正在下载并配置..."

                            # 下载 Maven (使用 Apache 官方源，如果慢可换阿里云)
                            # 如果下载失败，请告诉我，我帮你换阿里云镜像
                            curl -fsSL https://dlcdn.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz -o /tmp/maven.tar.gz

                            # 解压到 /tmp
                            tar -xzf /tmp/maven.tar.gz -C /tmp/

                            # 验证解压结果
                            if [ -d "${MAVEN_HOME}" ]; then
                                echo "✅ Maven 下载并解压成功！"
                                ${MAVEN_HOME}/bin/mvn -version
                            else
                                echo "❌ Maven 解压失败，目录不存在：${MAVEN_HOME}"
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
                    # 关键：确保当前 shell 能用到刚才下载的 Maven
                    # 如果上一步下载了，这里必须指定路径；如果原本就有，直接用 mvn 也行
                    MVN_CMD="mvn"
                    if [ ! -command -v mvn &> /dev/null ]; then
                        MVN_CMD="${MAVEN_HOME}/bin/mvn"
                    fi

                    # 执行编译 (跳过测试以加快速度，如需测试去掉 -DskipTests)
                    $MVN_CMD clean package -DskipTests

                    # 检查 jar 包是否生成
                    if ls target/*.jar >/dev/null 2>&1; then
                        echo "✅ 编译成功！生成的 Jar 包："
                        ls -lh target/*.jar
                    else
                        echo "❌ 编译失败：未找到 target/*.jar"
                        exit 1
                    fi
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
				// 确保你已经在 Jenkins 凭据里配置了 'harbor-cred'
				withCredentials([usernamePassword(
					credentialsId: 'harbor-cred',
					usernameVariable: 'HARBOR_USER',
					passwordVariable: 'HARBOR_PASS'
				)]) {
					sh """
                        echo "正在登录 Harbor..."
                        echo \${HARBOR_PASS} | docker login ${REGISTRY_URL} -u \${HARBOR_USER} --password-stdin

                        echo "正在推送镜像..."
                        docker push ${DOCKER_IMAGE}:${DOCKER_TAG}

                        echo "✅ 推送完成！"
                    """
				}
			}
		}
	}

	post {
		always {
			echo '🧹 清理临时文件...'
			// 清理下载的 Maven 压缩包，节省空间
			sh 'rm -f /tmp/maven.tar.gz || true'
			// 可选：清理本地构建的镜像，防止磁盘爆满
			sh "docker rmi ${DOCKER_IMAGE}:${DOCKER_TAG} || true"
		}
		success {
			echo '🎉 恭喜！构建全流程成功！'
		}
		failure {
			echo '💥 构建失败，请检查上方日志。'
		}
	}
}