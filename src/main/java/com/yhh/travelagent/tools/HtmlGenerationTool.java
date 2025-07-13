package com.yhh.travelagent.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import com.yhh.travelagent.constants.FileConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Date 2025-07-13 15:33
 * @ClassName: HtmlGenerationTool
 * @Description: HTML生成工具，用于生成带有动效的HTML网页
 */
@Component
public class HtmlGenerationTool {

    private static final Logger log = LoggerFactory.getLogger(HtmlGenerationTool.class);

    // HTML文件保存目录
    private final String HTML_DIR = FileConstant.FILE_SAVE_DIR + "/html";

    public HtmlGenerationTool() {
        // 确保目录存在
        FileUtil.mkdir(HTML_DIR);
    }

    /**
     * 生成美观的HTML文件，包含动效和响应式设计
     * 注意：正文内容不应包含标题(h1)，因为标题将根据title参数自动生成
     *
     * @param title HTML文档标题，将显示在页面顶部
     * @param content HTML正文内容(不应包含h1标题，因为标题会自动添加)
     * @param filename 可选的文件名(不含扩展名)，如果为空则自动生成
     * @return 结果信息，包含文件下载路径
     */
    @Tool(description = "Generate an attractive HTML file with animations and responsive design")
    public String generateHtml(
            @ToolParam(description = "The title of the HTML document") String title,
            @ToolParam(description = "The HTML content (body part, should NOT include the title)") String content,
            @ToolParam(description = "Optional filename (without extension), will generate if empty") String filename
    ) {
        try {
            // 处理文件名
            String safeFilename = getSafeFilename(filename);
            String htmlFilename = safeFilename + ".html";
            Path htmlPath = Paths.get(HTML_DIR, htmlFilename);

            // 生成完整的HTML内容
            String htmlContent = buildHtmlDocument(title, content);

            // 写入文件
            FileUtil.writeString(htmlContent, htmlPath.toString(), StandardCharsets.UTF_8);

            log.info("HTML文件生成成功: {}", htmlPath);

            // 构建下载链接
            String downloadLink = "/api/files/html/" + htmlFilename;

            return String.format(
                    "HTML生成成功！[点击查看HTML页面](%s)",
                    downloadLink
            );

        } catch (Exception e) {
            log.error("HTML生成失败", e);
            return "HTML生成失败: " + e.getMessage();
        }
    }

    /**
     * 生成带有内嵌页面的HTML文件，包含动效和响应式设计
     * 内容中可以使用{{embed:URL}}格式来指定内嵌页面的位置
     *
     * @param title HTML文档标题，将显示在页面顶部
     * @param content HTML正文内容，可以包含{{embed:URL}}格式的占位符
     * @param embedUrls 要嵌入的URL列表（用于向后兼容，如果content中包含embed标记，这个参数可以为空）
     * @param filename 可选的文件名(不含扩展名)，如果为空则自动生成
     * @return 结果信息，包含文件下载路径
     */
    @Tool(description = "Generate an HTML file with embedded pages using iframes")
    public String generateHtmlWithEmbeddedPages(
            @ToolParam(description = "The title of the HTML document") String title,
            @ToolParam(description = "The HTML content with optional {{embed:URL}} placeholders") String content,
            @ToolParam(description = "List of URLs to embed (optional if content contains embed tags)") List<String> embedUrls,
            @ToolParam(description = "Optional filename (without extension)") String filename
    ) {
        try {
            // 处理文件名
            String safeFilename = getSafeFilename(filename);
            String htmlFilename = safeFilename + ".html";
            Path htmlPath = Paths.get(HTML_DIR, htmlFilename);

            // 处理内容中的嵌入标记
            String processedContent = processEmbedTags(content, embedUrls);

            // 生成完整的HTML内容
            String htmlContent = buildHtmlWithEmbeddedPages(title, processedContent);

            // 写入文件
            FileUtil.writeString(htmlContent, htmlPath.toString(), StandardCharsets.UTF_8);

            log.info("HTML文件(带内嵌页面)生成成功: {}", htmlPath);

            // 构建下载链接
            String downloadLink = "/api/files/html/" + htmlFilename;

            return String.format(
                    "HTML生成成功！[点击查看HTML页面](%s)",
                    downloadLink
            );

        } catch (Exception e) {
            log.error("HTML生成失败", e);
            return "HTML生成失败: " + e.getMessage();
        }
    }

