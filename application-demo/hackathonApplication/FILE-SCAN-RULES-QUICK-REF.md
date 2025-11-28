# 文件扫描规则 - 快速参考

## 📋 一句话总结
**只读取源码文件、README.md、依赖配置文件；排除 lock 文件、非 README markdown、图片、IDE 配置。**

---

## ✅ 包含（INCLUDE）

### 源码
- Java, Python, Go, C#, C++
- JavaScript/TypeScript (`.js`, `.ts`, `.tsx` 等)
- Vue, React, Angular, Svelte
- HTML, CSS, SCSS, Less, Stylus

### 配置和依赖
- `package.json`, `pom.xml`, `go.mod`, `requirements.txt`
- `Cargo.toml`, `composer.json`, `Pipfile`
- `*.csproj`, `*.vcxproj`
- `Dockerfile`, `docker-compose.yml`
- `config.yml`, `config.yaml`
- `.env.example`, `.env.sample`

### 文档
- **仅限** `README.md` ✅

---

## ❌ 排除（EXCLUDE）

### Lock 文件
`package-lock.json`, `yarn.lock`, `pnpm-lock.yaml`, `Pipfile.lock`, `poetry.lock`, `Gemfile.lock`, `composer.lock`, `gradle.lock`

### Markdown
所有 `*.md` 和 `*.mdx` **除了** `README.md`
（`CHANGELOG.md`, `CONTRIBUTING.md`, `AUTHORS.md` 等都排除）

### 图片
`.png`, `.jpg`, `.jpeg`, `.gif`, `.svg`, `.webp`, `.ico`, `.bmp`, `.tiff`

### IDE 配置
`.vscode/`, `.idea/`, `.editorconfig`, `.eslintrc`, `.prettierrc`, `.stylelintrc`, `*.iml`

### 构建输出
`target/`, `build/`, `dist/`, `out/`, `bin/`, `obj/`, `Debug/`, `Release/`

### 依赖包
`node_modules/`, `vendor/`, `venv/`, `.venv/`, `__pycache__/`, `.nuxt/`

### 临时和日志
`*.log`, `.cache/`, `.tmp/`, `/tmp/`, `coverage/`, `.session`

### VCS 文件
`.git/`, `.svn/`, `.gitignore`, `.gitattributes`

### 编译输出
`.class`, `.o`, `.a`, `.lib`, `.dll`, `.so`, `.jar`, `.exe`

---

## 📊 统计表

| 文件类型 | 状态 | 原因 |
|--------|------|------|
| 源代码 `.java`, `.ts`, `.py` | ✅ | 代码质量评审的主体 |
| README.md | ✅ | 项目入口文档 |
| 其他 markdown | ❌ | 非必要的辅助文档 |
| package-lock.json | ❌ | 自动生成的锁定文件 |
| 图片文件 | ❌ | 无法代码评审 |
| node_modules | ❌ | 第三方依赖包 |
| .vscode, .idea | ❌ | IDE 配置 |
| 配置文件 (package.json 等) | ✅ | 项目依赖和配置信息 |

---

## 🔄 配置位置

**文件:** `application.yml`

**路径:**
```
ai-reviewer:
  scanner:
    include-patterns:  # 上面 ✅ 的规则
    exclude-patterns:  # 上面 ❌ 的规则
```

---

## 💡 设计理念

1. **精准性** - 只读必要文件，减少噪音
2. **效率** - 跳过自动生成的文件和依赖包
3. **可维护性** - 清晰的文件分类，易于维护
4. **一致性** - 统一的评审范围，公平比对项目


