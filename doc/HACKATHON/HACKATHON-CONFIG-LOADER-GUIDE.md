# 黑客松评分配置文件使用指南

## 功能说明

AI-Reviewer 现在支持通过 YAML 或 JSON 配置文件动态加载黑客松评分规则，无需修改代码即可调整评分标准。

## 支持的文件格式

- ✅ YAML (.yaml, .yml)
- ✅ JSON (.json)

## 配置文件示例

### YAML 格式示例

查看完整示例：`src/main/resources/hackathon-scoring-config-example.yaml`

```yaml
scoring:
  # 评分维度配置
  dimensions:
    code_quality:
      weight: 0.40
      display_name: "代码质量"
      description: "评估代码的可读性、可维护性和技术债务"
      enabled: true
    
    innovation:
      weight: 0.30
      display_name: "创新性"
      enabled: true
  
  # 评分规则配置
  rules:
    - name: "code-quality-basic"
      type: "code_quality"
      weight: 1.0
      strategy: "keyword_matching"
      enabled: true
      positive_keywords:
        "单元测试": 20
        "注释": 10
      negative_keywords:
        "代码重复": -15
  
  # AST分析配置
  ast_analysis:
    enabled: true
    thresholds:
      long_method: 50
      high_complexity: 10
```

### JSON 格式示例

查看完整示例：`src/main/resources/hackathon-scoring-config-example.json`

```json
{
  "scoring": {
    "dimensions": {
      "code_quality": {
        "weight": 0.40,
        "displayName": "代码质量",
        "enabled": true
      }
    },
    "rules": [
      {
        "name": "code-quality-basic",
        "type": "code_quality",
        "weight": 1.0,
        "enabled": true,
        "positiveKeywords": {
          "单元测试": 20
        }
      }
    ],
    "astAnalysis": {
      "enabled": true,
      "thresholds": {
        "long_method": 50
      }
    }
  }
}
```

## 使用方法

### 方法 1：从文件加载配置

```java
// 加载YAML配置
HackathonScoringConfig config = HackathonScoringConfig.loadFromFile("custom-scoring.yaml");

// 加载JSON配置
HackathonScoringConfig config = HackathonScoringConfig.loadFromFile("custom-scoring.json");
```

### 方法 2：使用默认配置

```java
// 如果不想使用配置文件，可以使用默认配置
HackathonScoringConfig config = HackathonScoringConfig.createDefault();
```

## 配置项说明

### 1. 评分维度 (dimensions)

定义评分的各个维度及其权重。

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `weight` | Double | 是 | 维度权重（0-1之间） |
| `display_name` / `displayName` | String | 否 | 显示名称 |
| `description` | String | 否 | 维度描述 |
| `enabled` | Boolean | 否 | 是否启用（默认true） |

**示例**：
```yaml
code_quality:
  weight: 0.40
  display_name: "代码质量"
  description: "评估代码的可读性、可维护性"
  enabled: true
```

### 2. 评分规则 (rules)

定义具体的评分规则。

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `name` | String | 是 | 规则名称 |
| `type` | String | 是 | 规则类型（对应维度） |
| `weight` | Double | 否 | 规则权重（默认1.0） |
| `strategy` | String | 否 | 评分策略 |
| `positive_keywords` | Map | 否 | 正向关键词及分数 |
| `negative_keywords` | Map | 否 | 负向关键词及扣分 |
| `enabled` | Boolean | 否 | 是否启用（默认true） |

**示例**：
```yaml
- name: "code-quality-basic"
  type: "code_quality"
  weight: 1.0
  strategy: "keyword_matching"
  enabled: true
  positive_keywords:
    "单元测试": 20
    "集成测试": 15
  negative_keywords:
    "代码重复": -15
```

### 3. AST 分析配置 (ast_analysis)

配置 AST 代码分析的阈值。

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `enabled` | Boolean | 否 | 是否启用AST分析 |
| `thresholds` | Map | 否 | 各种阈值配置 |

**阈值说明**：
- `long_method`: 长方法阈值（行数）
- `high_complexity`: 高复杂度阈值
- `god_class_methods`: 上帝类方法数阈值
- `god_class_fields`: 上帝类字段数阈值
- `too_many_parameters`: 参数过多阈值

**示例**：
```yaml
ast_analysis:
  enabled: true
  thresholds:
    long_method: 50
    high_complexity: 10
    god_class_methods: 20
```

## 命名风格支持

配置文件同时支持**驼峰命名**和**下划线命名**两种风格：

### 驼峰命名（camelCase）
```json
{
  "displayName": "代码质量",
  "positiveKeywords": {...},
  "astAnalysis": {...}
}
```

### 下划线命名（snake_case）
```yaml
display_name: "代码质量"
positive_keywords: {...}
ast_analysis: {...}
```

## 最佳实践

### 1. 权重分配建议

维度权重总和应该为 1.0：

```yaml
dimensions:
  code_quality:
    weight: 0.40  # 40%
  innovation:
    weight: 0.30  # 30%
  completeness:
    weight: 0.20  # 20%
  documentation:
    weight: 0.10  # 10%
```

### 2. 禁用不需要的维度

通过 `enabled: false` 禁用维度：

```yaml
media_quality:
  weight: 0.15
  display_name: "媒体质量"
  enabled: false  # 暂时不启用
```

### 3. 为不同比赛创建不同配置

```bash
configs/
├── hackathon-ai.yaml       # AI主题黑客松
├── hackathon-web.yaml      # Web开发黑客松
├── hackathon-mobile.yaml   # 移动应用黑客松
└── hackathon-game.yaml     # 游戏开发黑客松
```

### 4. 版本控制

将配置文件纳入版本控制：

```bash
git add configs/hackathon-scoring.yaml
git commit -m "feat: 添加黑客松评分配置"
```

## 故障排查

### 问题 1：配置文件未找到

```
WARN: 配置文件不存在: config.yaml, 使用默认配置
```

**解决方案**：检查文件路径是否正确。

### 问题 2：权重总和不为1.0

```
WARN: 维度权重总和不为1.0: 0.85
```

**解决方案**：调整各维度权重使总和为1.0，或者忽略此警告（系统仍可正常运行）。

### 问题 3：不支持的文件格式

```
ERROR: 不支持的配置文件格式: config.txt
```

**解决方案**：使用 .yaml、.yml 或 .json 格式。

## 进阶：扩展支持多文件类型

配置文件预留了多文件类型支持的配置项：

```yaml
file_type_configs:
  image:
    enabled: false
    supported_formats: ["jpg", "png", "gif"]
    max_size_mb: 10
    quality_check:
      min_resolution: [800, 600]
      max_resolution: [4096, 4096]
  
  video:
    enabled: false
    supported_formats: ["mp4", "avi", "mov"]
    max_size_mb: 100
    quality_check:
      min_duration_seconds: 10
      max_duration_seconds: 600
```

这些配置为未来的多文件类型分析预留了接口。

## 测试

运行测试验证配置加载功能：

```bash
mvn test -Dtest=HackathonScoringConfigLoaderTest
```

## 总结

- ✅ 支持 YAML 和 JSON 两种格式
- ✅ 支持驼峰和下划线两种命名风格
- ✅ 支持动态启用/禁用维度和规则
- ✅ 支持自定义权重和阈值
- ✅ 为未来多文件类型支持预留扩展

---

**更新日期**: 2025-11-15  
**功能状态**: ✅ 已实现并测试通过

