# 彩虹金刚 Rainbow Vajra

> 一个基于 D&D 5e 规则的资料库与 AI 驱动的冒险系统，融合暗黑奇幻 RPG 风格的前端界面。

## 项目简介

彩虹金刚是一个完整的 DND（龙与地下城）5e 中文资料库与 AI 冒险游戏系统。项目分为前后端两部分：

- **LinXR**（后端）：Spring Boot 3 + MyBatis-Plus + LangChain4j，提供资料库 API、角色管理、AI 驱动的地下城主（DM）冒险与战斗系统。
- **XRLin**（前端）：Vue 3 + Vite，采用暗黑奇幻 RPG 风格（参考《博德之门 3》），提供沉浸式的资料浏览、角色创建与冒险体验。

## 功能特性

### 资料图鉴
- **12 个职业**：野蛮人、吟游诗人、牧师、德鲁伊、战士、武僧、圣武士、游侠、游荡者、术士、邪术师、法师
- **9 个种族**：人类、精灵、矮人、半身人、龙裔、侏儒、半精灵、半兽人、提夫林
- **59 个法术**：涵盖 0-9 环奥术与神术，含伤害骰子、法术位、施法属性等完整数据
- **22 个怪物**：从地精到塔拉斯奎巨兽，含 CR、AC、HP、攻击加值、伤害公式
- **35 个魔法物品**：武器、护甲、药水与奇物，支持动态效果处理
- 专长、武器、护甲、背景、技能、语言等完整 DND 数据

### 角色系统
- 角色创建：选择种族、职业、背景，分配六维属性（标准阵列）
- 装备管理：武器、护甲、背包物品
- 升级系统：12 个职业 × 2-12 级的成长路线（136 条记录），支持属性提升、新法术、战斗风格等选择
- 用户认证：用户名 + 密码 + 验证码登录，BCrypt 加密，Redis 管理 Token

### AI 冒险系统
- **AI 地下城主（DM）**：基于 LangChain4j + 通义千问（qwen-plus），通过 Function Calling 调用游戏工具
- **流式叙事**：SSE 实时推送 AI 生成的冒险故事文本
- **回合制战斗**：近战攻击、远程攻击、施法、使用物品、逃跑，玩家与敌人回合分离
- **遭遇系统**：根据角色等级动态生成怪物（1-4 级遇 1-2 只，5+ 级遇 1-3 只）
- **物品效果处理器**：基于 `effectType` 的通用物品效果引擎，非硬编码
- **游戏工具集**：getGameState、playerMeleeAttack、playerRangedAttack、castSpell、useItem、attemptFlee、executeEnemyTurn、triggerEncounter、checkVictory、longRest 等

### 前端界面
- **暗黑奇幻风格**：深黑、暗棕、铜色、暗金配色，魔法紫/暗红/暗蓝点缀
- **中世纪字体**：Cinzel、MedievalSharp（标题）、Crimson Text（正文）
- **冒险探索界面**：羊皮纸地图场景，根据故事关键词自动切换地图（森林、地牢、废墟、山脉、城镇、沼泽）
- **战斗界面**：BG3 风格布局，角色立绘、状态栏、行动条、回合顺序
- **资料浏览器**：分类卡片、搜索筛选、详情面板、随机条目、收藏功能
- **角色立绘**：24 张职业立绘（男/女）、22 张怪物立绘、6 张地图场景

## 技术栈

### 后端
| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 运行环境 |
| Spring Boot | 3.5.14 | Web 框架 |
| MyBatis-Plus | 3.5.7 | ORM |
| LangChain4j | 1.1.0 | AI Agent 框架 |
| Spring Data Redis | - | 缓存/会话/验证码 |
| MinIO | 8.5.10 | 对象存储（图片） |
| spring-security-crypto | - | BCrypt 密码加密 |

### 前端
| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.5 | UI 框架 |
| Vue Router | 4.5 | 路由 |
| Vite | 7.0 | 构建工具 |

### 基础设施（Docker）
| 服务 | 镜像 | 端口 |
|------|------|------|
| MySQL | mysql:8.0 | 3307 |
| Redis | redis:7-alpine | 6379 |
| MinIO | minio/minio | 9000 / 9001 |
| Ollama | ollama/ollama | 11434 |

