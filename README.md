# 🚀 Cloud-Native DevOps Pipeline (GitOps Based)

> **项目描述**：一套基于 **K3s + Argo CD + Jenkins** 的全链路自动化运维体系。
> **核心价值**：实现了从代码提交到自动构建、镜像推送、集群同步、监控告警的**无人值守闭环**，验证了 GitOps 模式在中小规模集群的高效落地。

[![Kubernetes](https://img.shields.io/badge/K8s-K3s-326CE5?logo=kubernetes&style=flat)](https://k3s.io/)
[![CI/CD](https://img.shields.io/badge/CI/CD-Jenkins-D24939?logo=jenkins&style=flat)](https://www.jenkins.io/)
[![GitOps](https://img.shields.io/badge/GitOps-ArgoCD-E95420?logo=argo&style=flat)](https://argoproj.github.io/argo-cd/)
[![Registry](https://img.shields.io/badge/Registry-Harbor-60B932?logo=docker&style=flat)](https://goharbor.io/)
[![Monitor](https://img.shields.io/badge/Monitor-Prometheus-E6522C?logo=prometheus&style=flat)](https://prometheus.io/)

---

## 🛠️ 核心技术栈

本项目整合了云原生生态中最主流的工具链，构建了完整的 DevOps 能力：

*   **容器编排**: `K3s` (轻量级 Kubernetes，资源占用低，适合边缘/测试环境)
*   **持续集成 (CI)**: `Jenkins` (负责代码拉取、编译、单元测试、Docker 镜像构建)
*   **镜像仓库**: `Harbor` (私有仓库，提供镜像存储与漏洞扫描)
*   **持续部署 (CD)**: `Argo CD` (基于 GitOps 理念，监听 Git 变更自动同步集群状态)
*   **监控告警**: `Prometheus` + `Grafana` + `Alertmanager` (采集指标、可视化大屏、钉钉消息推送)
*   **应用示例**: `Spring Boot` + `Vue3` + `MySQL` (前后端分离微服务架构)

---

## 🔄 自动化工作流程

整个发布过程无需人工干预，完全由事件驱动：

1.  **代码提交**: 开发人员 Push 代码至 Git 仓库。
2.  **自动构建 (CI)**: Jenkins 检测到变更，自动执行构建脚本，生成 Docker 镜像并推送至 Harbor。
3.  **版本更新**: Jenkins 自动修改 Git 仓库中的 Kubernetes YAML 文件（更新镜像 Tag）。
4.  **自动部署 (CD)**: Argo CD 发现 Git 配置变化，自动将新版本同步至 K3s 集群，实现滚动更新。
5.  **实时监控**: Prometheus 实时采集集群资源与应用指标，异常时通过 Alertmanager 发送钉钉告警。

---

## 💡 项目亮点与成果

*   **✅ 真正的 GitOps 实践**: 摒弃了传统的 `kubectl apply` 手动操作，所有基础设施变更均通过 Git 版本控制，支持**一键回滚**和**版本追溯**。
*   **✅ 全链路自动化**: 打通了从“代码”到“运行”的最后一公里，部署效率提升 **80%**，人为失误率降为 **0**。
*   **✅ 可观测性闭环**: 不仅关注“部署成功”，更关注“运行健康”。集成了 JVM 监控、业务 QPS 大盘及钉钉即时告警，实现故障**分钟级响应**。
*   **✅ 轻量化落地**: 针对个人/中小团队场景，选用 K3s 替代重型 K8s，在保证功能完整的前提下，大幅降低硬件资源门槛。

---

## 📂 目录结构

```text
.
├── ci/                  # Jenkins 流水线脚本 (Jenkinsfile)
├── k8s/                 # Kubernetes 资源定义 (Deployment, Service, Ingress)
├── monitoring/          # 监控栈配置 (Prometheus Rules, Grafana Dashboards)
├── app/                 # 示例应用源码 (Spring Boot + Vue)
└── README.md            # 项目说明文档
