package top.yumbo.ai.rag.impl.embedding;

import ai.onnxruntime.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 本地向量嵌入引擎
 * 使用 ONNX Runtime 运行本地 Sentence-BERT 模型
 *
 * 支持的模型：
 * - text2vec-base-chinese (中文，384维)
 * - all-MiniLM-L6-v2 (英文，384维)
 * - paraphrase-multilingual-MiniLM-L12-v2 (多语言，384维)
 *
 * P0修复：解决缺少向量嵌入能力的问题
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
@Slf4j
public class LocalEmbeddingEngine implements AutoCloseable {

    private final OrtEnvironment env;
    private final OrtSession session;
    private final int embeddingDim;
    private final String modelName;
    private final int maxSequenceLength;

    // 常量
    private static final int DEFAULT_MAX_SEQUENCE_LENGTH = 512;
    private static final String DEFAULT_MODEL_PATH = "models/text2vec-base-chinese/model.onnx";

    /**
     * 使用默认模型路径构造
     */
    public LocalEmbeddingEngine() throws OrtException, IOException {
        this(DEFAULT_MODEL_PATH);
    }

    /**
     * 指定模型路径构造
     *
     * @param modelPath ONNX模型文件路径
     */
    public LocalEmbeddingEngine(String modelPath) throws OrtException, IOException {
        this(modelPath, DEFAULT_MAX_SEQUENCE_LENGTH);
    }

    /**
     * 完整构造函数
     *
     * @param modelPath ONNX模型文件路径
     * @param maxSequenceLength 最大序列长度
     */
    public LocalEmbeddingEngine(String modelPath, int maxSequenceLength)
            throws OrtException, IOException {

        Path modelFile = Paths.get(modelPath);
        if (!Files.exists(modelFile)) {
            throw new IOException(String.format(
                "模型文件不存在: %s\n" +
                "请下载模型文件到该路径。\n" +
                "推荐模型：\n" +
                "  中文：https://huggingface.co/shibing624/text2vec-base-chinese\n" +
                "  英文：https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2",
                modelPath
            ));
        }

        this.maxSequenceLength = maxSequenceLength;
        this.modelName = modelFile.getParent().getFileName().toString();

        // 初始化 ONNX Runtime 环境
        this.env = OrtEnvironment.getEnvironment();

        // 配置会话选项
        OrtSession.SessionOptions options = new OrtSession.SessionOptions();
        options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
        options.setInterOpNumThreads(4); // 使用4个线程加速推理
        options.setIntraOpNumThreads(4);

        // 加载模型
        this.session = env.createSession(modelPath, options);

        // 获取输出维度
        this.embeddingDim = inferEmbeddingDimension();

        log.info("✅ 本地嵌入模型已加载");
        log.info("   - 模型: {}", modelName);
        log.info("   - 路径: {}", modelPath);
        log.info("   - 维度: {}", embeddingDim);
        log.info("   - 最大序列长度: {}", maxSequenceLength);
    }

    /**
     * 将文本转换为向量
     *
     * @param text 输入文本
     * @return 嵌入向量（已归一化）
     */
    public float[] embed(String text) {
        if (text == null || text.trim().isEmpty()) {
            log.warn("输入文本为空，返回零向量");
            return new float[embeddingDim];
        }

        try {
            // 1. 分词（简化版，生产环境建议使用 HuggingFace Tokenizers）
            long[] inputIds = tokenize(text);
            long[] attentionMask = createAttentionMask(inputIds);
            long[] tokenTypeIds = createTokenTypeIds(inputIds); // 🔧 修复：添加 token_type_ids

            // 2. 构建 ONNX 输入张量
            long[][] inputIdsArray = new long[][]{inputIds};
            long[][] attentionMaskArray = new long[][]{attentionMask};
            long[][] tokenTypeIdsArray = new long[][]{tokenTypeIds};

            OnnxTensor inputIdsTensor = OnnxTensor.createTensor(env, inputIdsArray);
            OnnxTensor attentionMaskTensor = OnnxTensor.createTensor(env, attentionMaskArray);
            OnnxTensor tokenTypeIdsTensor = OnnxTensor.createTensor(env, tokenTypeIdsArray);

            Map<String, OnnxTensor> inputs = new HashMap<>();
            inputs.put("input_ids", inputIdsTensor);
            inputs.put("attention_mask", attentionMaskTensor);
            inputs.put("token_type_ids", tokenTypeIdsTensor); // 🔧 修复：添加到输入

            // 3. 模型推理
            OrtSession.Result result = session.run(inputs);

            // 4. 提取输出向量
            float[][] embeddings = (float[][]) result.get(0).getValue();
            float[] vector = embeddings[0]; // 取第一个样本（批量大小=1）

            // 5. L2 归一化（余弦相似度需要）
            float[] normalized = l2Normalize(vector);

            // 清理资源
            inputIdsTensor.close();
            attentionMaskTensor.close();
            tokenTypeIdsTensor.close();
            result.close();

            log.trace("文本嵌入完成: {} chars -> {} dims", text.length(), embeddingDim);

            return normalized;

        } catch (OrtException e) {
            log.error("嵌入生成失败: {}", text.substring(0, Math.min(50, text.length())), e);
            return new float[embeddingDim]; // 返回零向量
        }
    }

