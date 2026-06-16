# 项目运行指南

这份文档用于指导评委在一台“没有提前配置开发环境”的电脑上，从零开始运行本项目前端代码。

本项目是一个前端项目，技术栈是 `Vue 3 + TypeScript + Vite`。真正的前端源码目录在：

```text
.codebuddy/sate
```

也就是说，运行命令时不要停留在项目最外层目录，而是要先进入 `.codebuddy/sate`。

## 1. 需要准备什么

### 1.1 必须安装的软件

需要安装：

1. Node.js
2. npm
3. 浏览器，推荐 Chrome 或 Edge

其中 `npm` 会随 Node.js 一起安装，一般不需要单独安装。
下载链接：https://nodejs.org/zh-cn/download
### 1.2 推荐 Node.js 版本

本项目使用的 Vite 版本要求 Node.js 满足：

```text
Node.js 20.19.0 或更高版本
也可以使用 Node.js 22.12.0 或更高版本
```

为了减少环境问题，推荐直接安装 Node.js 22 LTS。

下载地址：

```text
https://nodejs.org/
```

安装时一路默认即可。安装完成后，重新打开命令行窗口。

### 1.3 检查是否安装成功

在命令行输入：

```bash
node -v
npm -v
```

如果能看到版本号，例如：

```text
v22.x.x
10.x.x
```

说明 Node.js 和 npm 已经安装成功。

如果提示 `node 不是内部或外部命令`，通常说明 Node.js 没安装成功，或者安装后没有重新打开命令行窗口。

## 2. 解压项目

把项目压缩包解压到任意位置，例如：

```text
D:\satellite-front
```

如果路径里有中文或空格也可以运行，但为了减少命令行输入错误，建议放在比较简单的路径下，例如：

```text
D:\projects\satellite-front
```

## 3. 进入前端源码目录

打开命令行。

Windows 可以使用 PowerShell、CMD 或 VS Code 终端。

假设项目解压在：

```text
D:\projects\satellite-front
```

则执行：

```bash
cd D:\projects\satellite-front\.codebuddy\sate
```

如果是在 macOS 或 Linux 上，路径写法类似：

```bash
cd /你的项目路径/.codebuddy/sate
```

进入目录后，可以执行：

```bash
dir
```

如果能看到这些文件，就说明目录进对了：

```text
package.json
package-lock.json
vite.config.ts
src
public
```

macOS 或 Linux 上可以用：

```bash
ls
```

## 4. 安装项目依赖

在 `.codebuddy/sate` 目录下执行：

```bash
npm ci
```

这个命令会根据 `package-lock.json` 安装项目依赖，适合评审、验收、重新部署等稳定场景。

如果 `npm ci` 报错，可以改用：

```bash
npm install
```

依赖安装完成后，目录里会出现 `node_modules` 文件夹。

注意：

1. 第一次安装需要联网。
2. 如果网络较慢，安装可能需要几分钟。
3. 不要手动修改或删除 `node_modules`。

## 5. 配置环境变量

项目使用 `.env.development` 保存开发环境配置。

如果项目包里已经有 `.env.development`，一般可以先不改，直接运行。

如果没有这个文件，可以复制 `.env.example`：

```bash
copy .env.example .env.development
```

macOS 或 Linux 使用：

```bash
cp .env.example .env.development
```

然后用文本编辑器打开 `.env.development`，按实际情况修改。

一个常见配置如下：

```bash
VITE_API_BASE_URL=http://localhost:8080/
VITE_PORT=4173
VITE_PROXY_ENABLED=true

VITE_AMAP_KEY=你的高德地图JSAPI Key
VITE_AMAP_ROUTE_KEY=你的高德地图Web服务Key
```

各项含义：

| 配置项 | 作用 | 是否必须 |
| --- | --- | --- |
| `VITE_API_BASE_URL` | 后端接口地址 | 如果要登录、注册、AI 咨询等功能，需要配置 |
| `VITE_PORT` | 前端本地启动端口 | 建议保留，当前项目常用 `4173` |
| `VITE_PROXY_ENABLED` | 是否启用前端代理 | 开发运行时建议为 `true` |
| `VITE_AMAP_KEY` | 高德地图前端 JSAPI Key | 圣地巡游地图功能需要 |
| `VITE_AMAP_ROUTE_KEY` | 高德地图路线规划 Web 服务 Key | 圣地巡游路线规划效果更完整 |

说明：

1. 本项目的前端请求统一走 `/api`。
2. Vite 会把 `/api` 请求转发到 `VITE_API_BASE_URL`。
3. 例如前端请求 `/api/auth/login/password`，实际会转发到后端的 `/auth/login/password`。
4. 如果后端不是运行在 `8080` 端口，需要把 `VITE_API_BASE_URL` 改成真实后端地址。

## 6. 启动项目

确认当前目录仍然是：

```text
.codebuddy/sate
```

然后执行：

```bash
npm run dev
```

启动成功后，终端会显示类似内容：

```text
Local:   http://localhost:4173/
Network: http://你的局域网IP:4173/
```

用浏览器打开 `Local` 后面的地址即可。

如果你的 `.env.development` 里配置的是：

```bash
VITE_PORT=4173
```

则通常打开：

```text
http://localhost:4173/
```

如果没有配置端口，Vite 默认可能是：

```text
http://localhost:5173/
```

以终端实际显示的地址为准。

## 7. 后端服务说明