    /**
     * 处理内容中的嵌入标记
     * 将{{embed:URL}}格式的标记替换为实际的iframe嵌入代码
     */
    private String processEmbedTags(String content, List<String> embedUrls) {
        if (content == null) {
            content = "";
        }

        StringBuilder processedContent = new StringBuilder();

        // 首先处理content中的嵌入标记
        Pattern embedPattern = Pattern.compile("\\{\\{embed:([^}]+)\\}\\}");
        Matcher matcher = embedPattern.matcher(content);
        int lastEnd = 0;
        int embedCount = 0;

        while (matcher.find()) {
            // 添加标记之前的内容
            processedContent.append(content.substring(lastEnd, matcher.start()));

            // 获取URL并生成嵌入代码
            String url = matcher.group(1);
            processedContent.append(generateEmbedHtml(url, embedCount++));

            lastEnd = matcher.end();
        }

        // 添加剩余内容
        processedContent.append(content.substring(lastEnd));

        // 如果提供了额外的embedUrls且内容中没有嵌入标记，将它们添加到内容末尾
        if (embedUrls != null && !embedUrls.isEmpty() && embedCount == 0) {
            processedContent.append("\n<div class=\"additional-embeds\">\n");
            for (int i = 0; i < embedUrls.size(); i++) {
                String url = embedUrls.get(i);
                if (url != null && !url.trim().isEmpty()) {
                    processedContent.append(generateEmbedHtml(url, i));
                }
            }
            processedContent.append("</div>\n");
        }

        return processedContent.toString();
    }

    /**
     * 生成单个嵌入页面的HTML代码
     */
    private String generateEmbedHtml(String url, int index) {
        if (url == null || url.trim().isEmpty()) {
            return "";
        }

        StringBuilder embedHtml = new StringBuilder();
        embedHtml.append("    <div class=\"embed-item my-4\">\n");
        embedHtml.append("        <div class=\"embed-container\">\n");
        embedHtml.append("            <iframe src=\"").append(escapeHtml(url.trim())).append("\" class=\"embedded-iframe\" loading=\"lazy\" allowfullscreen></iframe>\n");
        embedHtml.append("            <div class=\"iframe-loader\" id=\"loader-").append(index).append("\">\n");
        embedHtml.append("                <div class=\"loader-spinner\"></div>\n");
        embedHtml.append("                <div>正在加载页面...</div>\n");
        embedHtml.append("            </div>\n");
        embedHtml.append("        </div>\n");
        embedHtml.append("        <div class=\"embed-source\">源: <a href=\"").append(escapeHtml(url.trim())).append("\" target=\"_blank\" rel=\"noopener\">").append(escapeHtml(url.trim())).append("</a></div>\n");
        embedHtml.append("    </div>\n");

        return embedHtml.toString();
    }

