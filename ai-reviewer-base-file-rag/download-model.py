#!/usr/bin/env python3
"""
向量嵌入模型下载脚本
自动下载并转换 Sentence-BERT 模型到 ONNX 格式
"""

import os
import sys

def check_dependencies():
    """检查依赖"""
    try:
        import transformers
        import optimum.onnxruntime
        print("✅ 依赖检查通过")
        return True
    except ImportError as e:
        print("❌ 缺少依赖包，请先安装：")
        print("   pip install transformers optimum onnxruntime")
        return False

def download_chinese_model():
    """下载中文模型"""
    from optimum.onnxruntime import ORTModelForFeatureExtraction
    from transformers import AutoTokenizer

    print("\n📥 开始下载中文模型...")
    print("   模型: shibing624/text2vec-base-chinese")
    print("   这可能需要几分钟...\n")

    model_name = "shibing624/text2vec-base-chinese"
    output_dir = "./models/text2vec-base-chinese"

    try:
        # 下载并转换模型
        print("⏳ 下载模型...")
        model = ORTModelForFeatureExtraction.from_pretrained(
            model_name,
            export=True,
            use_auth_token=False
        )

        print("⏳ 下载分词器...")
        tokenizer = AutoTokenizer.from_pretrained(model_name)

        # 保存到本地
        print(f"⏳ 保存到 {output_dir}...")
        os.makedirs(output_dir, exist_ok=True)
        model.save_pretrained(output_dir)
        tokenizer.save_pretrained(output_dir)

        print(f"\n✅ 模型下载成功！")
        print(f"   保存位置: {os.path.abspath(output_dir)}")
        print(f"   模型文件: {os.path.join(output_dir, 'model.onnx')}")

        # 验证文件
        model_file = os.path.join(output_dir, "model.onnx")
        if os.path.exists(model_file):
            size_mb = os.path.getsize(model_file) / 1024 / 1024
            print(f"   文件大小: {size_mb:.1f} MB")
            return True
        else:
            print("❌ 错误：model.onnx 文件未生成")
            return False

    except Exception as e:
        print(f"\n❌ 下载失败: {e}")
        return False

def download_english_model():
    """下载英文模型"""
    from optimum.onnxruntime import ORTModelForFeatureExtraction
    from transformers import AutoTokenizer

    print("\n📥 开始下载英文模型...")
    print("   模型: sentence-transformers/all-MiniLM-L6-v2")

    model_name = "sentence-transformers/all-MiniLM-L6-v2"
    output_dir = "./models/all-MiniLM-L6-v2"

    try:
        model = ORTModelForFeatureExtraction.from_pretrained(model_name, export=True)
        tokenizer = AutoTokenizer.from_pretrained(model_name)

        os.makedirs(output_dir, exist_ok=True)
        model.save_pretrained(output_dir)
        tokenizer.save_pretrained(output_dir)

        print(f"\n✅ 英文模型下载成功！")
        print(f"   保存位置: {os.path.abspath(output_dir)}")
        return True

    except Exception as e:
        print(f"\n❌ 下载失败: {e}")
        return False

def main():
    print("=" * 80)
    print("🚀 向量嵌入模型下载工具")
    print("=" * 80)

    # 检查依赖
    if not check_dependencies():
        sys.exit(1)

    # 选择模型
    print("\n请选择要下载的模型：")
    print("  1. 中文模型 (text2vec-base-chinese) - 推荐")
    print("  2. 英文模型 (all-MiniLM-L6-v2)")
    print("  3. 两者都下载")

    choice = input("\n请输入选择 (1/2/3): ").strip()

    success = False
    if choice == "1":
        success = download_chinese_model()
    elif choice == "2":
        success = download_english_model()
    elif choice == "3":
        success1 = download_chinese_model()
        success2 = download_english_model()
        success = success1 or success2
    else:
        print("❌ 无效选择")
        sys.exit(1)

    if success:
        print("\n" + "=" * 80)
        print("✅ 下载完成！现在可以运行向量检索系统了")
        print("=" * 80)
        print("\n运行命令：")
        print("  mvn exec:java -Dexec.mainClass=top.yumbo.ai.rag.example.ExcelKnowledgeQASystem")
    else:
        print("\n❌ 下载失败，请检查网络连接或手动下载")
        sys.exit(1)

if __name__ == "__main__":
    main()

