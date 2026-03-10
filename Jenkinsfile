pipeline {
	agent any

	environment {
		// --- Docker & Harbor 配置 ---
		REGISTRY_URL = "192.168.23.128:80"
		PROJECT_NAME = "blog-backend"
		IMAGE_NAME = "blog-backend"
		DOCKER_IMAGE = "${REGISTRY_URL}/${PROJECT_NAME}/${IMAGE_NAME}"
		// 使用 BUILD_NUMBER 作为版本号
		DOCKER_TAG = "1.0.${BUILD_NUMBER}"

		// --- Git 自动更新配置 ---
		GIT_REPO_URL = "https://github.com/Bready1222/blog-backend.git"
		GIT_BRANCH = "master"
		GIT_CREDENTIALS_ID = "git-credentials-id"

		// ⚠️ 关键配置：Jenkins 专用的身份标识
		// 必须与下面 stage 中 git config 设置的 email 一致
		GIT_USER_NAME = "Jenkins Bot"
		GIT_USER_EMAIL = "jenkins@local.com"

		// 定义用于识别“自家提交”的特殊标记
		GIT_COMMIT_SKIP_FLAG = "[skip-ci-loop]"

		// K8s 文件相对路径
		K8S_FILE_PATH = "k8s/blog-backend-deploy.yaml"
	}

	stages {
		stage('0. 检查是否为由 Jenkins 触发的循环构建') {
			steps {
				script {
					echo '🔍 正在检查上一次提交来源，防止死循环...'

					// 获取最后一次提交的作者邮箱
					def lastAuthor = sh(script: 'git log -1 --pretty=format:"%ae"', returnStdout: true).trim()
					// 获取最后一次提交的信息
					def lastMessage = sh(script: 'git log -1 --pretty=format:"%s"', returnStdout: true).trim()

					echo "📝 检测到最新提交作者: ${lastAuthor}"
					echo "📝 检测到最新提交信息: ${lastMessage}"

					// 【核心逻辑】如果是 Jenkins 自己提交的，直接终止
					if (lastAuthor == env.GIT_USER_EMAIL || lastMessage.contains(env.GIT_COMMIT_SKIP_FLAG)) {
						echo "⚠️ 检测到当前构建是由 Jenkins 自己的 Push 触发的！"
						echo "🛑 为避免死循环，本次构建将直接跳过。"
						currentBuild.result = 'SUCCESS' // 标记为成功，避免发送失败通知
						return // 终止整个 Pipeline
					} else {
						echo "✅ 确认为人工提交或第三方触发，继续执行构建流程。"
					}
				}
			}
		}

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
					credentialsId: 'harbor-cred',
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
                            # 1. 配置 Git 用户信息 (必须与环境变量一致，以便下次被识别)
                            git config user.name "${GIT_USER_NAME}"
                            git config user.email "${GIT_USER_EMAIL}"

                            # 2. 解决 detached HEAD 问题
                            echo "🔄 切换到 ${GIT_BRANCH} 分支并拉取最新代码..."
                            git checkout -B ${GIT_BRANCH}
                            git pull origin ${GIT_BRANCH} --rebase

                            # 3. 执行替换
                            echo "🔄 执行替换：将镜像 Tag 更新为 ${DOCKER_TAG}"
                            sed -i "s|image: ${DOCKER_IMAGE}:.*|image: ${DOCKER_IMAGE}:${DOCKER_TAG}|g" ${K8S_FILE_PATH}

                            # 4. 检查是否有实际变动
                            if [ -n "\$(git status --porcelain ${K8S_FILE_PATH})" ]; then
                                echo "📤 检测到文件变更，准备提交..."

                                CREDENTIALIZED_URL="https://\${GIT_USER}:\${GIT_PASS}@github.com/Bready1222/blog-backend.git"

                                git add ${K8S_FILE_PATH}

                                # ⚠️ 关键：提交信息中包含特殊标记，供下一次构建识别
                                git commit -m "chore: auto-update image to ${DOCKER_TAG} ${GIT_COMMIT_SKIP_FLAG}"

                                echo "🚀 推送到远程分支 ${GIT_BRANCH}..."
                                git push \${CREDENTIALIZED_URL} HEAD:${GIT_BRANCH} || {
                                    echo "⚠️ 推送失败，尝试先 pull 再 push..."
                                    git pull \${CREDENTIALIZED_URL} ${GIT_BRANCH} --no-edit
                                    git push \${CREDENTIALIZED_URL} HEAD:${GIT_BRANCH}
                                }

                                echo "🎉 Git 更新成功！ArgoCD 即将检测到变更。"
                            else
                                echo "⚠️ 文件内容无变化，跳过 Git 提交。"
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
			deleteDir()
		}
		success {
			echo "🎉 全流程构建部署成功！镜像版本：${DOCKER_TAG}"
		}
		failure {
			echo '💥 构建失败，请检查上方日志定位问题。'
			deleteDir()
		}
	}
}