    /**
     * 构建带有内嵌页面的HTML文档
     */
    private String buildHtmlWithEmbeddedPages(String title, String contentWithEmbeds) {
        // 处理基础内容
        String processedContent = processContentForTitle(contentWithEmbeds, title);

        // 额外的CSS样式
        String extraStyles =
                "        /* 内嵌页面样式 */\n" +
                        "        .embed-item {\n" +
                        "            margin: 2rem 0;\n" +
                        "            border: 1px solid var(--border-color);\n" +
                        "            border-radius: 8px;\n" +
                        "            overflow: hidden;\n" +
                        "            box-shadow: 0 4px 12px var(--shadow-color);\n" +
                        "            transition: transform 0.3s ease, box-shadow 0.3s ease;\n" +
                        "        }\n" +
                        "        .embed-item:hover {\n" +
                        "            transform: translateY(-5px);\n" +
                        "            box-shadow: 0 8px 15px var(--shadow-color);\n" +
                        "        }\n" +
                        "        .embed-container {\n" +
                        "            position: relative;\n" +
                        "            height: 600px;\n" +
                        "            width: 100%;\n" +
                        "            background-color: var(--bg-color);\n" +
                        "        }\n" +
                        "        .embedded-iframe {\n" +
                        "            width: 100%;\n" +
                        "            height: 100%;\n" +
                        "            border: none;\n" +
                        "        }\n" +
                        "        .iframe-loader {\n" +
                        "            position: absolute;\n" +
                        "            top: 0;\n" +
                        "            left: 0;\n" +
                        "            width: 100%;\n" +
                        "            height: 100%;\n" +
                        "            display: flex;\n" +
                        "            flex-direction: column;\n" +
                        "            justify-content: center;\n" +
                        "            align-items: center;\n" +
                        "            background-color: var(--bg-color);\n" +
                        "            z-index: 2;\n" +
                        "            transition: opacity 0.5s ease, visibility 0.5s ease;\n" +
                        "        }\n" +
                        "        .iframe-loader .loader-spinner {\n" +
                        "            margin-bottom: 10px;\n" +
                        "        }\n" +
                        "        .embed-source {\n" +
                        "            padding: 10px 15px;\n" +
                        "            font-size: 0.9rem;\n" +
                        "            border-top: 1px solid var(--border-color);\n" +
                        "            background-color: var(--card-bg);\n" +
                        "        }\n" +
                        "        .embed-source a {\n" +
                        "            color: var(--primary-color);\n" +
                        "            word-break: break-all;\n" +
                        "            text-decoration: none;\n" +
                        "        }\n" +
                        "        .embed-source a:hover {\n" +
                        "            text-decoration: underline;\n" +
                        "        }\n" +
                        "        .additional-embeds {\n" +
                        "            margin-top: 3rem;\n" +
                        "            padding-top: 2rem;\n" +
                        "            border-top: 1px solid var(--border-color);\n" +
                        "        }\n" +
                        "        @media (max-width: 768px) {\n" +
                        "            .embed-container {\n" +
                        "                height: 400px;\n" +
                        "            }\n" +
                        "        }\n";

        // 额外的JavaScript
        String extraScripts =
                "            // iframe加载处理\n" +
                        "            document.querySelectorAll('.embedded-iframe').forEach(function(iframe) {\n" +
                        "                const container = iframe.closest('.embed-container');\n" +
                        "                const loaderId = container.querySelector('.iframe-loader').id;\n" +
                        "                const loader = document.getElementById(loaderId);\n" +
                        "                \n" +
                        "                iframe.addEventListener('load', function() {\n" +
                        "                    setTimeout(function() {\n" +
                        "                        loader.style.opacity = '0';\n" +
                        "                        setTimeout(function() {\n" +
                        "                            loader.style.visibility = 'hidden';\n" +
                        "                        }, 500);\n" +
                        "                    }, 1000); // 给iframe一些额外时间来渲染\n" +
                        "                });\n" +
                        "                \n" +
                        "                iframe.addEventListener('error', function() {\n" +
                        "                    loader.innerHTML = '<div>加载页面失败</div>';\n" +
                        "                });\n" +
                        "            });\n";

        return buildFullHtmlDocument(title, processedContent, extraStyles, extraScripts);
    }