### AI 模型
- **大语言模型**：通义千问 qwen-plus（通过 DashScope OpenAI 兼容接口）
- **向量嵌入模型**：bge-m3（通过本地 Ollama）

## 项目结构

```
LXR/
├── LinXR/                          # 后端 (Spring Boot 多模块)
│   ├── pom.xml                     # 父 POM
│   ├── common/                     # 公共模块
│   │   └── src/main/java/com/jjwpp/linxr/common/
│   │       ├── base/R.java         # 统一响应体
│   │       ├── config/             # CorsConfig, MinioConfig, MyBatisConfig, GlobalExceptionHandler
│   │       └── dto/                # CategoryDTO, CodexEntryDTO
│   ├── model/                      # 实体模块
│   │   └── src/main/java/com/jjwpp/linxr/entity/
│   │       └── (17 个实体类: Armor, Background, CharacterInventory, Class, 
│   │            ClassLevelProgression, Condition, Feat, Language, MagicItem, 
│   │            Monster, MonsterDrop, PlayerCharacter, Race, Skill, Spell, 
│   │            User, Weapon)
│   ├── web/                        # Web 模块
│   │   └── src/main/
│   │       ├── java/com/jjwpp/linxr/
│   │       │   ├── controller/     # 18 个 Controller
│   │       │   │   └── (Auth, Adventure, Class, Race, Spell, Monster, 
│   │       │   │      MagicItem, Feat, Weapon, Armor, Background, Skill, 
│   │       │   │      Language, Condition, PlayerCharacter, Inventory, 
│   │       │   │      Image, Health)
│   │       │   ├── dm/             # AI 地下城主
│   │       │   │   ├── DungeonMasterAgent.java   # Agent 入口
│   │       │   │   ├── DmService.java            # 冒险服务
│   │       │   │   ├── GameTools.java            # 游戏工具集（Function Calling）
│   │       │   │   ├── AdventureState.java       # 冒险状态管理
│   │       │   │   ├── ItemEffectProcessor.java  # 物品效果处理器
│   │       │   │   ├── LevelUpService.java       # 升级服务
│   │       │   │   └── tool/                     # 8 个游戏工具类
│   │       │   │       ├── AbilityTool.java      # 属性检定
│   │       │   │       ├── AttackTool.java       # 攻击计算
│   │       │   │       ├── DamageTool.java       # 伤害计算
│   │       │   │       ├── DiceTool.java         # 骰子工具
│   │       │   │       ├── EncounterTool.java    # 遭遇生成
│   │       │   │       ├── HpTool.java           # 生命值管理
│   │       │   │       ├── LootTool.java         # 战利品掉落
│   │       │   │       └── SpellTool.java        # 法术验证
│   │       │   ├── mapper/         # 17 个 MyBatis Mapper
│   │       │   └── service/        # 17 个 Service 接口 + impl/ 实现类
│   │       └── resources/
│   │           ├── application.yml.example  # 配置模板（安全）
│   │           ├── .env.example             # 环境变量模板
│   │           ├── mapper/                  # 12 个 MyBatis XML
│   │           └── prompts/                 # 10 个 AI 提示词模板
│   │               ├── system.txt           # 系统提示词
│   │               ├── system_agent.txt     # Agent 系统提示词
│   │               ├── adventure_start.txt  # 冒险开场
│   │               ├── combat_round.txt     # 战斗回合
│   │               ├── combat_trigger.txt   # 战斗触发
│   │               ├── encounter_dialog.txt # 遭遇对话
│   │               ├── victory_explore.txt  # 胜利探索
│   │               ├── victory_levelup.txt  # 胜利升级
│   │               ├── levelup_complete.txt # 升级完成
│   │               └── fled.txt             # 逃跑
│   └── DND/                        # 数据库脚本
│       ├── docker-compose.yml      # Docker 编排
│       ├── init.sql                # 初始化脚本（建表 + 数据）
│       ├── user.sql                # 用户表
│       ├── migration_*.sql         # 10 个迁移脚本
│       └── fix_*.sql               # 3 个法术数据修复脚本
├── XRLin/                          # 前端 (Vue3 + Vite)
│   ├── package.json
│   ├── vite.config.js
│   ├── .env.example
│   └── src/
│       ├── assets/                 # 图片资源
│       │   ├── classes/            # 24 张职业立绘（男/女各 12）
│       │   ├── monsters/           # 22 张怪物立绘
│       │   ├── maps/               # 6 张地图场景
│       │   ├── hero-bg.jpg         # 首页背景
│       │   └── texture-parchment.jpg  # 羊皮纸纹理
│       ├── components/             # 28 个 Vue 组件
│       ├── composables/            # 8 个组合式函数
│       │   └── (useApi, useAuth, useClassImages, useFavorites, 
│       │      useMapImages, useMonsterImages, useRecentlyViewed, useTheme)
│       ├── pages/                  # 8 个页面
│       │   └── (Home, Browse, Detail, Random, CharacterBuilder, 
│       │      CharacterList, Adventure, Login)
│       └── router/                 # 路由配置
├── .gitignore
└── README.md
```

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- Node.js 18+
- Docker Desktop
- 通义千问 API Key（[获取地址](https://dashscope.console.aliyun.com/)）

### 1. 启动基础设施

```bash
cd LinXR/DND
docker-compose up -d
```

等待 MySQL 初始化完成（约 10-20 秒），然后导入数据库：

```bash
# 导入初始数据（如果 linxr 数据库不存在或表结构不完整）
docker exec -i mysql mysql -uroot -pRoot@123456 < init.sql

# 执行迁移脚本
docker exec -i mysql mysql -uroot -pRoot@123456 linxr < migration_ability_scores.sql
docker exec -i mysql mysql -uroot -pRoot@123456 linxr < user.sql
docker exec -i mysql mysql -uroot -pRoot@123456 linxr < migration_player_character_user.sql
docker exec -i mysql mysql -uroot -pRoot@123456 linxr < migration_gender.sql
docker exec -i mysql mysql -uroot -pRoot@123456 linxr < migration_image_urls.sql
docker exec -i mysql mysql -uroot -pRoot@123456 linxr < migration_inventory_system.sql
docker exec -i mysql mysql -uroot -pRoot@123456 linxr < migration_level_system.sql
docker exec -i mysql mysql -uroot -pRoot@123456 linxr < migration_spell_class.sql
docker exec -i mysql mysql -uroot -pRoot@123456 linxr < migration_starting_equipment.sql
docker exec -i mysql mysql -uroot -pRoot@123456 linxr < migration_tool_refactor.sql
docker exec -i mysql mysql -uroot -pRoot@123456 linxr < migration_weapon_attack_bonus.sql

# 法术数据修复脚本（如法术伤害骰子或编码有误）
docker exec -i mysql mysql -uroot -pRoot@123456 linxr < fix_spell_damage_dice.sql
docker exec -i mysql mysql -uroot -pRoot@123456 linxr < fix_spell_damage_v2.sql
docker exec -i mysql mysql -uroot -pRoot@123456 linxr < fix_spell_encoding.sql
```

拉取 Ollama 嵌入模型：

```bash
docker exec -it ollama ollama pull bge-m3
```

### 2. 配置后端

```bash
# 复制配置模板并填入你的 API Key
cp LinXR/web/src/main/resources/application.yml.example LinXR/web/src/main/resources/application.yml
```

编辑 `application.yml`，填入你的通义千问 API Key 和数据库密码。

### 3. 启动后端

```bash
cd LinXR
mvn install -DskipTests
cd web
mvn spring-boot:run
```

后端启动后访问 `http://localhost:8080/api/health` 验证。

### 4. 启动前端

```bash
cd XRLin
npm install
npm run dev
```

前端启动后访问 `http://localhost:5173`。

## API 接口

### 认证
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/captcha` | 获取图形验证码 |
| POST | `/api/auth/register` | 用户注册 |
| POST | `/api/auth/login` | 用户登录 |
| GET | `/api/auth/me` | 获取当前登录用户 |
| POST | `/api/auth/logout` | 登出 |

### 资料图鉴
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/class/list` | 职业列表 |
| GET | `/api/class/{id}` | 职业详情 |
| GET | `/api/class/random` | 随机职业 |
| GET | `/api/race/list` | 种族列表 |
| GET | `/api/race/{id}` | 种族详情 |
| GET | `/api/spell/list` | 法术列表 |
| GET | `/api/spell/{id}` | 法术详情 |
| GET | `/api/monster/list` | 怪物列表 |
| GET | `/api/monster/{id}` | 怪物详情 |
| GET | `/api/magic-item/list` | 魔法物品列表 |
| GET | `/api/magic-item/{id}` | 魔法物品详情 |
| GET | `/api/feat/list` | 专长列表 |
| GET | `/api/feat/{id}` | 专长详情 |
| GET | `/api/weapon/list` | 武器列表 |
| GET | `/api/armor/list` | 护甲列表 |
| GET | `/api/background/list` | 背景列表 |
| GET | `/api/background/{id}` | 背景详情 |
| GET | `/api/skill/list` | 技能列表 |
| GET | `/api/skill/{id}` | 技能详情 |
| GET | `/api/language/list` | 语言列表 |
| GET | `/api/language/{id}` | 语言详情 |
| GET | `/api/condition/list` | 状态条件列表 |
| GET | `/api/condition/{id}` | 状态条件详情 |

> 以上图鉴接口均支持 `/{id}` 详情、`/page` 分页、`/random` 随机、`/count` 计数等标准 CRUD 操作。

### 角色管理
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/player-character/list` | 我的角色列表 |
| GET | `/api/player-character/{id}` | 角色详情 |
| POST | `/api/player-character` | 创建角色 |
| DELETE | `/api/player-character/{id}` | 删除角色 |

### 背包与装备
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/inventory/{characterId}` | 获取角色背包 |
| POST | `/api/inventory/use` | 使用物品 |
| POST | `/api/inventory/equip` | 装备物品 |
| GET | `/api/inventory/drop/{battleId}` | 获取战斗掉落 |

### AI 冒险
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/adventure/start` | 开始冒险（SSE 流式） |
| POST | `/api/adventure/{sessionId}/act` | 玩家行动（SSE 流式） |
| POST | `/api/adventure/{sessionId}/rest` | 长休 |
| POST | `/api/adventure/{sessionId}/short-rest` | 短休 |
| POST | `/api/adventure/{sessionId}/use-item` | 冒险中使用物品 |
| GET | `/api/adventure/{sessionId}/level-up-info` | 获取升级选项 |
| POST | `/api/adventure/{sessionId}/levelup` | 执行升级 |
| POST | `/api/adventure/{sessionId}/encounter/confirm` | 确认遭遇 |
| GET | `/api/adventure/{sessionId}/character-panel` | 角色面板数据 |
| POST | `/api/adventure/{sessionId}/end-turn` | 结束玩家回合 |

### 图片管理
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/image/upload` | 上传图片到 MinIO |
| POST | `/api/image/import/monsters` | 批量导入怪物立绘 |
| POST | `/api/image/import/classes` | 批量导入职业立绘 |

### 健康检查
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/health` | 服务健康检查 |

> 所有角色、背包和冒险接口需要 `Authorization: Bearer {token}` 请求头。

## 配置说明

### 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `DB_PASSWORD` | MySQL 密码 | - |
| `DASHSCOPE_API_KEY` | 通义千问 API Key | - |
| `MINIO_USERNAME` | MinIO 用户名 | minioadmin |
| `MINIO_PASSWORD` | MinIO 密码 | - |
| `CORS_ALLOWED_ORIGINS` | CORS 允许的域名 | localhost:5173,localhost:8080 |

## 数据库概览

| 表名 | 记录数 | 说明 |
|------|--------|------|
| class | 12 | 职业 |
| race | 9 | 种族 |
| spell | 59 | 法术 |
| monster | 22 | 怪物 |
| magic_item | 35 | 魔法物品 |
| feat | 8 | 专长 |
| weapon | 12 | 武器 |
| armor | 11 | 护甲 |
| background | 13 | 背景 |
| skill | 18 | 技能 |
| language | 16 | 语言 |
| class_level_progression | 136 | 职业升级配置 |
| monster_drop | 27 | 怪物掉落 |
| condition | 15 | 状态条件 |
| user | - | 用户 |
| player_character | - | 玩家角色 |
| character_inventory | - | 角色背包 |

## License

MIT
