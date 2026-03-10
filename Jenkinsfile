pipeline {
	agent any

	environment {
		// --- Docker & Harbor 配置 ---
		REGISTRY_URL = "192.168.23.128:80"
		PROJECT_NAME = "blog-backend"
		IMAGE_NAME = "blog-backend"
		DOCKER_IMAGE = "${REGISTRY_URL}/${PROJECT_NAME}/${IMAGE_NAME}"
		// 使用 BUILD_NUMBER 作为版本号，每次构建自动递增
		DOCKER_TAG = "1.0.${BUILD_NUMBER}"

		// --- Git 自动更新配置 ---
		GIT_REPO_URL = "github.com/Bready1222/blog-backend.git"
		GIT_BRANCH = "master"
		// ⚠️ 请确保 Jenkins 凭证管理中有一个 ID 为 'git-credentials-id' 的凭证 (Username/Password)
		GIT_CREDENTIALS_ID = "git-credentials-id"

		// Git 提交信息
		GIT_USER_NAME = "Jenkins Bot"
		GIT_USER_EMAIL = "jenkins@local.com"

		// K8s 文件相对路径 (相对于项目根目录)
		K8S_FILE_PATH = "k8s/blog-backend-deploy.yaml"
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
                    echo "✅ 检查 Maven 版本:"
                    mvn -version

                    echo "🚀 开始构建 (跳过测试)..."
                    mvn clean package -DskipTests

                    echo "📦 检查构建产物:"
                    ls -lh target/*.jar || { echo "❌ 未找到 jar 包，构建失败"; exit 1; }
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
					credentialsId: 'harbor-cred', // ⚠️ 确保此凭证 ID 存在
					usernameVariable: 'HARBOR_USER',
					passwordVariable: 'HARBOR_PASS'
				)]) {
					sh """
                        echo "🔑 登录 Harbor..."
                        echo \${HARBOR_PASS} | docker login ${REGISTRY_URL} -u \${HARBOR_USER} --password-stdin

                        echo "🚀 推送镜像 ${DOCKER_IMAGE}:${DOCKER_TAG}..."
                        docker push ${DOCKER_IMAGE}:${DOCKER_TAG}

                        echo "🏷️ 同时推送 latest 标签..."
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
					withCredentials([usernamePassword(
						credentialsId: "${GIT_CREDENTIALS_ID}",
						usernameVariable: 'GIT_USER',
						passwordVariable: 'GIT_PASS'
					)]) {
						sh """
                            # 1. 配置 Git 用户信息
                            git config user.name "${GIT_USER_NAME}"
                            git config user.email "${GIT_USER_EMAIL}"

                            # 2. 显示替换前的内容
                            echo "🔍 原文件内容:"
                            if grep "image:" ${K8S_FILE_PATH}; then
                                echo "✅ 找到 image 字段"
                            else
                                echo "❌ 错误：未在 ${K8S_FILE_PATH} 中找到 image 字段"
                                exit 1
                            fi

                            # 3. 执行替换 (使用 | 分隔符避免 URL 中的 / 干扰)
                            echo "🔄 执行替换：将镜像 Tag 更新为 ${DOCKER_TAG}"
                            sed -i "s|image: ${DOCKER_IMAGE}:.*|image: ${DOCKER_IMAGE}:${DOCKER_TAG}|g" ${K8S_FILE_PATH}

                            # 4. 显示替换后的内容
                            echo "✅ 新文件内容:"
                            grep "image:" ${K8S_FILE_PATH}

                            # 5. 检查是否有实际变动
                            if [ -n "\$(git status --porcelain ${K8S_FILE_PATH})" ]; then
                                echo "📤 检测到文件变更，准备提交..."

                                # 构造带凭证的 URL (用于 push)
                                # 处理 https:// 开头，插入 user:pass@
                                CREDENTIALIZED_URL=\$(echo "${GIT_REPO_URL}" | sed 's|https://|https://\${GIT_USER}:\${GIT_PASS}@|')

                                git add ${K8S_FILE_PATH}
                                git commit -m "chore: auto-update image to ${DOCKER_TAG} [skip ci]"

                                echo "🚀 推送到远程分支 ${GIT_BRANCH}..."
                                # 如果推送失败，尝试拉取后再推 (防止并发冲突)
                                git push \${CREDENTIALIZED_URL} HEAD:${GIT_BRANCH} || {
                                    echo "⚠️ 推送失败，尝试先 pull 再 push..."
                                    git pull \${CREDENTIALIZED_URL} ${GIT_BRANCH} --no-edit
                                    git push \${CREDENTIALIZED_URL} HEAD:${GIT_BRANCH}
                                }

                                echo "🎉 Git 更新成功！ArgoCD 即将检测到变更并自动同步。"
                            else
                                echo "⚠️ 文件内容无变化 (可能是 Tag 重复)，跳过 Git 提交。"
                                echo "💡 提示：如果 ArgoCD 未更新，请检查部署配置是否强制拉取最新镜像。"
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
			// ✅ 修改点：使用原生 deleteDir() 替代需要插件的 cleanWs()
			// 这会删除当前 workspace 下的所有文件，达到清理目的
			deleteDir()
		}
		success {
			echo '🎉 全流程构建部署成功！镜像版本：${DOCKER_TAG}'
		}
		failure {
			echo '💥 构建失败，请检查上方日志定位问题。'
			// 即使失败也清理一下，防止脏数据影响下次构建
			deleteDir()
		}
	}
}