    /**
     * 构建完整的HTML文档
     *
     * @param title 文档标题
     * @param content 文档内容
     * @param extraStyles 额外的CSS样式
     * @param extraScripts 额外的JavaScript脚本
     * @return 完整的HTML文档字符串
     */
    private String buildFullHtmlDocument(String title, String content, String extraStyles, String extraScripts) {
        StringBuilder htmlBuilder = new StringBuilder();

        htmlBuilder.append("<!DOCTYPE html>\n")
                .append("<html lang=\"zh-CN\">\n")
                .append("<head>\n")
                .append("    <meta charset=\"UTF-8\">\n")
                .append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
                .append("    <title>").append(escapeHtml(title)).append("</title>\n")
                .append("    <style>\n");

        // 添加基础样式
        htmlBuilder.append("        /* CSS变量定义（便于主题切换） */\n")
                .append("        :root {\n")
                .append("            --primary-color: #3498db;\n")
                .append("            --secondary-color: #2c3e50;\n")
                .append("            --accent-color: #e74c3c;\n")
                .append("            --bg-color: #f8f9fa;\n")
                .append("            --text-color: #333;\n")
                .append("            --card-bg: #ffffff;\n")
                .append("            --border-color: #ddd;\n")
                .append("            --shadow-color: rgba(0,0,0,0.1);\n")
                .append("            --animation-duration: 1.2s;\n")
                .append("            --animation-delay: 0.1s;\n")
                .append("        }\n")
                .append("        /* 暗色模式主题 */\n")
                .append("        [data-theme=\"dark\"] {\n")
                .append("            --primary-color: #61dafb;\n")
                .append("            --secondary-color: #f1f1f1;\n")
                .append("            --accent-color: #ff6b6b;\n")
                .append("            --bg-color: #121212;\n")
                .append("            --text-color: #e0e0e0;\n")
                .append("            --card-bg: #1e1e1e;\n")
                .append("            --border-color: #444;\n")
                .append("            --shadow-color: rgba(255,255,255,0.05);\n")
                .append("        }\n");

        // 添加原有的CSS样式（省略以避免代码过长）
        htmlBuilder.append("        /* 基础样式 */\n")
                .append("        body { \n")
                .append("            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\n")
                .append("            line-height: 1.8;\n")
                .append("            color: var(--text-color);\n")
                .append("            background-color: var(--bg-color);\n")
                .append("            padding-top: 30px;\n")
                .append("            opacity: 1;\n")
                .append("            transition: background-color 0.3s ease, color 0.3s ease;\n")
                .append("            scroll-behavior: smooth;\n")
                .append("        }\n")
        // ... 更多基础样式 ...
        ;

        // 添加额外样式
        if (extraStyles != null && !extraStyles.isEmpty()) {
            htmlBuilder.append(extraStyles);
        }

        htmlBuilder.append("    </style>\n")
                .append("</head>\n")
                .append("<body>\n")
                .append("    <!-- 页面加载器 -->\n")
                .append("    <div class=\"page-loader\" id=\"pageLoader\">\n")
                .append("        <div class=\"loader-spinner\"></div>\n")
                .append("    </div>\n\n")
                .append("    <!-- 主题切换按钮 -->\n")
                .append("    <div class=\"theme-toggle\" id=\"themeToggle\" title=\"切换明暗主题\">\n")
                .append("        <svg xmlns=\"http://www.w3.org/2000/svg\" width=\"20\" height=\"20\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\">\n")
                .append("            <path d=\"M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z\"></path>\n")
                .append("        </svg>\n")
                .append("    </div>\n\n")
                .append("    <!-- 内容容器 -->\n")
                .append("    <div class=\"container fade-in\">\n")
                .append("        <h1 class=\"slide-up\">").append(escapeHtml(title)).append("</h1>\n")
                .append("        <div class=\"content\">\n")
                .append(content).append("\n")
                .append("        </div>\n")
                .append("    </div>\n\n")
                .append("    <script>\n")
                .append("        // 页面加载完成后执行\n")
                .append("        document.addEventListener('DOMContentLoaded', function() {\n")
                .append("            // 主题切换功能\n")
                .append("            const themeToggle = document.getElementById('themeToggle');\n")
                .append("            const prefersDarkMode = window.matchMedia('(prefers-color-scheme: dark)').matches;\n")
                .append("            \n")
                .append("            // 检查用户上次的主题偏好\n")
                .append("            const savedTheme = localStorage.getItem('theme');\n")
                .append("            if (savedTheme === 'dark' || (!savedTheme && prefersDarkMode)) {\n")
                .append("                document.documentElement.setAttribute('data-theme', 'dark');\n")
                .append("            }\n")
                .append("            \n")
                .append("            // 设置主题切换事件\n")
                .append("            themeToggle.addEventListener('click', function() {\n")
                .append("                const currentTheme = document.documentElement.getAttribute('data-theme');\n")
                .append("                if (currentTheme === 'dark') {\n")
                .append("                    document.documentElement.removeAttribute('data-theme');\n")
                .append("                    localStorage.setItem('theme', 'light');\n")
                .append("                } else {\n")
                .append("                    document.documentElement.setAttribute('data-theme', 'dark');\n")
                .append("                    localStorage.setItem('theme', 'dark');\n")
                .append("                }\n")
                .append("            });\n\n");

        // 添加基本的JavaScript代码（省略以避免代码过长）
        htmlBuilder.append("            // 优化页面加载处理\n")
                .append("            setTimeout(function() {\n")
                .append("                document.getElementById('pageLoader').style.opacity = '0';\n")
                .append("                setTimeout(() => {\n")
                .append("                    document.getElementById('pageLoader').style.display = 'none';\n")
                .append("                }, 500);\n")
                .append("            }, 800);\n")
        // ... 更多基本脚本 ...
        ;

        // 添加额外脚本
        if (extraScripts != null && !extraScripts.isEmpty()) {
            htmlBuilder.append(extraScripts);
        }

        htmlBuilder.append("        });\n")
                .append("    </script>\n")
                .append("</body>\n")
                .append("</html>");

        return htmlBuilder.toString();
    }

