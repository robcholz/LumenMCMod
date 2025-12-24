# LumenMCMod

面向初学者的说明：这是一个 Fabric 客户端模组，用于支持 Project Lumen。

## 功能简介
- 客户端支持：为 Project Lumen 提供 Minecraft 客户端侧支持。
- Mod Menu 集成：在模组菜单中显示配置入口。
- 自动重连：串口断开后按设定周期尝试重新连接。

## 运行环境
- Minecraft：1.21.3
- Fabric Loader：0.17.2（或更高）
- Fabric API：0.112.1+1.21.3
- Java：21

## 快速安装（玩家）
1. 安装 Minecraft 1.21.3。
2. 安装 Fabric Loader。
3. 将 Fabric API 和本模组的 `jar` 文件放入 `mods` 目录。
4. 启动游戏即可。

## 配置界面（Config Screen）
在 Mod Menu 中打开本模组配置：
- 端口路径：手动输入串口路径，留空则自动检测。
- 选择端口：弹出端口列表，支持刷新。
- 自动重连：开/关自动重连。
- 重连周期（秒）：自动重连的间隔时间。

## 本地构建（开发者）
1. 安装 JDK 21。
2. 克隆本项目并进入目录。
3. 构建：
   ```bash
   ./gradlew build
   ```
4. 生成的 `jar` 位于 `build/libs/`。

## 运行开发环境（可选）
如果你想在开发环境中直接启动客户端：
```bash
./gradlew runClient
```

## 版本信息
- 模组版本：1.0.0
- 许可证：MIT

## 反馈与问题
- 项目主页：<https://github.com/robcholz/LumenMCMod>
- Issue：<https://github.com/robcholz/LumenMCMod/issues>