    /**
     * 批量嵌入（提高性能）
     *
     * @param texts 文本列表
     * @return 向量列表
     */
    public List<float[]> embedBatch(List<String> texts) {
        List<float[]> vectors = new ArrayList<>();
        for (String text : texts) {
            vectors.add(embed(text));
        }
        return vectors;
    }

    /**
     * 简化的分词器（基于字符级别）
     *
     * 注意：这是简化实现，生产环境建议使用：
     * - HuggingFace Tokenizers
     * - 或预先使用 Python 生成 token IDs
     */
    private long[] tokenize(String text) {
        // 简化策略：
        // 1. 截断到最大长度
        // 2. 使用字符的 Unicode 编码作为 token ID

        char[] chars = text.toCharArray();
        int length = Math.min(chars.length, maxSequenceLength - 2); // 预留 [CLS] 和 [SEP]

        long[] tokens = new long[length + 2];
        tokens[0] = 101; // [CLS] token

        for (int i = 0; i < length; i++) {
            // 将字符映射到词汇表范围 (101-30000)
            tokens[i + 1] = (chars[i] % 29900) + 101;
        }

        tokens[length + 1] = 102; // [SEP] token

        return tokens;
    }

    /**
     * 创建注意力掩码（全1，表示所有token都有效）
     */
    private long[] createAttentionMask(long[] inputIds) {
        long[] mask = new long[inputIds.length];
        Arrays.fill(mask, 1L);
        return mask;
    }

    /**
     * 创建 token type IDs（全0，表示单句输入）
     * 用于区分句子对，对于单句任务，全部填充0即可
     */
    private long[] createTokenTypeIds(long[] inputIds) {
        long[] tokenTypeIds = new long[inputIds.length];
        Arrays.fill(tokenTypeIds, 0L);
        return tokenTypeIds;
    }

    /**
     * L2 归一化
     */
    private float[] l2Normalize(float[] vector) {
        double sumSquares = 0;
        for (float v : vector) {
            sumSquares += v * v;
        }

        double norm = Math.sqrt(sumSquares);
        if (norm < 1e-10) {
            return vector; // 避免除零
        }

        float[] normalized = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalized[i] = (float) (vector[i] / norm);
        }

        return normalized;
    }

    /**
     * 推断嵌入维度
     */
    private int inferEmbeddingDimension() throws OrtException {
        try {
            // 使用测试输入推断输出维度
            long[][] testInput = new long[][]{{101, 102}}; // [CLS] [SEP]
            long[][] testMask = new long[][]{{1, 1}};
            long[][] testTokenTypeIds = new long[][]{{0, 0}}; // 🔧 修复：添加 token_type_ids

            OnnxTensor inputTensor = OnnxTensor.createTensor(env, testInput);
            OnnxTensor maskTensor = OnnxTensor.createTensor(env, testMask);
            OnnxTensor tokenTypeIdsTensor = OnnxTensor.createTensor(env, testTokenTypeIds);

            Map<String, OnnxTensor> inputs = new HashMap<>();
            inputs.put("input_ids", inputTensor);
            inputs.put("attention_mask", maskTensor);
            inputs.put("token_type_ids", tokenTypeIdsTensor); // 🔧 修复：添加到输入

            OrtSession.Result result = session.run(inputs);
            float[][] output = (float[][]) result.get(0).getValue();

            int dim = output[0].length;

            inputTensor.close();
            maskTensor.close();
            tokenTypeIdsTensor.close();
            result.close();

            return dim;

        } catch (OrtException e) {
            log.warn("无法推断维度，使用默认值 384");
            return 384; // 默认维度
        }
    }

    /**
     * 获取嵌入维度
     */
    public int getEmbeddingDim() {
        return embeddingDim;
    }

    /**
     * 获取模型名称
     */
    public String getModelName() {
        return modelName;
    }

    @Override
    public void close() {
        try {
            if (session != null) {
                session.close();
            }
            log.info("嵌入引擎已关闭");
        } catch (OrtException e) {
            log.error("关闭嵌入引擎失败", e);
        }
    }

    /**
     * 测试方法
     */
    public static void main(String[] args) {
        try {
            LocalEmbeddingEngine engine = new LocalEmbeddingEngine();

            String text = "人工智能正在改变世界";
            float[] vector = engine.embed(text);

            System.out.println("文本: " + text);
            System.out.println("向量维度: " + vector.length);
            System.out.println("向量前10维: " + Arrays.toString(
                Arrays.copyOf(vector, Math.min(10, vector.length))
            ));

            engine.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