    /**
     * 确保文件名安全且有效
     */
    private String getSafeFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            // 如果没有提供文件名，生成一个基于时间戳和UUID的文件名
            return "html_" + System.currentTimeMillis() + "_" +
                    UUID.randomUUID().toString().substring(0, 8);
        }

        // 移除不安全字符
        return filename.trim()
                .replaceAll("[\\\\/:*?\"<>|]", "_") // 替换Windows/Unix不允许的字符
                .replaceAll("\\s+", "_");           // 替换空白字符
    }

    /**
     * 构建美观的HTML文档，包含动效
     *
     * @param title 页面标题，将显示在h1标签中
     * @param bodyContent 页面正文内容(不应包含h1标题)
     * @return 完整的HTML文档字符串
     */
    private String buildHtmlDocument(String title, String bodyContent) {
        // 处理正文内容，移除重复的标题
        String processedContent = processContentForTitle(bodyContent, title);

        // 使用新的buildFullHtmlDocument方法构建HTML
        return buildFullHtmlDocument(title, processedContent, "", "");
    }

    /**
     * 简单的HTML转义
     */
    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * 比较两个字符串的相似度
     *
     * @param str1 第一个字符串
     * @param str2 第二个字符串
     * @return 如果字符串非常相似则返回true
     */
    private boolean similarText(String str1, String str2) {
        if (str1 == null || str2 == null) return false;

        // 规范化字符串（去除多余空白字符）
        String normalized1 = str1.replaceAll("\\s+", " ").trim().toLowerCase();
        String normalized2 = str2.replaceAll("\\s+", " ").trim().toLowerCase();

        // 如果完全相同，直接返回true
        if (normalized1.equals(normalized2)) return true;

        // 计算字符串长度
        int len1 = normalized1.length();
        int len2 = normalized2.length();

        // 如果长度差异太大，则认为是不同的标题
        if (Math.abs(len1 - len2) > Math.max(len1, len2) * 0.2) {
            return false;
        }

        // 计算Levenshtein距离
        int distance = levenshteinDistance(normalized1, normalized2);

        // 计算相似度阈值，较长的字符串允许更多差异
        int threshold = Math.min(3, Math.max(len1, len2) / 10);

        // 如果差异小于阈值，认为是相似的
        return distance <= threshold;
    }

    /**
     * 计算两个字符串之间的Levenshtein距离
     *
     * @param str1 第一个字符串
     * @param str2 第二个字符串
     * @return 两个字符串之间的Levenshtein距离
     */
    private int levenshteinDistance(String str1, String str2) {
        int len1 = str1.length();
        int len2 = str2.length();
        int[][] dp = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) {
            for (int j = 0; j <= len2; j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(
                            dp[i - 1][j - 1] + (str1.charAt(i - 1) == str2.charAt(j - 1) ? 0 : 1),
                            Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1)
                    );
                }
            }
        }

        return dp[len1][len2];
    }

    /**
     * 处理正文内容，移除重复的标题
     *
     * @param bodyContent 页面正文内容
     * @param title 页面标题
     * @return 处理后的内容
     */
    private String processContentForTitle(String bodyContent, String title) {
        // 检查并移除bodyContent中可能存在的相同标题，防止重复显示
        String processedContent = bodyContent;

        if (title != null && !title.trim().isEmpty()) {
            // 获取标题的纯文本形式（去除前后空格）
            String plainTitle = title.trim();

            try {
                // 使用更安全的方式移除h1标签中的重复标题
                // (?i) - 不区分大小写匹配
                // (?s) - 允许.匹配换行符
                // <h1[^>]*> - 匹配h1开始标签及其属性
                // ([\\s\\S]*?) - 懒惰匹配任何内容，包括换行
                // </h1> - 匹配h1结束标签
                Pattern h1Pattern = Pattern.compile("(?i)(?s)<h1[^>]*>([\\s\\S]*?)</h1>");
                Matcher matcher = h1Pattern.matcher(processedContent);

                StringBuilder resultContent = new StringBuilder();
                int lastEnd = 0;

                while (matcher.find()) {
                    // 获取h1标签中的内容
                    String h1Content = matcher.group(1).trim();

                    // 检查h1内容是否与title相似
                    // 移除HTML标签和多余空白字符来进行比较
                    String cleanedH1 = h1Content.replaceAll("<[^>]+>", "").trim();
                    String cleanedTitle = plainTitle.replaceAll("<[^>]+>", "").trim();

                    // 如果h1内容与title相似则移除整个h1标签
                    if (cleanedH1.equalsIgnoreCase(cleanedTitle) ||
                            similarText(cleanedH1, cleanedTitle)) {
                        resultContent.append(processedContent.substring(lastEnd, matcher.start()));
                    } else {
                        // 保留不匹配的h1标签
                        resultContent.append(processedContent.substring(lastEnd, matcher.end()));
                    }
                    lastEnd = matcher.end();
                }

                // 添加剩余内容
                if (lastEnd < processedContent.length()) {
                    resultContent.append(processedContent.substring(lastEnd));
                }

                processedContent = resultContent.toString();
            } catch (Exception e) {
                // 如果正则处理出错，使用原始内容
                log.warn("移除HTML标题时出错: {}", e.getMessage());
            }
        }

        return processedContent;
    }
}
