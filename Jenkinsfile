pipeline {
	agent any

	environment {
		// --- Docker & Harbor 配置 ---
		REGISTRY_URL = "192.168.23.128:80"
		PROJECT_NAME = "blog-backend"
		IMAGE_NAME = "blog-backend"
		DOCKER_IMAGE = "${REGISTRY_URL}/${PROJECT_NAME}/${IMAGE_NAME}"
		DOCKER_TAG = "1.0.${BUILD_NUMBER}"
		// --- Git 自动更新配置 (⚠️ 请根据实际情况修改这里) ---
		// 你的 Git 仓库 HTTP 地址 (不要用 SSH，因为要用账号密码推送)
		GIT_REPO_URL = "https://github.com/Bready1222/blog-backend.git"
		GIT_BRANCH = "master" // 或者 "main"，看你仓库的主分支名
		// Jenkins 中配置的凭证 ID (必须包含 Git 用户名和 访问令牌/密码)
		GIT_CREDENTIALS_ID = "git-credentials-id"

		// Git 提交时的用户信息
		GIT_USER_NAME = "Jenkins Bot"
		GIT_USER_EMAIL = "jenkins@local.com"

		// K8s 文件路径 (根据你的项目结构调整)
		K8S_FILE_PATH = "k8s/blog-backend-deploy.yaml"
	}

	stages {
		stage('1. 拉取代码') {
			steps {
				echo '📥 正在从 Git 拉取代码...'
				// 使用 checkout scm 拉取当前任务关联的代码
				checkout scm
			}
		}

		stage('2. Maven 编译打包') {
			steps {
				echo '🔨 开始编译 Java 项目...'
				sh '''
                    echo "✅ 检查 Maven 版本:"
                    mvn -version

                    echo "🚀 开始构建 (跳过测试)..."
                    mvn clean package -DskipTests

                    echo "📦 检查构建产物:"
                    ls -lh target/*.jar || echo "⚠️ 未找到 jar 包，请检查 pom.xml"
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
                        echo "🔑 登录 Harbor..."
                        echo \${HARBOR_PASS} | docker login ${REGISTRY_URL} -u \${HARBOR_USER} --password-stdin

                        echo "🚀 推送镜像 ${DOCKER_IMAGE}:${DOCKER_TAG}..."
                        docker push ${DOCKER_IMAGE}:${DOCKER_TAG}

                        # 可选：同时也推一个 latest 标签，方便本地调试，但生产主要靠具体版本号
                        docker tag ${DOCKER_IMAGE}:${DOCKER_TAG} ${DOCKER_IMAGE}:latest
                        docker push ${DOCKER_IMAGE}:latest
                    """
				}
			}
		}

		stage('5. 自动更新 Git 配置 (触发 ArgoCD)') {
			steps {
				echo '📝 正在更新 k8s/deployment.yaml 并推送到 Git...'
				script {
					// 使用 Git 凭证执行推送操作
					withCredentials([usernamePassword(
						credentialsId: "${GIT_CREDENTIALS_ID}",
						usernameVariable: 'GIT_USER',
						passwordVariable: 'GIT_PASS'
					)]) {
						sh """
                            # 1. 配置 Git 用户信息
                            git config user.name "${GIT_USER_NAME}"
                            git config user.email "${GIT_USER_EMAIL}"

                            # 2. 核心步骤：替换 YAML 中的镜像 Tag
                            # 逻辑：查找 image: 开头且包含镜像名的行，将 : 后面的内容替换为新 Tag
                            echo "🔍 原文件内容:"
                            grep "image:" ${K8S_FILE_PATH} || echo "⚠️ 未找到 image 字段，请检查路径 ${K8S_FILE_PATH}"

                            echo "🔄 执行替换: ${DOCKER_TAG}"
                            sed -i "s|image: ${DOCKER_IMAGE}:.*|image: ${DOCKER_IMAGE}:${DOCKER_TAG}|g" ${K8S_FILE_PATH}

                            echo "✅ 新文件内容:"
                            grep "image:" ${K8S_FILE_PATH}

                            # 3. 检查是否有变动，有则提交
                            if [ -n "\$(git status --porcelain ${K8S_FILE_PATH})" ]; then
                                echo "📤 检测到变更，正在提交..."

                                # 构造带凭证的 URL (用于 push)
                                # 注意：如果 URL 中已有 http://，需处理一下防止双斜杠，这里假设 GIT_REPO_URL 是完整的
                                CREDENTIALIZED_URL=\$(echo ${GIT_REPO_URL} | sed 's|http://|http://\${GIT_USER}:\${GIT_PASS}@|')

                                git add ${K8S_FILE_PATH}
                                git commit -m "chore: auto-update image to ${DOCKER_TAG} [skip ci]"

                                echo "🚀 推送到分支 ${GIT_BRANCH}..."
                                git push \${CREDENTIALIZED_URL} HEAD:${GIT_BRANCH}

                                echo "🎉 Git 更新成功！ArgoCD 即将自动同步。"
                            else
                                echo "⚠️ 文件没有变化 (可能 Tag 重复或替换失败)，跳过提交。"
                            fi
                        """
					}
				}
			}
		}
	}

	post {
		always {
			echo '🧹 清理工作空间...'
			cleanWs()
		}
		success {
			echo '🎉 全流程构建部署成功！'
		}
		failure {
			echo '💥 构建失败，请检查日志。'
		}
	}
}