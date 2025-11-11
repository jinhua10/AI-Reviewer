# 🚀 黑客松AI评审工具 - 快速使用指南

## 📋 准备工作

### 1. 环境要求
- ✅ **JDK 17+**: 确保Java环境正确安装
- ✅ **网络连接**: 需要访问DeepSeek AI服务
- ✅ **API密钥**: 需要有效的DeepSeek API密钥

### 2. 验证环境
```bash
# 检查Java版本
java -version

# 预期输出: java version "17.0.x" 或更高版本
```

### 3. 设置API密钥
```bash
# 设置环境变量 (Windows)
setx DEEPSEEK_API_KEY "your-api-key-here"

# 或在Linux/macOS上
export DEEPSEEK_API_KEY="your-api-key-here"
```

## 🎯 立即开始评审

### 方法1: 使用命令行工具 (推荐)

#### 步骤1: 编译工具
```bash
cd /path/to/ai-reviewer
mvn clean compile
```

#### 步骤2: 快速评审您的项目
```bash
# 基本评审 (自动选择模式)
java -cp target/classes top.yumbo.ai.reviewer.HackathonReviewer

# 但更好的方式是使用我们的演示程序
java -cp target/classes top.yumbo.ai.reviewer.HackathonDemo
```

#### 步骤3: 高级评审选项
```bash
# 快速评审 (10秒)
java -cp target/classes top.yumbo.ai.reviewer.HackathonCLI review /path/to/your/project QUICK

# 详细评审 (30秒)
java -cp target/classes top.yumbo.ai.reviewer.HackathonCLI review /path/to/your/project DETAILED

# 专家评审 (60秒)
java -cp target/classes top.yumbo.ai.reviewer.HackathonCLI review /path/to/your/project EXPERT
```

### 方法2: 使用Java代码集成

#### 创建评审程序
```java
import top.yumbo.ai.reviewer.*;
import top.yumbo.ai.reviewer.HackathonReviewer.ReviewMode;

public class MyProjectReviewer {
    public static void main(String[] args) throws Exception {
        // 1. 创建评审器
        HackathonReviewer reviewer = new HackathonReviewer();

        // 2. 评审您的项目
        String projectPath = "/path/to/your/project";
        HackathonReviewer.HackathonScore score = reviewer.smartReview(projectPath);

        // 3. 显示结果
        System.out.println("项目评分结果:");
        System.out.println("项目名称: " + score.getProjectName());
        System.out.println("总评分: " + score.getTotalScore() + "/100");
        System.out.println("评审状态: " + score.getJudgeStatus());
        System.out.println("评审模式: " + score.getReviewMode().getDisplayName());

        // 4. 生成报告
        reviewer.generateReviewReport(score, "review-report.md", score.getReviewMode());

        System.out.println("评审报告已生成: review-report.md");
    }
}
```

#### 编译并运行
```bash
# 编译
javac -cp "target/classes:." MyProjectReviewer.java

# 运行
java -cp "target/classes:." MyProjectReviewer
```

## 📊 评审结果解读

### 评分维度说明
```
🏗️ 架构设计 (15-20%): 代码结构、设计模式、扩展性
💻 代码质量 (20%): 规范性、注释、异常处理、命名
🔧 技术债务 (10-15%): 过时技术、安全问题、维护难度
⚙️ 功能完整性 (25%): 需求实现、边界处理、用户体验
💰 商业价值 (15-20%): 市场潜��、创新程度、竞争优势
🧪 测试覆盖率 (5-10%): 单元测试、集成测试、测试质量
🚀 创新性 (10%): 技术创新、解决方案创新 (专家模式)
```

### 评审状态等级
- 🏆 **优秀 (90-100分)**: 技术出众，功能完整，创新突出
- 🥈 **良好 (75-89分)**: 技术扎实，功能较好，有创新点
- 🥉 **及格 (60-74分)**: 技术基础，功能基本，可改进
- 📜 **待改进 (<60分)**: 需要大幅改进，基础薄弱

## 🎯 评审模式选择指南

### 快速评审 (QUICK)
**适用场景**: 初次评估，快速了解项目概况
**分析时间**: 10秒
**适合项目**: 任何大小的项目
**输出内容**: 基础评分 + 总体评价

### 详细评审 (DETAILED)
**适用场景**: 深入评估，准备提交评审
**分析时间**: 30秒
**适合项目**: 中小型项目 (建议<100个文件)
**输出内容**: 全面评分 + 技术分析 + 改进建议

### 专家评审 (EXPERT)
**适用场景**: 专业评审，决赛评估
**分析时间**: 60秒
**适合项目**: 重要项目，需要深度分析
**输出内容**: 专家级分析 + 详细技术报告 + 评审意见

## 📄 生成评审报告

### 自动生成报告
```bash
# 评审完成后自动生成
java -cp target/classes top.yumbo.ai.reviewer.HackathonCLI review /path/to/project DETAILED

# 报告文件: hackathon-detailed-report.md
```

### 报告内容包含
- 📊 **评分总览**: 总体评分和评审状态
- 📈 **维度评分**: 各技术维度的详细评分
- 🔍 **技术分析**: 架构、代码质量等深度分析
- 💡 **改进建议**: 具体的技术和产品建议
- 🏆 **评审结论**: 明确的评审意见和建议

## 🛠️ 故障排除

### 常见问题

#### 1. Java版本问题
```bash
# 检查Java版本
java -version

# 如果版本低于17，请升级JDK
# 下载地址: https://adoptium.net/
```

#### 2. API密钥问题
```bash
# 验证API密钥设置
echo %DEEPSEEK_API_KEY%

# 如果未设置，请设置环境变量
setx DEEPSEEK_API_KEY "your-api-key-here"
```

#### 3. 网络连接问题
```bash
# 测试网络连接
ping api.deepseek.com

# 如果无法连接，请检查网络设置
```

#### 4. 项目路径问题
```bash
# 使用绝对路径
java -cp target/classes top.yumbo.ai.reviewer.HackathonCLI review "C:\Users\YourName\Projects\MyProject" QUICK

# 或使用相对路径
java -cp target/classes top.yumbo.ai.reviewer.HackathonCLI review .\MyProject QUICK
```

#### 5. 内存不足问题
```bash
# 增加JVM内存
java -Xmx2g -cp target/classes top.yumbo.ai.reviewer.HackathonCLI review /path/to/project DETAILED
```

## 📞 获取帮助

### 技术支持
- 📧 **邮箱**: support@hackathon-ai-reviewer.com
- 📚 **文档**: [评审指南](HACKATHON-REVIEW-GUIDE.md)
- 🐛 **问题反馈**: [GitHub Issues](https://github.com/jinhua10/ai-reviewer/issues)

### 快速诊断
```bash
# 运行诊断程序
java -cp target/classes top.yumbo.ai.reviewer.HackathonValidation
```

## 🎉 成功案例

### 示例评审结果
```
项目名称: MyAwesomeProject
评审模式: 详细评审
总评分: 87.5/100
评审状态: 🥈 良好 - 晋级复赛

详细评分:
├─ 架构设计: 85/100
├─ 代码质量: 88/100
├─ 功能完整性: 90/100
├─ 商业价值: 82/100
├─ 测试覆盖率: 75/100
└─ 创新性: 80/100
```

---

**🎯 现在就开始评审您的项目吧！**

只需几秒钟，您就能获得专业的AI评审结果和改进建议。🚀</content>
<parameter name="filePath">D:\Jetbrains\hackathon\AI-Reviewer\QUICK-START-GUIDE.md
