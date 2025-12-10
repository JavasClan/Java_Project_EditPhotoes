package imgedit.core.operations;

import imgedit.core.ImageOperation;
import imgedit.core.exceptions.ImageProcessingException;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 智能修复操作实现类（内容感知填充）
 *
 * 功能说明：
 * - 移除图片中的不需要的物体
 * - 基于周围像素智能填充移除区域
 * - 保持纹理和结构的连续性
 *
 * 算法原理：
 * - 使用PatchMatch或类似算法
 * - 基于纹理合成技术
 * - 边缘感知的像素填充
 */
public class ContentAwareFillOperation implements ImageOperation {

    private final Rectangle fillArea;
    private final FillAlgorithm algorithm;

    /**
     * 填充算法枚举
     */
    public enum FillAlgorithm {
        PATCH_BASED("基于块匹配"),
        DIFFUSION_BASED("基于扩散"),
        HYBRID("混合算法");

        private final String description;

        FillAlgorithm(String description) {
            this.description = description;
        }

        public String getDescription() { return description; }
    }

    /**
     * 构造函数
     */
    public ContentAwareFillOperation(Rectangle fillArea, FillAlgorithm algorithm) {
        validateFillArea(fillArea);
        this.fillArea = new Rectangle(fillArea);
        this.algorithm = algorithm;
    }

    /**
     * 便捷工厂方法
     */
    public static ContentAwareFillOperation create(Rectangle fillArea) {
        return new ContentAwareFillOperation(fillArea, FillAlgorithm.HYBRID);
    }

    @Override
    public BufferedImage apply(BufferedImage image) throws ImageProcessingException {
        try {
            int width = image.getWidth();
            int height = image.getHeight();

            // 验证填充区域在图片范围内
            Rectangle validArea = getValidArea(image, fillArea);
            if (validArea.width <= 0 || validArea.height <= 0) {
                throw new ImageProcessingException("填充区域无效或超出图片范围");
            }

            // 创建结果图片
            BufferedImage result = new BufferedImage(width, height, image.getType());

            // 复制原始图片
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    result.setRGB(x, y, image.getRGB(x, y));
                }
            }

            // 根据算法选择填充方法
            switch (algorithm) {
                case PATCH_BASED:
                    applyPatchBasedFill(result, validArea);
                    break;
                case DIFFUSION_BASED:
                    applyDiffusionBasedFill(result, validArea);
                    break;
                case HYBRID:
                    applyHybridFill(result, validArea);
                    break;
            }

