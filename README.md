# Naval War - 项目说明

概述
----
Naval War 是一个基于 Java + Swing 的简单海战样机。玩家控制军舰在海面左右移动并投放深水炸弹（或发射导弹）来消灭海面下的潜艇。UI 使用 Swing 窗口渲染，游戏逻辑由引擎（GameEngine）管理实体的生成、更新与碰撞判定。图像资源位于 `resources/` 目录，支持背景、军舰、潜艇、炸弹、导弹与爆炸帧。

目录结构（重点文件）
-------------------
- src/
  - entities/
    - Entity.java         : 所有游戏实体的抽象基类，负责位置/尺寸/活跃态与包围盒（Rect）返回。
    - Ship.java           : 玩家控制的军舰实体，负责左右移动、投弹冷却与朝向（facing）。
    - Bomb.java           : 深水炸弹实体，下落动力学（简单重力）、爆炸触发标记（explode）。
    - Missile.java        : UI 维护的追踪导弹实体（homing），会寻找最近潜艇并移动；提供角度用于图片旋转。
    - Submarine.java      : 潜艇实体，带有 Type（RED/BLACK），根据类型设定速度与得分（RED=20，BLACK=10），支持上下浮动与受伤。
    - Explosion.java      : 爆炸实体，构造时对周围潜艇做范围伤害并通过 `engine.registerKill(...)` 汇报击落；自身有生命周期用于渲染与清理。
  - ui/
    - GameWindow.java     : Swing 窗口与渲染层，负责：
                             - 加载 `resources/*.png` 图像（背景、ship0/1、q1/q2/r1/h2、boom、missile、b/b1/b2）；
                             - 接收键盘输入（左右、空格、1/2 切换武器）；
                             - 管理 UI 侧导弹（missiles）和冷却（5 秒）；
                             - 按帧（Timer）调用 engine.tick(dt) 并重绘场景；
                             - 绘制船、炸弹、导弹、潜艇与爆炸（优先使用图片，缺图时回退到矢量绘制）；
                             - 提供矩形碰撞辅助方法 rectsIntersect(...)。
  - Main.java             : 程序入口，在 EDT 中创建 GameEngine 与 GameWindow，启动引擎与窗口。

资源（resources）
----------------
项目使用的图片资源应放在项目根目录下的 `resources/` 目录中，常用名称：
- background.png   : 背景
- ship0.png, ship1.png : 军舰两帧（根据朝向选择）
- q1.png, q2.png, r1.png, h2.png : 四种潜艇图片（代码中 RED/BLACK 使用特定子集）
- boom.png         : 深水炸弹
- missile.png      : 导弹（会根据导弹角度旋转绘制）
- b.png, b1.png, b2.png : 爆炸帧

实现要点与交互接口
------------------
- 实体设计（entities 包）：
  - Entity：抽象基类，提供位置 x,y，尺寸 w,h，活跃状态 active，和 getBox() 返回包围盒（整型 Rect）。
  - 各实体实现 `update(long dt)`，以 ms 为单位推进逻辑（部分实体在 UI 侧以不同步方式更新，例如 Missile）。
  - Submarine 带 Type，构造时会基于随机决定 RED（速度更快、score=20）或 BLACK（速度较慢、score=10）。
  - Explosion 构造时执行范围检测，并调用 `engine.registerKill(...)` 与 `engine.addExplosion(this)`（引擎需实现这些方法）。

- 引擎接口（GameEngine，项目中应存在 engine/GameEngine.java，UI 与实体按下述接口交互）：
  - engine.tick(long dt)            : 推进引擎一帧（ms）。
  - engine.start()/engine.stop()    : 启动/停止引擎（若引擎有独立线程或资源需管理）。
  - engine.getShip()                : 返回 Ship 实例（UI 用于渲染和发射起点）。
  - engine.playerMoveLeft()/playerMoveRight(): 玩家控制接口（GameWindow 使用）。
  - engine.playerDropBomb()         : 由引擎创建并管理 Bomb 实例（GameWindow 调用以发射普通炸弹）。
  - engine.getBombs()/getSubs()/getExplosions(): 返回当前活跃实体列表（UI 用于绘制与检测）。
  - engine.getSeaTop()/getHeight()  : UI 用于确定海面位置与窗口高度。
  - engine.registerKill(Submarine s, int score) : 引擎接收击杀事件以累积分数并处理潜艇死亡（Explosion 已调用）。
  - engine.addExplosion(Explosion e) : 引擎保存 Explosion 以便计时与渲染。

- UI 实现要点（GameWindow）：
  - 使用 Swing Timer 驱动主循环（约 60 FPS），每帧先处理键盘状态（左右持续移动），再处理一次性的发射请求（空格），然后更新 UI 侧的 missiles，最后调用 engine.tick(dt) 并重绘。
  - 武器系统：按 1 切换为普通炸弹（调用 engine.playerDropBomb()），按 2 切换为导弹（UI 侧创建 Missile，5 秒冷却）。
  - 导弹（Missile）由 UI 维护并会追踪最近潜艇；命中时 UI 创建 Explosion 并交由 engine 处理伤害/计分。
  - 碰撞检测：UI 使用 `rectsIntersect(Entity.Rect a, Entity.Rect b)` 来替代缺失的静态 intersect 方法。
  - 绘制细节：当图片存在时优先使用图片渲染；导弹图片会按导弹当前角度旋转；潜艇根据 Type 选择图片子集（RED -> q2/r1，BLACK -> h2/q1）；爆炸可用多帧图片循环显示。

已知假设与注意事项
------------------
- GameEngine 源码未在本 README 中详细列出，但 UI 与实体依赖其提供的接口（见上文）。若引擎实现与接口不符，请对照接口做小改动或在 README 中补充说明。
- 部分实体（例如 Missile）是在 UI 层维护的，为了快速集成而非完全后端化。若希望统一管理，建议把 Missile 迁移到 engine 包并由 engine 统一更新、碰撞处理与冷却控制。
- 资源路径以相对项目根目录 `resources/*.png` 为准；运行前请确保文件存在且名称与代码一致。
- 并发：UI（EDT）与 engine 的实现若使用独立线程，需要对实体集合的访问加锁或提供线程安全快照接口；当前代码在 UI 侧对 missiles 使用 synchronized，engine 侧应在 getSubs()/getBombs()/getExplosions() 返回安全的只读视图或快照以避免并发问题。

运行方法
--------
1. 确保 JDK 已安装（建议 11+）。
2. 将项目根目录设置为当前工作目录，确保 `resources/` 目录存在并包含所需图片。
3. 使用 IDE（如 IntelliJ IDEA）导入项目或用命令行：
   javac -d out $(find src -name "*.java")
   java -cp out Main
4. 程序会弹出窗口：左右键 / A D 控制移动，空格投弹，1/2 切换武器（导弹有 5 秒冷却）。

扩展建议
--------
- 把 Missiles、Bombs、Explosions 全部迁移到 GameEngine 中统一管理，减少 UI 与逻辑耦合。
- 实现更精准的物理与时间步（固定时间步+插值）以保证不同机器上的一致体验。
- 添加音效与更丰富的 UI（生命值、关卡、存档）。
- 改进资源管理，使用 classpath 资源加载（getResourceAsStream）以提高可移植性。

联系方式
--------
- 本文档基于当前 src 下的文件分析生成，如需进一步代码修正或把 Missile 迁移到 engine 层，可说明我会给出具体补丁。

