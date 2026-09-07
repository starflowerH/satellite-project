# Git 网络环境、身份与多账号防卡死规范 (Git Environment & Identity Runbook)

> **重要性**：本文档记录了当前开发环境关于 GitHub 提交、网络代理与凭据管理器的关键配置与避坑指南，确保后续所有代码提交（Commit）与推送（Push）零失误、零挂起，并确保 GitHub 小绿点稳定计入用户主页。

---

## 一、 核心身份规范 (Author & Committer)

所有 Git 提交必须统一使用用户的真实 GitHub 账户，严禁使用任何 AI/Bot/默认无主邮箱：

- **Git User Name**: `starflowerH`
- **Git User Email**: `codel323529@163.com`

### 检查与强制设定命令
```bash
git config --local user.name "starflowerH"
git config --local user.email "codel323529@163.com"
```
在每次发起 `commit` 时，推荐带上明确声明：
```bash
git commit --author="starflowerH <codel323529@163.com>" -m "feat/fix: 提交说明"
```

---

## 二、 网络代理配置 (梯子端口 7895)

用户的本地科学上网客户端（Clash 等）运行在端口 **`7895`**。国内直连 GitHub HTTPS 经常超时或重置连接，因此 Git 操作必须走本地代理：

### 本地代理设定命令
```bash
git config --local http.proxy http://127.0.0.1:7895
git config --local https.proxy http://127.0.0.1:7895
```

### 验证代理连通性
```bash
git ls-remote origin
```
若正常配置，2 秒内即可返回远程分支的 HEAD Hash，表示代理畅通。

---

## 三、 多账号凭据冲突防挂起 (极其关键)

### 问题现象
本地 Windows 凭据管理器（Credential Manager）存有多个 GitHub 账号的历史凭据（如 `starflowerH` 与其他历史账号）。
若使用默认的裸地址 `https://github.com/starflowerH/satellite-project.git` 进行 `git push`，`git-credential-manager` 无法识别应该使用哪个账号，会在后台（GUI Session）弹出账号选择窗口，而在无交互式的终端环境中表现为**任务无限期卡死挂起（Last progress: never）**。

### 解决方案
必须在远程 URL 中显式嵌入目标账号名 `starflowerH@`：
```bash
git remote set-url origin https://starflowerH@github.com/starflowerH/satellite-project.git
```
这样 Git 会直接精准调用 `git:https://starflowerH@github.com` 的持久化 Token，实现 100% 静默免密秒级推送。

---

## 四、 SSH 与 HTTP CONNECT 备忘

- 本机虽然配置了 `~/.ssh/config`（使用 `connect.exe -H 127.0.0.1:7895 %h %p`），虽然 `ssh -T git@github.com` 握手成功，但在推送大体量 Git Pack 时，容易受到 HTTP CONNECT 代理中断（抛出 `Connection closed by UNKNOWN port 65535`）。
- **因此最佳且最稳定的推送途径是**：**HTTPS + 显式账号前缀 + 7895 本地代理**。

---

## 五、 一键初始化与核验脚本 (PowerShell)

在任何新工作区或环境重置时，一键执行以下代码即可全部就绪：

```powershell
# 1. 规范用户身份
git config --local user.name "starflowerH"
git config --local user.email "codel323529@163.com"

# 2. 绑定本地梯子代理 (7895)
git config --local http.proxy http://127.0.0.1:7895
git config --local https.proxy http://127.0.0.1:7895

# 3. 显式绑定账号防凭据弹窗挂起
git remote set-url origin https://starflowerH@github.com/starflowerH/satellite-project.git

# 4. 测试连通性
git ls-remote origin
```