            return result;

        } catch (Exception e) {
            throw new ImageProcessingException("智能填充失败: " + e.getMessage(), e);
        }
    }

    /**
     * 基于块匹配的填充算法
     */
    private void applyPatchBasedFill(BufferedImage image, Rectangle area) {
        int patchSize = 5; // 块大小
        int searchRadius = 20; // 搜索半径

        // 获取填充区域周围的样本像素
        List<int[]> samplePatches = collectSamplePatches(image, area, patchSize);

        // 对填充区域内的每个像素进行填充
        for (int y = area.y; y < area.y + area.height; y++) {
            for (int x = area.x; x < area.x + area.width; x++) {
                // 寻找最佳匹配块
                int[] bestPatch = findBestPatch(image, x, y, samplePatches, patchSize, searchRadius);

                if (bestPatch != null) {
                    // 使用最佳匹配块的中心像素填充
                    image.setRGB(x, y, bestPatch[patchSize * patchSize / 2]);
                }
            }
        }
    }

    /**
     * 收集样本块
     */
    private List<int[]> collectSamplePatches(BufferedImage image, Rectangle excludeArea, int patchSize) {
        List<int[]> patches = new ArrayList<>();
        int width = image.getWidth();
        int height = image.getHeight();
        int halfPatch = patchSize / 2;

        // 在排除区域周围采集样本
        int margin = 10;
        for (int y = margin; y < height - margin; y += 2) {
            for (int x = margin; x < width - margin; x += 2) {
                // 确保不在排除区域内
                if (!excludeArea.contains(x, y)) {
                    int[] patch = extractPatch(image, x, y, patchSize);
                    if (patch != null) {
                        patches.add(patch);
                    }
                }
            }
        }

        return patches;
    }

    /**
     * 提取像素块
     */
    private int[] extractPatch(BufferedImage image, int centerX, int centerY, int patchSize) {
        int halfSize = patchSize / 2;
        int[] patch = new int[patchSize * patchSize];
        int index = 0;

        for (int dy = -halfSize; dy <= halfSize; dy++) {
            for (int dx = -halfSize; dx <= halfSize; dx++) {
                int x = centerX + dx;
                int y = centerY + dy;

                if (x >= 0 && x < image.getWidth() && y >= 0 && y < image.getHeight()) {
                    patch[index++] = image.getRGB(x, y);
                } else {
                    return null; // 块不完整
                }
            }
        }

        return patch;
    }

    /**
     * 寻找最佳匹配块
     */
    private int[] findBestPatch(BufferedImage image, int targetX, int targetY,
                                List<int[]> patches, int patchSize, int searchRadius) {
        int[] bestPatch = null;
        double bestScore = Double.MAX_VALUE;
        Random random = new Random();

        // 随机采样多个块进行评估
        int sampleCount = Math.min(100, patches.size());
        for (int i = 0; i < sampleCount; i++) {
            int[] patch = patches.get(random.nextInt(patches.size()));
            double score = calculatePatchSimilarity(image, targetX, targetY, patch, patchSize);

            if (score < bestScore) {
                bestScore = score;
                bestPatch = patch;
            }
        }

        return bestPatch;
    }

    /**
     * 计算块相似度
     */
    private double calculatePatchSimilarity(BufferedImage image, int centerX, int centerY,
                                            int[] patch, int patchSize) {
        double totalDifference = 0;
        int halfSize = patchSize / 2;
        int validPixels = 0;

        for (int dy = -halfSize; dy <= halfSize; dy++) {
            for (int dx = -halfSize; dx <= halfSize; dx++) {
                int x = centerX + dx;
                int y = centerY + dy;

                if (x >= 0 && x < image.getWidth() && y >= 0 && y < image.getHeight()) {
                    int pixelIndex = (dy + halfSize) * patchSize + (dx + halfSize);
                    int sourcePixel = image.getRGB(x, y);
                    int patchPixel = patch[pixelIndex];

                    // 计算像素差异
                    double diff = calculatePixelDifference(sourcePixel, patchPixel);
                    totalDifference += diff;
                    validPixels++;
                }
            }
        }

        return validPixels > 0 ? totalDifference / validPixels : Double.MAX_VALUE;
    }

    /**
     * 计算像素差异
     */
    private double calculatePixelDifference(int pixel1, int pixel2) {
        int r1 = (pixel1 >> 16) & 0xFF;
        int g1 = (pixel1 >> 8) & 0xFF;
        int b1 = pixel1 & 0xFF;

        int r2 = (pixel2 >> 16) & 0xFF;
        int g2 = (pixel2 >> 8) & 0xFF;
        int b2 = pixel2 & 0xFF;

        // 欧氏距离
        return Math.sqrt((r1 - r2) * (r1 - r2) +
                (g1 - g2) * (g1 - g2) +
                (b1 - b2) * (b1 - b2));
    }

    /**
     * 基于扩散的填充算法（快速但效果一般）
     */
    private void applyDiffusionBasedFill(BufferedImage image, Rectangle area) {
        // 多轮迭代扩散
        int iterations = 10;
        for (int i = 0; i < iterations; i++) {
            diffuseRegion(image, area);
        }
    }

    /**
     * 区域扩散
     */
    private void diffuseRegion(BufferedImage image, Rectangle area) {
        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage temp = new BufferedImage(width, height, image.getType());

        // 复制图片
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                temp.setRGB(x, y, image.getRGB(x, y));
            }
        }

        // 对填充区域进行扩散
        for (int y = area.y; y < area.y + area.height; y++) {
            for (int x = area.x; x < area.x + area.width; x++) {
                // 计算周围像素的平均值
                int avgColor = calculateNeighborhoodAverage(image, x, y, 1);
                temp.setRGB(x, y, avgColor);
            }
        }

        // 更新原图
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, temp.getRGB(x, y));
            }
        }
    }

    /**
     * 计算邻域平均值
     */
    private int calculateNeighborhoodAverage(BufferedImage image, int centerX, int centerY, int radius) {
        int totalRed = 0, totalGreen = 0, totalBlue = 0, totalAlpha = 0;
        int count = 0;

        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int x = centerX + dx;
                int y = centerY + dy;

                if (x >= 0 && x < image.getWidth() && y >= 0 && y < image.getHeight()) {
                    int pixel = image.getRGB(x, y);
                    totalAlpha += (pixel >> 24) & 0xFF;
                    totalRed += (pixel >> 16) & 0xFF;
                    totalGreen += (pixel >> 8) & 0xFF;
                    totalBlue += pixel & 0xFF;
                    count++;
                }
            }
        }

        if (count == 0) return 0;

        int avgAlpha = totalAlpha / count;
        int avgRed = totalRed / count;
        int avgGreen = totalGreen / count;
        int avgBlue = totalBlue / count;

        return (avgAlpha << 24) | (avgRed << 16) | (avgGreen << 8) | avgBlue;
    }

    /**
     * 混合算法（结合块匹配和扩散）
     */
    private void applyHybridFill(BufferedImage image, Rectangle area) {
        // 先用扩散算法进行初步填充
        applyDiffusionBasedFill(image, area);

        // 再用块匹配算法进行优化
        applyPatchBasedFill(image, area);
    }

    /**
     * 验证填充区域
     */
    private void validateFillArea(Rectangle area) {
        if (area.width <= 0 || area.height <= 0) {
            throw new IllegalArgumentException("填充区域尺寸无效");
        }
    }

    /**
     * 获取有效区域
     */
    private Rectangle getValidArea(BufferedImage image, Rectangle desiredArea) {
        int width = image.getWidth();
        int height = image.getHeight();

        int x = Math.max(0, Math.min(desiredArea.x, width - 1));
        int y = Math.max(0, Math.min(desiredArea.y, height - 1));
        int w = Math.min(desiredArea.width, width - x);
        int h = Math.min(desiredArea.height, height - y);

        return new Rectangle(x, y, w, h);
    }

    @Override
    public String getOperationName() {
        return String.format("智能填充 [区域:%d,%d,%d,%d, 算法:%s]",
                fillArea.x, fillArea.y, fillArea.width, fillArea.height,
                algorithm.getDescription());
    }
}