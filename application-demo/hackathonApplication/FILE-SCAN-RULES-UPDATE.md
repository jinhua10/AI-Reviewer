# 文件扫描规则更新说明

## 修改日期
2025-11-28

## 修改内容

### 文件扫描配置 (application.yml)

已修改 `ai-reviewer.scanner` 的 `include-patterns` 和 `exclude-patterns` 配置。

---

## 包含文件清单 (include-patterns)

### ✅ 源代码文件
| 类型 | 扩展名 |
|------|--------|
| Java | `*.java` |
| Python | `*.py` |
| Go | `*.go` |
| C# | `*.cs` |
| C++ | `*.cpp`, `*.cc`, `*.cxx`, `*.h`, `*.hpp` |
| JavaScript | `*.js`, `*.jsx`, `*.mjs`, `*.cjs` |
| TypeScript | `*.ts`, `*.tsx` |
| Vue | `*.vue` |
| Svelte | `*.svelte` |
| HTML/CSS | `*.html`, `*.htm`, `*.css`, `*.scss`, `*.sass`, `*.less`, `*.styl`, `*.stylus` |
| CSS Modules | `*.module.css`, `*.module.scss` |
| Angular | `*.component.ts`, `*.component.html`, `*.service.ts` |

### ✅ 依赖配置文件
- `package.json` - Node.js 依赖
- `pom.xml` - Maven 依赖
- `gradle.build` - Gradle 依赖
- `go.mod`, `go.sum` - Go 依赖
- `requirements.txt` - Python 依赖
- `Pipfile` - Python pipenv
- `Cargo.toml` - Rust 依赖
- `composer.json` - PHP 依赖
- `*.csproj` - C# 项目文件
- `*.vcxproj` - C++ 项目文件

### ✅ 项目配置文件
- `config.yml`, `config.yaml`
- `.env.example`, `.env.sample`
- `Dockerfile`, `docker-compose.yml`, `docker-compose.yaml`

### ✅ 文档文件
- **仅限** `README.md` （主文档）

---

## 排除文件清单 (exclude-patterns)

### ❌ Lock 文件（依赖锁定）
```
package-lock.json    yarn.lock
pnpm-lock.yaml       npm-shrinkwrap.json
Pipfile.lock         poetry.lock
Gemfile.lock         composer.lock
gradle.lock
```

### ❌ Markdown 文件（非 README）
```
*.md (除 README.md 外)
CHANGELOG.md         CONTRIBUTING.md
AUTHORS.md           LICENSE.md
*.mdx
```

### ❌ 图片文件
```
*.png      *.jpg      *.jpeg     *.gif
*.svg      *.webp     *.ico      *.bmp      *.tiff
```

### ❌ IDE 配置文件
```
.vscode/**           .idea/**
*.iml                .editorconfig
.jshintrc            .eslintrc*
.prettierrc*         .stylelintrc*
*.swp                *.swo
Thumbs.db            .DS_Store
```

### ❌ 构建输出
```
target/**            build/**
out/**               dist/**
bin/**               obj/**
Debug/**             Release/**
```

### ❌ 包管理缓存
```
node_modules/**      .next/**
coverage/**          .nuxt/**
bower_components/**  vendor/**
__pycache__/**       .pytest_cache/**
.venv/**             venv/**
packages/**          .gradle/**
.m2/**               gems/**
```

### ❌ 临时文件和日志
```
.git/**              .svn/**
.cache/**            .tmp/**
tmp/**               temp/**
*.log                .log
.session
/docs/_build/**
site-packages/**
```

### ❌ 编译后文件
```
*.exe       *.dll      *.so       *.dylib
*.class     *.o        *.a        *.lib
*.jar       *.war      *.ear
```

### ❌ VCS 和配置
```
.gitignore           .gitattributes
.env                 .env.local
.env.*.local         secrets/**
.sonarqube/**        .vs/**
```

---

## 关键修改说明

### 1. **lock 文件完全排除**
   - ✅ 不再包含任何 `*-lock.json`, `*.lock` 文件
   - 原因：这些文件是自动生成的，不需要评审

### 2. **Markdown 文件精确控制**
   - ✅ 仅包含 `README.md`
   - ❌ 排除其他所有 `.md` 文件
   - 原因：只有 README 是项目文档，其他 md 是辅助信息

### 3. **图片文件完全排除**
   - ❌ 所有图片格式（png, jpg, svg 等）
   - 原因：图片不需要代码评审

### 4. **IDE 配置文件完全排除**
   - ❌ `.vscode`, `.idea`, `*.iml` 等
   - 原因：IDE 配置不是项目源码

### 5. **NPM/包管理工具缓存排除**
   - ❌ `node_modules`, `vendor`, `venv` 等
   - 原因：这些是依赖包，不是源码

---

## 工作流程示例

### ✅ 包含在评审中的文件
```
my-project/
├── README.md                    ✅ 项目说明
├── src/
│   ├── main.ts                  ✅ TypeScript 源码
│   ├── app.vue                  ✅ Vue 组件
│   └── styles.scss              ✅ SCSS 样式
├── package.json                 ✅ 依赖配置
├── Dockerfile                   ✅ Docker 配置
└── config.yaml                  ✅ 项目配置
```

### ❌ 排除在评审之外的文件
```
my-project/
├── package-lock.json            ❌ Lock 文件
├── CHANGELOG.md                 ❌ 非 README markdown
├── CONTRIBUTING.md              ❌ 非 README markdown
├── docs/
│   ├── guide.md                 ❌ 非 README markdown
│   └── images/
│       └── logo.png             ❌ 图片文件
├── .vscode/                     ❌ IDE 配置
├── .idea/                       ❌ IDE 配置
├── node_modules/                ❌ 依赖包
├── dist/                        ❌ 构建输出
└── .git/                        ❌ VCS 文件
```

---

## 配置生效范围

- 文件扫描器在解析项目时，自动应用这些规则
- 批量评审和单个项目评审都遵守这些规则
- 可在 `application.yml` 中随时修改配置

---

## 验证方式

运行评审时，查看日志输出：
```
[FileScanner] Scanned files: X files (excludes applied)
[FileScanner] Included: source code, configs, README.md
[FileScanner] Excluded: lock files, images, IDE config, markdown docs
```

---

## 常见问题

**Q: 为什么排除 package-lock.json?**
A: 该文件由 npm 自动生成，包含具体的依赖版本锁定信息。评审源码时不需要这些自动生成的文件。

**Q: 为什么只保留 README.md?**
A: README 是项目的主文档和入口，包含项目目标和使用说明。其他 markdown 文件（CHANGELOG, CONTRIBUTING 等）是辅助信息，与核心代码评审无关。

**Q: 为什么排除所有图片?**
A: 代码评审关注的是源码质量，图片文件无法通过代码评估。

**Q: 为什么排除 node_modules 等依赖文件夹?**
A: 这些是第三方依赖，不是项目自己的代码，评审的是项目本身的源码。