这个仓库是前端项目。页面本身可以启动，但部分功能需要后端接口配合。

需要后端的功能包括：

1. 登录
2. 注册
3. 获取验证码
4. 用户资料
5. 本命英雄列表
6. AI 咨询
7. 圣地巡游智能体推荐

如果没有启动后端，可能出现：

1. 页面能打开，但登录失败。
2. AI 咨询无法获取智能体列表。
3. 个人资料无法从服务器读取。
4. 浏览器控制台或页面提示网络错误。

如果只是查看前端页面效果，可以先直接运行前端；如果要完整演示业务流程，需要同时启动后端，并保证 `.env.development` 里的 `VITE_API_BASE_URL` 指向正确的后端地址。

## 8. 地图和定位功能说明

圣地巡游页面使用高德地图。

要完整使用地图和路线规划功能，需要：

1. 电脑可以访问互联网。
2. 浏览器允许定位权限。
3. `.env.development` 中配置了 `VITE_AMAP_KEY`。
4. 如果要使用路线规划接口，还需要配置 `VITE_AMAP_ROUTE_KEY`。

如果没有配置高德 Key，页面不会整体崩溃，但地图或路线功能会受影响。

如果浏览器询问是否允许定位，请选择允许。

如果定位失败，可以检查：

1. 浏览器是否禁止了定位权限。
2. 当前页面是否运行在 `localhost`。
3. 网络是否能访问高德地图服务。

## 9. 打包检查

如果想检查项目是否可以正常构建，可以执行：

```bash
npm run build
```

成功后会生成：

```text
dist
```

`dist` 是打包后的静态文件目录。

如果只是本地评审运行，一般执行 `npm run dev` 就够了。

## 10. 预览打包结果

先执行：

```bash
npm run build
```

再执行：

```bash
npm run preview
```

终端会显示一个本地地址，例如：

```text
http://localhost:4173/
```

浏览器打开即可预览打包后的版本。

注意：

1. `npm run dev` 是开发模式。
2. `npm run preview` 是预览打包后的 `dist`。
3. 评委现场演示一般用 `npm run dev` 更方便。

## 11. 常见问题

### 11.1 提示 npm 不是内部或外部命令

原因通常是 Node.js 没装好，或者安装后没有重新打开命令行。

解决办法：

1. 重新安装 Node.js 22 LTS。
2. 安装完成后关闭当前命令行窗口。
3. 重新打开命令行。
4. 再执行 `node -v` 和 `npm -v`。

### 11.2 npm ci 安装失败

可以尝试：

```bash
npm install
```

如果仍然失败，通常是网络问题。可以换网络后重试。

### 11.3 npm run dev 启动失败，提示 Node 版本太低

检查版本：

```bash
node -v
```

如果低于 `20.19.0`，请升级 Node.js。

推荐安装 Node.js 22 LTS。

### 11.4 端口被占用

如果 `4173` 端口被占用，可以修改 `.env.development`：

```bash
VITE_PORT=5173
```

保存后重新执行：

```bash
npm run dev
```

也可以使用终端输出的新地址访问。

### 11.5 页面打开后接口报错

先检查 `.env.development`：

```bash
VITE_API_BASE_URL=http://localhost:8080/
VITE_PROXY_ENABLED=true
```

然后确认后端是否已经启动，并且后端地址和端口是否正确。

如果后端运行在别的地址，例如：

```text
http://192.168.1.20:8080/
```

则需要改成：

```bash
VITE_API_BASE_URL=http://192.168.1.20:8080/
```

修改环境变量后，需要停止并重新启动前端。

### 11.6 修改 .env.development 后没有生效

环境变量只会在启动时读取。

修改 `.env.development` 后，需要：

1. 在终端按 `Ctrl + C` 停止项目。
2. 重新执行：

```bash
npm run dev
```

### 11.7 地图加载失败

检查：

1. 是否配置了 `VITE_AMAP_KEY`。
2. 高德 Key 是否有效。
3. 高德控制台是否限制了域名或服务类型。
4. 当前电脑是否能访问外网。

### 11.8 登录、注册、AI 咨询不可用

这些功能依赖后端。

请确认：

1. 后端服务已经启动。
2. `.env.development` 中的 `VITE_API_BASE_URL` 指向后端。
3. 后端接口路径和前端约定一致。
4. 浏览器开发者工具 Network 中没有跨域或 500 报错。

## 12. 最短运行流程

如果环境已经安装好，最短流程是：

```bash
cd 项目目录\.codebuddy\sate
npm ci
npm run dev
```

然后打开终端里显示的本地地址。

通常是：

```text
http://localhost:4173/
```

## 13. 项目命令说明

| 命令 | 作用 |
| --- | --- |
| `npm ci` | 根据锁文件安装依赖，推荐首次运行使用 |
| `npm install` | 普通安装依赖，`npm ci` 失败时可用 |
| `npm run dev` | 启动本地开发服务器 |
| `npm run build` | 打包生成 `dist` |
| `npm run preview` | 预览打包后的项目 |

## 14. 给评委的检查顺序建议

建议按这个顺序检查：

1. 先确认 `node -v` 和 `npm -v` 正常。
2. 进入 `.codebuddy/sate`。
3. 执行 `npm ci`。
4. 检查 `.env.development` 是否存在。
5. 执行 `npm run dev`。
6. 打开终端显示的本地地址。
7. 如果要测完整功能，再确认后端和高德 Key 是否配置正确。

