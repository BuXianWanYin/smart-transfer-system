/**
 * GXWGY_FormatThesis_WPS_v1.js - 广西外国语学院本科毕设论文格式化宏
 * 适配：广西外国语学院毕业论文（设计）格式模板（本科毕设论文）
 * 兼容：WPS 最新版（仅用 WPS 支持的 API：Application.ActiveDocument、Paragraphs、Range、ParagraphFormat、Font 等，行距/对齐用数值常量）
 * 字号对应：小初=36pt, 三号=16pt, 四号=14pt, 小二号=18pt, 小三=15pt, 小四=12pt, 五号=10.5pt；行距统一固定值25磅
 *
 * 模板对照说明（均已按广西外国语学院本科毕设模板设置）：
 * - 封面：本科毕设论文（设计）黑体小初、中文题目黑体三号、外文题目TNR四号、信息栏/日期宋体四号 → 脚本不处理，需手动设置。
 * - 诚信声明/授权书：标题黑体四号、正文宋体小四，固定值25磅 ✓
 * - 目录：目  录 黑体小二号居中、空2行；只排到三级；一级黑体小四、二级黑体五号、三级宋体五号；固定值25磅；当前目录域整体小四黑体，二级/三级五号/宋体可手动微调 ✓
 * - 摘要：摘  要 黑体小四；摘要页中文题目（若在“摘  要”下一行）黑体小二号居中；摘要内容宋体小四；关键词黑体小四+内容宋体小四；段前1行段后0.5行，固定值25磅 ✓
 * - 英文摘要：题目TNR小三加粗居中；Abstract/Key words 小四加粗；正文小四首行缩进；固定值25磅 ✓
 * - 外文题目：TNR四号居中 ✓
 * - 正文：一级黑体四号、二级黑体小四、三/四级宋体小四；正文宋体小四；段前段后0，固定值25磅 ✓
 * - 参考文献：标题黑体四号居中；条目仿宋五号左顶格、悬挂缩进，固定值25磅 ✓
 * - 致谢：标题黑体四号居中字间空2格；内容宋体小四首行缩进2格，固定值25磅 ✓
 * - 题注/图注：仿宋五号 ✓
 * - 页眉：广西外国语学院毕业论文（设计），宋体五号 ✓
 */

function FormatAll() {
    var doc = Application.ActiveDocument;
    if (!doc) { alert("请先打开文档！"); return; }

    var cfg = _createConfig();
    var L = _createLogger("ERROR");
    var M = _createModules();

    Application.ScreenUpdating = false;
    try {
        _runFormatting(doc, cfg, L, M);
        alert("格式化完成！");
    } catch (e) {
        alert("格式化出错: " + e.message);
    }
    Application.ScreenUpdating = true;
}

function FormatAllDebug() {
    var doc = Application.ActiveDocument;
    if (!doc) { alert("请先打开文档！"); return; }

    var cfg = _createConfig();
    var L = _createLogger("DEBUG");
    var M = _createModules();
    var D = _createDebugConfig();  // 调试配置

    Application.ScreenUpdating = false;
    try {
        var pos = _runFormatting(doc, cfg, L, M, D);
        var report = _generateReport(L, doc, pos);
        _showReport(report);
    } catch (e) {
        L.error("Main", "格式化失败", { error: e.message });
        alert("格式化出错: " + e.message);
    }
    Application.ScreenUpdating = true;
}

function TestDetector() {
    var doc = Application.ActiveDocument;
    if (!doc) { alert("请先打开文档！"); return; }

    var L = _createLogger("DEBUG");
    var M = _createModules();
    var pos = M.Detector.findAll(doc, L);

    var msg = "=== 检测结果 ===\n\n";
    msg += "文档: " + doc.Name + "\n";
    msg += "段落数: " + doc.Paragraphs.Count + "\n";
    msg += "节数: " + doc.Sections.Count + "\n\n";
    msg += "摘要: P" + pos.abstract + "\n";
    msg += "目录: P" + pos.toc + "\n";
    msg += "第一章: P" + pos.chapter1 + "\n";
    msg += "参考文献: P" + pos.reference + "\n";
    msg += "致谢: P" + pos.acknowledgement + "\n";
    msg += "章节数: " + pos.chapters.length + "\n";
    alert(msg);
}

function ShowHelp() {
    alert("GXWGY_FormatThesis_WPS_v1.js\n广西外国语学院本科毕设论文格式\n\n入口函数:\n  FormatAll() - 一键格式化\n  FormatAllDebug() - 调试模式\n  TestDetector() - 测试检测器");
}

function _createDebugConfig() {
    return {
        enabled: true,              // 总开关
        // 位置检测
        position: {
            sectionBreaks: true,    // 分节符位置（前后对比）
            pageBreaks: false,      // 分页符位置（前后对比）
            showDetail: false,      // 显示分页符/分节符前后段落内容
            detailLimit: 50         // 详细信息最多显示几个
        },
        // 格式对比（按区域）- 开启后输出该类型段落的格式变化
        // 默认全部关闭，需要时手动开启
        format: {
            abstractTitle: false,   // 摘要标题
            abstractBody: false,    // 摘要正文
            keywords: false,        // 关键词
            englishTitle: false,    // 英文题目
            abstractEn: false,      // ABSTRACT
            keywordsEn: false,      // 英文关键词
            abstractEnBody: false,  // 英文摘要正文
            tocTitle: false,        // 目录标题
            heading1: false,        // 一级标题
            heading2: false,        // 二级标题
            heading3: false,        // 三级标题
            body: false,            // 正文
            caption: false,         // 题注
            reference: false,       // 参考文献
            acknowledgement: false  // 致谢
        },
        // 输出控制
        sampleCount: 3              // 每类输出几个样本
    };
}

function _createConfig() {
    // 广西外国语学院本科毕设：固定值25磅=lineSpacingRule:5, lineSpacing:25；字号 小初36/小二号18/三号16/小三15/四号14/小四12/五号10.5
    return {
        page: { top: 85, bottom: 71, left: 74, right: 74, headerDistance: 57, footerDistance: 50 },
        header: { content: "广西外国语学院毕业论文（设计）", font: "宋体", size: 10.5, align: 1 },
        // 摘要：“摘  要”行黑体小四（与关键词一致）；摘要页中文题目黑体小二号居中；摘要内容宋体小四，段前1行(12磅)段后0.5行(6磅)，固定值25磅
        abstractTitle: { font: "黑体", fontEn: "Times New Roman", size: 12, align: 1, lineSpacingRule: 5, lineSpacing: 25, spaceBefore: 12, spaceAfter: 12, bold: false, firstLineIndent: 0 },
        abstractPageChineseTitle: { font: "黑体", fontEn: "Times New Roman", size: 18, align: 1, lineSpacingRule: 5, lineSpacing: 25, spaceBefore: 0, spaceAfter: 6, bold: false, firstLineIndent: 0 },
        abstractBody: { font: "宋体", fontEn: "Times New Roman", size: 12, align: 3, lineSpacingRule: 5, lineSpacing: 25, spaceBefore: 12, spaceAfter: 6, bold: false, firstLineIndent: 2, widowControl: true, keepTogether: false, keepWithNext: false },
        keywords: { font: "宋体", fontEn: "Times New Roman", size: 12, align: 3, lineSpacingRule: 5, lineSpacing: 25, spaceBefore: 0, spaceAfter: 6, bold: false, firstLineIndent: 2, widowControl: true },
        englishTitle: { font: "Times New Roman", fontEn: "Times New Roman", size: 14, align: 1, lineSpacingRule: 5, lineSpacing: 25, spaceBefore: 0, spaceAfter: 0, bold: false, firstLineIndent: 0, widowControl: false, keepTogether: false, keepWithNext: false },
        abstractEnTitle: { font: "Times New Roman", fontEn: "Times New Roman", size: 15, align: 1, lineSpacingRule: 5, lineSpacing: 25, spaceBefore: 12, spaceAfter: 6, bold: true, firstLineIndent: 0 },
        abstractEnBody: { font: "Times New Roman", fontEn: "Times New Roman", size: 12, align: 3, lineSpacingRule: 5, lineSpacing: 25, spaceBefore: 0, spaceAfter: 0, bold: false, firstLineIndent: 2, widowControl: false, keepTogether: false, keepWithNext: false },
        keywordsEn: { font: "Times New Roman", fontEn: "Times New Roman", size: 12, align: 0, lineSpacingRule: 5, lineSpacing: 25, spaceBefore: 0, spaceAfter: 6, bold: false, firstLineIndent: 2, widowControl: false, keepTogether: false, keepWithNext: false },
        integrityTitle: { font: "黑体", fontEn: "Times New Roman", size: 14, align: 1, lineSpacingRule: 5, lineSpacing: 25, spaceBefore: 12, spaceAfter: 12, bold: false, firstLineIndent: 0, widowControl: false, keepTogether: true, keepWithNext: true },
        integrityBody: { font: "宋体", fontEn: "Times New Roman", size: 12, align: 3, lineSpacingRule: 5, lineSpacing: 25, spaceBefore: 0, spaceAfter: 0, bold: false, firstLineIndent: 2, widowControl: false, keepTogether: false, keepWithNext: false },
        tocTitle: { font: "黑体", fontEn: "Times New Roman", size: 18, align: 1, lineSpacingRule: 5, lineSpacing: 25, spaceBefore: 24, spaceAfter: 24, bold: false, firstLineIndent: 0, widowControl: false, keepTogether: true, keepWithNext: true, outlineLevel: 10 },
        heading1: { font: "黑体", fontEn: "Times New Roman", size: 14, align: 1, lineSpacingRule: 5, lineSpacing: 25, spaceBefore: 0, spaceAfter: 0, bold: false, firstLineIndent: 0, widowControl: false, keepTogether: true, keepWithNext: true },
        heading2: { font: "黑体", fontEn: "Times New Roman", size: 12, align: 0, lineSpacingRule: 5, lineSpacing: 25, spaceBefore: 0, spaceAfter: 0, bold: false, firstLineIndent: 0, widowControl: false, keepTogether: true, keepWithNext: true },
        heading3: { font: "宋体", fontEn: "Times New Roman", size: 12, align: 0, lineSpacingRule: 5, lineSpacing: 25, spaceBefore: 0, spaceAfter: 0, bold: false, firstLineIndent: 0, widowControl: false, keepTogether: true, keepWithNext: true },
        heading4: { font: "宋体", fontEn: "Times New Roman", size: 12, align: 0, lineSpacingRule: 5, lineSpacing: 25, spaceBefore: 0, spaceAfter: 0, bold: false, firstLineIndent: 0, widowControl: false, keepTogether: true, keepWithNext: true },
        body: { font: "宋体", fontEn: "Times New Roman", size: 12, align: 3, lineSpacingRule: 5, lineSpacing: 25, spaceBefore: 0, spaceAfter: 0, bold: false, firstLineIndent: 2, widowControl: false, keepTogether: false, keepWithNext: false },
        referenceTitle: { font: "黑体", fontEn: "Times New Roman", size: 14, align: 1, lineSpacingRule: 5, lineSpacing: 25, spaceBefore: 0, spaceAfter: 0, bold: false, firstLineIndent: 0, widowControl: false, keepTogether: true, keepWithNext: true },
        referenceBody: { font: "仿宋", fontEn: "Times New Roman", size: 10.5, align: 0, lineSpacingRule: 5, lineSpacing: 25, spaceBefore: 0, spaceAfter: 0, bold: false, firstLineIndent: 0, hangingIndent: 2, widowControl: false, keepTogether: false, keepWithNext: false },
        ackTitle: { font: "黑体", fontEn: "Times New Roman", size: 14, align: 1, lineSpacingRule: 5, lineSpacing: 25, spaceBefore: 0, spaceAfter: 0, bold: false, firstLineIndent: 0, widowControl: false, keepTogether: true, keepWithNext: true },
        ackBody: { font: "宋体", fontEn: "Times New Roman", size: 12, align: 3, lineSpacingRule: 5, lineSpacing: 25, spaceBefore: 0, spaceAfter: 0, bold: false, firstLineIndent: 2, widowControl: false, keepTogether: false, keepWithNext: false },
        caption: { font: "仿宋", fontEn: "Times New Roman", size: 10.5, align: 1, lineSpacingRule: 5, lineSpacing: 25, spaceBefore: 0, spaceAfter: 0, bold: false, firstLineIndent: 0, widowControl: false, keepTogether: false, keepWithNext: false },
        continuedCaption: { font: "仿宋", fontEn: "Times New Roman", size: 10.5, align: 0, lineSpacingRule: 5, lineSpacing: 25, spaceBefore: 0, spaceAfter: 0, bold: false, firstLineIndent: 0, widowControl: false, keepTogether: false, keepWithNext: false },
        codeCaption: { font: "宋体", fontEn: "Times New Roman", size: 12, align: 3, lineSpacingRule: 5, lineSpacing: 25, spaceBefore: 0, spaceAfter: 0, bold: false, firstLineIndent: 2, widowControl: false, keepTogether: false, keepWithNext: false },
        citation: { enabled: true, superscript: true, useCrossReference: true, createBookmarks: true, createHyperlinks: true },  // useCrossReference=true启用交叉引用超链接
        // 功能开关
        features: {
            resizeImages: false,      // 调整图片尺寸
            formatTables: true,       // 设置三线表
            handleContinuedTables: false,  // 处理跨页表格续表（默认关闭，可单独调用HandleContinuedTables）
            formatTOC: true,          // 格式化目录
            processCitation: true,    // 处理文献引用
            processCodeBlocks: true,  // 处理代码块
            replaceQuotes: true       // 替换英文双引号为中文双引号
        },
        // 图片尺寸配置（百分比为页面可用宽度的比例）
        image: {
            // 图片调整范围：仅调整第一章之后的图片（跳过封面/诚信声明书/摘要等前置部分）
            // 按宽高比分类：16:9(1.78)=100%, 4:3(1.33)/3:2(1.5)=80%
            widescreen: { defaultWidth: 1.0, minWidth: 0.8, maxWidth: 1.0 },   // 16:9 宽高比>=1.7
            standard: { defaultWidth: 0.8, minWidth: 0.6, maxWidth: 0.9 },     // 4:3, 3:2 宽高比<1.7
            // 竖屏图片（高>宽）：默认50%，范围40%~60%
            portrait: { defaultWidth: 0.5, minWidth: 0.4, maxWidth: 0.6 }
        },
        // 目录配置（广西外国语学院：只排到三级标题；一级黑体小四、二级黑体五号、三级宋体五号；固定值25磅）
        toc: {
            upperLevel: 1,
            lowerLevel: 3,
            font: "黑体",
            size: 12,        // 一级小四；二级三级由目录域生成后整体为五号时可再调
            align: 3,
            lineSpacing: 25  // 固定值25磅
        },
        // 标题级别配置：只对列表中的级别设置大纲级别，其他级别只设置格式不设置大纲
        headingLevels: [1, 2, 3]  // 默认只处理1-3级标题
    };
}

function _createLogger(level) {
    return {
        level: level || "INFO",
        levels: { DEBUG: 0, INFO: 1, WARN: 2, ERROR: 3 },
        entries: [],
        stats: { startTime: new Date(), errors: 0, warnings: 0 },
        shouldLog: function (lvl) { return this.levels[lvl] >= this.levels[this.level]; },
        log: function (lvl, mod, msg, data) {
            if (!this.shouldLog(lvl)) return;
            this.entries.push({ t: new Date() - this.stats.startTime, l: lvl, m: mod, msg: msg, d: data });
            if (lvl === "ERROR") this.stats.errors++;
            if (lvl === "WARN") this.stats.warnings++;
        },
        debug: function (mod, msg, data) { this.log("DEBUG", mod, msg, data); },
        info: function (mod, msg, data) { this.log("INFO", mod, msg, data); },
        warn: function (mod, msg, data) { this.log("WARN", mod, msg, data); },
        error: function (mod, msg, data) { this.log("ERROR", mod, msg, data); },
        step: function (num, title) { this.log("INFO", "Main", "步骤" + num + ": " + title); }
    };
}

function _createModules() {
    var Utils = {
        cleanText: function (text) {
            if (!text) return "";
            // 清理换行、制表符、分页符、分节符等特殊字符
            var result = "";
            for (var i = 0; i < text.length; i++) {
                var ch = text.charCodeAt(i);
                // 跳过：回车(13)、换行(10)、制表符(9)、分页符(12)、分节符(14,28,29,30)
                if (ch === 9 || ch === 10 || ch === 12 || ch === 13 || ch === 14 || ch === 28 || ch === 29 || ch === 30) continue;
                result += text.charAt(i);
            }
            return result.replace(/^\s+|\s+$/g, "");
        },
        isTocLine: function (text, para) {
            // 目录行特征1：含Tab + 末尾是页码（阿拉伯数字或罗马数字）
            if (text && text.indexOf("\t") >= 0) {
                if (/(\d+|[IVXLCDM]+)[\r\n]*$/i.test(text)) return true;
            }
            // 目录行特征2：段落样式为目录样式
            if (para) {
                try {
                    var styleName = para.Range.ParagraphStyle.NameLocal || "";
                    if (/目录|TOC|toc/i.test(styleName)) return true;
                } catch (e) { }
            }
            // 目录行特征3：有Tab但没有页码，且在目录区域内（简化判断：有Tab且长度较短）
            if (text && text.indexOf("\t") >= 0 && text.length < 50) {
                // 目录条目通常较短，且以标题格式开头
                var cleanTxt = text.replace(/[\r\n\t]/g, "").trim();
                if (/^第[一二三四五六七八九十\d]+章/.test(cleanTxt) || 
                    /^\d+(\.\d+)*\s/.test(cleanTxt) ||
                    /^[一二三四五六七八九十]+[、\s]/.test(cleanTxt)) {
                    return true;
                }
            }
            return false;
        },
        hasImage: function (para) {
            try { return para.Range.InlineShapes && para.Range.InlineShapes.Count > 0; }
            catch (e) { return false; }
        },
        isEmpty: function (para) {
            try {
                if (this.hasImage(para)) return false;
                // 如果有自动编号，不算空段落
                try { if (para.Range.ListFormat.ListString) return false; } catch(e) {}
                return para.Range.Text.replace(/[\r\n\f\t\s\u00A0]/g, "").length === 0;
            } catch (e) { return false; }
        },
        hasPageBreak: function (para) {
            try {
                var txt = para.Range.Text;
                for (var i = 0; i < txt.length; i++) if (txt.charCodeAt(i) === 12) return true;
                return false;
            } catch (e) { return false; }
        },
        endsWithPunctuation: function (txt) {
            if (!txt || txt.length === 0) return false;
            var last = txt.charAt(txt.length - 1);
            return "。；，：.;,、！？!?".indexOf(last) >= 0;
        },
        // 获取段落完整文本（包含自动编号）
        getFullText: function (para) {
            var txt = this.cleanText(para.Range.Text);
            var listStr = "";
            try { listStr = para.Range.ListFormat.ListString || ""; } catch (e) { }
            if (listStr) {
                return this.cleanText(listStr) + txt;
            }
            return txt;
        },
        // 检测段落是否在表格内（只用Tables.Count，Information可能不可靠）
        isInTable: function (para) {
            try {
                return para.Range.Tables && para.Range.Tables.Count > 0;
            } catch (e) { return false; }
        },
        // 获取分节符位置
        getSectionBreaks: function (doc) {
            var breaks = [];
            for (var i = 1; i <= doc.Sections.Count; i++) {
                try {
                    var sec = doc.Sections.Item(i);
                    breaks.push({ section: i, start: sec.Range.Start });
                } catch (e) { }
            }
            return breaks;
        },
        // 获取分页符位置
        getPageBreaks: function (doc) {
            var breaks = [];
            var count = doc.Paragraphs.Count;
            for (var i = 1; i <= count; i++) {
                try {
                    if (this.hasPageBreak(doc.Paragraphs.Item(i))) {
                        breaks.push(i);
                    }
                } catch (e) { }
            }
            return breaks;
        },
        // 获取分页符详细信息（含前后段落内容）
        getPageBreaksDetail: function (doc, limit) {
            var breaks = [];
            var count = doc.Paragraphs.Count;
            var maxBreaks = limit || 20;
            for (var i = 1; i <= count && breaks.length < maxBreaks; i++) {
                try {
                    if (this.hasPageBreak(doc.Paragraphs.Item(i))) {
                        var prevTxt = i > 1 ? this.cleanText(doc.Paragraphs.Item(i - 1).Range.Text).substring(0, 30) : "";
                        var currTxt = this.cleanText(doc.Paragraphs.Item(i).Range.Text).substring(0, 30);
                        breaks.push({ para: i, prev: prevTxt, curr: currTxt });
                    }
                } catch (e) { }
            }
            return breaks;
        },
        // 获取分节符详细信息
        getSectionBreaksDetail: function (doc) {
            var breaks = [];
            for (var i = 1; i <= doc.Sections.Count; i++) {
                try {
                    var sec = doc.Sections.Item(i);
                    var firstPara = sec.Range.Paragraphs.Item(1);
                    var txt = this.cleanText(firstPara.Range.Text).substring(0, 30);
                    breaks.push({ section: i, firstPara: txt });
                } catch (e) { breaks.push({ section: i, firstPara: "error" }); }
            }
            return breaks;
        },
        // 获取段落格式快照
        getParaFormat: function (para) {
            try {
                var rng = para.Range;
                var pf = rng.ParagraphFormat;
                return {
                    font: rng.Font.Name,
                    fontEn: rng.Font.NameAscii,
                    size: rng.Font.Size,
                    bold: rng.Font.Bold,
                    align: pf.Alignment,
                    lineSpacing: pf.LineSpacing,
                    firstIndent: pf.CharacterUnitFirstLineIndent,
                    spaceBefore: pf.SpaceBefore,
                    spaceAfter: pf.SpaceAfter
                };
            } catch (e) { return null; }
        },
        // 设置三线表格式（封装复用）
        formatThreeLineTable: function (tbl) {
            try {
                // 清除所有边框（包括内部边框 -6 到 -1）
                for (var b = -6; b <= -1; b++) {
                    try { tbl.Borders.Item(b).LineStyle = 0; } catch (e) {}
                }
                // 顶部和底部边框（1.5磅）
                tbl.Borders.Item(-1).LineStyle = 1;
                tbl.Borders.Item(-1).LineWidth = 12;
                tbl.Borders.Item(-3).LineStyle = 1;
                tbl.Borders.Item(-3).LineWidth = 12;
                // 表头下边框（0.75磅）
                if (tbl.Rows.Count > 1) {
                    tbl.Rows.Item(1).Borders.Item(-3).LineStyle = 1;
                    tbl.Rows.Item(1).Borders.Item(-3).LineWidth = 6;
                }
                // 设置单元格边距为0
                try {
                    tbl.TopPadding = 0; tbl.BottomPadding = 0;
                    tbl.LeftPadding = 0; tbl.RightPadding = 0;
                } catch (e) {}
                // 设置每行首行缩进为0
                for (var r = 1; r <= tbl.Rows.Count; r++) {
                    try {
                        var row = tbl.Rows.Item(r);
                        // 设置行高为最小值模式，让行高自动适应内容
                        row.HeightRule = 1;  // wdRowHeightAtLeast（最小值）
                        row.Height = 0;      // 清除固定行高
                        row.HeightRule = 0;  // wdRowHeightAuto（自动）
                        for (var cc = 1; cc <= row.Cells.Count; cc++) {
                            try {
                                var cell = row.Cells.Item(cc);
                                cell.TopPadding = 0; cell.BottomPadding = 0;
                                cell.LeftPadding = 0; cell.RightPadding = 0;
                                cell.VerticalAlignment = 1;  // wdCellAlignVerticalCenter（垂直居中）
                                cell.Range.ParagraphFormat.FirstLineIndent = 0;
                                cell.Range.ParagraphFormat.CharacterUnitFirstLineIndent = 0;
                                // 强制设置每个单元格的行距和段落间距，确保覆盖已有格式
                                cell.Range.ParagraphFormat.LineSpacingRule = 5;  // wdLineSpaceExactly
                                cell.Range.ParagraphFormat.LineSpacing = 15;     // 15磅
                                cell.Range.ParagraphFormat.SpaceBefore = 0;      // 段前间距
                                cell.Range.ParagraphFormat.SpaceAfter = 0;       // 段后间距
                            } catch (e2) {}
                        }
                    } catch (e) {}
                }
                // 设置表格内容格式
                try {
                    tbl.Range.Font.NameFarEast = "宋体";
                    tbl.Range.Font.NameAscii = "Times New Roman";
                    tbl.Range.Font.Size = 12;
                    tbl.Range.ParagraphFormat.Alignment = 1;
                    tbl.Range.ParagraphFormat.LineSpacingRule = 5;
                    tbl.Range.ParagraphFormat.LineSpacing = 15;
                    tbl.Range.ParagraphFormat.FirstLineIndent = 0;
                    tbl.Range.ParagraphFormat.CharacterUnitFirstLineIndent = 0;
                } catch (e) {}
                // 自动调整表格宽度：先按内容调整，再撑满页面
                tbl.AutoFitBehavior(1);  // wdAutoFitContent
                tbl.AutoFitBehavior(2);  // wdAutoFitWindow
            } catch (e) {}
        }
    };

    var Detector = {
        findIntegrityDeclaration: function (doc) {
            for (var i = 1; i <= Math.min(doc.Paragraphs.Count, 100); i++) {
                var txt = Utils.cleanText(doc.Paragraphs.Item(i).Range.Text);
                if (txt.indexOf("诚信声明") === 0) return i;
            }
            return -1;
        },
        findAbstractTitle: function (doc) {
            for (var i = 1; i <= Math.min(doc.Paragraphs.Count, 100); i++) {
                var txt = Utils.cleanText(doc.Paragraphs.Item(i).Range.Text);
                if (txt === "摘要" || txt === "摘  要") return i;
            }
            return -1;
        },
        findKeywords: function (doc, start) {
            for (var i = start + 1; i <= Math.min(doc.Paragraphs.Count, start + 30); i++) {
                var txt = Utils.cleanText(doc.Paragraphs.Item(i).Range.Text);
                if (txt.indexOf("关键词") === 0) return i;
            }
            return -1;
        },
        findEnglishTitle: function (doc, start) {
            // 方法1：先找ABSTRACT，然后检查其上方段落是否为全英文
            var abstractIdx = this.findAbstractEn(doc, start);
            if (abstractIdx > 1) {
                // 从ABSTRACT往上找，跳过空段落
                for (var j = abstractIdx - 1; j > start; j--) {
                    var prevTxt = Utils.cleanText(doc.Paragraphs.Item(j).Range.Text);
                    if (prevTxt.length < 5) continue;  // 跳过空段落
                    // 检查是否为全英文（无中文字符，长度>15）
                    var chineseCount = (prevTxt.match(/[\u4e00-\u9fa5]/g) || []).length;
                    if (chineseCount === 0 && prevTxt.length > 15 && /^[A-Z]/.test(prevTxt)) {
                        return j;
                    }
                    break;  // 只检查紧邻的非空段落
                }
            }
            // 方法2：备用 - 遍历查找
            for (var i = start + 1; i <= Math.min(doc.Paragraphs.Count, start + 50); i++) {
                var txt = Utils.cleanText(doc.Paragraphs.Item(i).Range.Text);
                var chineseCount = (txt.match(/[\u4e00-\u9fa5]/g) || []).length;
                if (/^[A-Z]/.test(txt) && chineseCount === 0 && txt.length > 15 && 
                    txt.toUpperCase().indexOf("ABSTRACT") !== 0 && 
                    !/^key\s*words?/i.test(txt)) {
                    return i;
                }
            }
            return -1;
        },
        findAbstractEn: function (doc, start) {
            for (var i = start + 1; i <= Math.min(doc.Paragraphs.Count, start + 60); i++) {
                var txt = Utils.cleanText(doc.Paragraphs.Item(i).Range.Text).toUpperCase();
                if (txt === "ABSTRACT") return i;
            }
            return -1;
        },
        findKeywordsEn: function (doc, start) {
            if (start < 0) return -1;
            for (var i = start + 1; i <= Math.min(doc.Paragraphs.Count, start + 20); i++) {
                var txt = Utils.cleanText(doc.Paragraphs.Item(i).Range.Text);
                if (/^key\s*words?/i.test(txt)) return i;
            }
            return -1;
        },
        findTocTitle: function (doc, start) {
            for (var i = start + 1; i <= Math.min(doc.Paragraphs.Count, start + 80); i++) {
                var txt = Utils.cleanText(doc.Paragraphs.Item(i).Range.Text);
                if (txt === "目录" || txt === "目  录") return i;
            }
            return -1;
        },
        // 检测标题格式类型
        // 返回: { level: 1-4, format: 'zhangCn'|'zhangNum'|'jieCn'|'cnNum'|'cnParen'|'arabDot'|'arabParen'|'arabComma' }
        detectHeadingFormat: function (txt) {
            // 一级：第X章（中文数字，支持十一、二十一等组合）
            if (/^第[一二三四五六七八九十百零]+章/.test(txt)) return { level: 1, format: 'zhangCn' };
            // 一级：第1章（阿拉伯数字）
            if (/^第\d+章/.test(txt)) return { level: 1, format: 'zhangNum' };
            // 二级：第X节（支持十一、二十一等组合）
            if (/^第[一二三四五六七八九十百零]+节/.test(txt)) return { level: 2, format: 'jieCn' };
            // 二级：第1节（阿拉伯数字）
            if (/^第\d+节/.test(txt)) return { level: 2, format: 'jieNum' };
            // 二级/三级/四级：1.1 或 1.1.1 或 1.1.1.1
            if (/^\d+\.\d+\.\d+\.\d+/.test(txt)) return { level: 4, format: 'arabDotDotDot' };
            if (/^\d+\.\d+\.\d+(?!\.)/.test(txt)) return { level: 3, format: 'arabDotDot' };
            if (/^\d+\.\d+(?!\.)/.test(txt)) return { level: 2, format: 'arabDot' };
            // 一级/三级：一、二、三、（中文数字+顿号，后面是中文，支持十一等组合）
            if (/^[一二三四五六七八九十百零]+、[\u4e00-\u9fa5]/.test(txt)) return { level: 1, format: 'cnComma' };
            // 一级：一 绪论（中文数字+空格+中文，排除量词和特殊词）
            // 排除：一 个、三 种、百 分、零 基础、一 般、一 定、一 些、一 起、一 下、一 时 等
            if (/^[一二三四五六七八九十百零]+\s+[\u4e00-\u9fa5]/.test(txt) && 
                !/^[一二三四五六七八九十百零]+\s+[个多种项条件位名次是分基般定些样直共同起下时边点面]/.test(txt) &&
                !/^百\s*分/.test(txt) && !/^零\s*基/.test(txt)) return { level: 1, format: 'cnSpace' };
            // 二级/四级：（一）（二）（三）后面跟中文或空格+中文（支持十一等组合）
            if (/^[（\(][一二三四五六七八九十百零]+[）\)]\s*[\u4e00-\u9fa5]/.test(txt)) return { level: 2, format: 'cnParen' };
            // 一级/三级：1、2、3、（阿拉伯数字+顿号，后面是中文）
            if (/^\d+、[\u4e00-\u9fa5]/.test(txt)) return { level: 1, format: 'arabComma' };
            // 一级：1 绪论（阿拉伯数字+空格+中文，排除量词和年份）
            // 排除：10 个、20 多、2024 年、100 万 等
            if (/^\d+\s+[\u4e00-\u9fa5]/.test(txt) && 
                !/^\d+\s+[个多种项条件位名次年月日万亿%]/.test(txt) && 
                !/^\d+\.\d+/.test(txt)) return { level: 1, format: 'arabSpace' };
            // 一级：1.绪论（阿拉伯数字+点+中文，排除1.1格式）
            if (/^\d+\.[\u4e00-\u9fa5]/.test(txt) && !/^\d+\.\d+/.test(txt)) return { level: 1, format: 'arabDot1' };
            // 四级/五级：（1）（2）（3）后面跟中文或空格+中文
            if (/^[（\(]\d+[）\)]\s*[\u4e00-\u9fa5]/.test(txt)) return { level: 4, format: 'arabParen' };
            // 独立标题（支持中间有空格，必须严格匹配）
            // 结论类：结论、总结、结论与展望、总结与展望、结束语、结语
            if (/^结\s*论$/.test(txt) || /^总\s*结$/.test(txt)) return { level: 1, format: 'standalone' };
            if (/^结\s*论\s*与?\s*展\s*望$/.test(txt) || /^总\s*结\s*与?\s*展\s*望$/.test(txt)) return { level: 1, format: 'standalone' };
            if (/^结\s*束\s*语$/.test(txt) || /^结\s*语$/.test(txt)) return { level: 1, format: 'standalone' };
            // 引言类：引言、绪论、概述、前言
            if (/^引\s*言$/.test(txt) || /^绪\s*论$/.test(txt) || /^概\s*述$/.test(txt) || /^前\s*言$/.test(txt)) return { level: 1, format: 'standalone' };
            return null;
        },
        // 三种论文格式类型定义
        // type1: 第一章 → 1.1 → 1.1.1
        // type2: 一、 → （一）、 → 1、 → （1）
        // type3: 第一章 → 第一节 → 一、 → （一）、
        FORMAT_TYPES: {
            type1: { level1: ['zhangCn', 'zhangNum'], level2: ['arabDot'], level3: ['arabDotDot'] },
            type2: { level1: ['cnComma'], level2: ['cnParen'], level3: ['arabComma'], level4: ['arabParen'] },
            type3: { level1: ['zhangCn', 'zhangNum'], level2: ['jieCn'], level3: ['cnComma'], level4: ['cnParen'] }
        },
        // 根据第一章格式确定论文类型
        detectThesisType: function (firstChapterFormat) {
            if (firstChapterFormat === 'zhangCn' || firstChapterFormat === 'zhangNum') {
                return 'type1or3';  // 需要进一步检测二级标题来区分
            } else if (firstChapterFormat === 'cnComma') {
                return 'type2';
            } else if (firstChapterFormat === 'arabSpace') {
                return 'type1';  // 1 绪论 格式视为 type1
            }
            return 'unknown';
        },
        // 检测是否为一级标题（支持多种格式）
        isChapter1Title: function (txt) {
            var fmt = this.detectHeadingFormat(txt);
            if (!fmt) return false;
            // 只有明确的一级标题格式才返回true
            return fmt.level === 1 && ['zhangCn', 'zhangNum', 'cnComma', 'cnSpace', 'arabComma', 'arabSpace', 'arabDot1', 'standalone'].indexOf(fmt.format) >= 0;
        },
        findFirstChapter: function (doc, start, L) {
            for (var i = start + 1; i <= doc.Paragraphs.Count; i++) {
                var para = doc.Paragraphs.Item(i);
                var rawTxt = para.Range.Text;
                var txt = Utils.cleanText(rawTxt);
                var fullTxt = Utils.getFullText(para);  // 包含自动编号

                // 调试：检查所有含"第X章"的段落（包括自动编号）
                if ((fullTxt.indexOf("第") >= 0 && fullTxt.indexOf("章") > 0) && L) {
                    var isToc = Utils.isTocLine(rawTxt, para);
                    L.debug("Detector", "findFirstChapter P" + i, {
                        txt: fullTxt.substring(0, 25),
                        isTocLine: isToc,
                        match: /^\u7b2c[\u4e00\u4e8c\u4e09\u56db\u4e94\u516d\u4e03\u516b\u4e5d\u5341]+\u7ae0/.test(fullTxt)
                    });
                    // 如果是目录行，继续检查下一个
                    if (isToc) continue;
                }

                // 排除目录行
                if (Utils.isTocLine(rawTxt, para)) continue;
                // 排除太长的段落（标题一般不超过50字）
                if (fullTxt.length > 50) continue;
                // 排除以标点结尾的正文句子
                if (Utils.endsWithPunctuation(fullTxt)) continue;

                // 匹配多种一级标题格式（使用包含自动编号的完整文本）
                // 第X章、第1章、1 标题、一 标题、一、标题
                if (this.isChapter1Title(fullTxt)) return i;
            }
            return -1;
        },
        findAllChapters: function (doc, start, end, L) {
            var chapters = [];
            var skipped = [];  // 记录被跳过的可疑标题
            var firstChapterFormat = null;  // 第一章的格式
            var thesisType = null;  // 论文类型
            var self = this;

            for (var i = start; i <= end; i++) {
                var para = doc.Paragraphs.Item(i);
                var rawTxt = para.Range.Text;
                var txt = Utils.cleanText(rawTxt);
                var fullTxt = Utils.getFullText(para);  // 包含自动编号

                // 调试：检查第一章检测
                if (i === start && L) {
                    var charCodes = [];
                    for (var c = 0; c < Math.min(rawTxt.length, 10); c++) {
                        charCodes.push(rawTxt.charCodeAt(c));
                    }
                    L.debug("Detector", "检查P" + i, { rawLen: rawTxt.length, cleanTxt: fullTxt.substring(0, 20), charCodes: charCodes.join(",") });
                }

                // 排除目录行（含Tab和页码）
                if (Utils.isTocLine(rawTxt, para)) continue;
                // 排除以标点结尾的正文句子
                if (Utils.endsWithPunctuation(fullTxt)) continue;
                // 排除"第X章为"这种正文段落
                if (/^第[一二三四五六七八九十]+章为/.test(fullTxt)) continue;
                // 排除太长的段落（标题一般不超过50字）
                if (fullTxt.length > 50) continue;

                // 检测标题格式
                var fmt = this.detectHeadingFormat(fullTxt);
                if (!fmt) continue;

                // 第一章：记录格式类型
                if (chapters.length === 0 && fmt.level === 1) {
                    firstChapterFormat = fmt.format;
                    thesisType = this.detectThesisType(firstChapterFormat);
                    chapters.push({ index: i, text: fullTxt.substring(0, 30), format: fmt.format });
                    if (L) L.debug("Detector", "第一章格式", { format: firstChapterFormat, thesisType: thesisType });
                    continue;
                }

                // 后续章节：验证格式一致性
                if (fmt.level === 1) {
                    var isValidFormat = false;
                    
                    // 独立标题（结论、总结）始终允许
                    if (fmt.format === 'standalone') {
                        isValidFormat = true;
                    }
                    // 验证格式是否与第一章一致
                    else if (firstChapterFormat) {
                        // 第X章格式：只接受第X章
                        if ((firstChapterFormat === 'zhangCn' || firstChapterFormat === 'zhangNum') && 
                            (fmt.format === 'zhangCn' || fmt.format === 'zhangNum')) {
                            isValidFormat = true;
                        }
                        // 一、或 一 格式：只接受同类（cnComma 和 cnSpace 互通）
                        else if ((firstChapterFormat === 'cnComma' || firstChapterFormat === 'cnSpace') && 
                                 (fmt.format === 'cnComma' || fmt.format === 'cnSpace')) {
                            isValidFormat = true;
                        }
                        // 1、或 1 绪论格式：只接受同类（arabComma 和 arabSpace 互通）
                        else if ((firstChapterFormat === 'arabComma' || firstChapterFormat === 'arabSpace') && 
                                 (fmt.format === 'arabComma' || fmt.format === 'arabSpace')) {
                            isValidFormat = true;
                        }
                        // 1.绪论格式：只接受同类
                        else if (firstChapterFormat === 'arabDot1' && fmt.format === 'arabDot1') {
                            isValidFormat = true;
                        }
                    }

                    if (isValidFormat) {
                        chapters.push({ index: i, text: fullTxt.substring(0, 30), format: fmt.format });
                    } else {
                        // 记录被跳过的可疑标题
                        skipped.push({ index: i, text: fullTxt.substring(0, 30), format: fmt.format, reason: '格式不匹配' });
                        if (L) L.warn("Detector", "跳过不匹配格式", { para: i, text: fullTxt.substring(0, 20), format: fmt.format, expected: firstChapterFormat });
                    }
                }
            }

            // 记录被跳过的标题到日志
            if (skipped.length > 0 && L) {
                L.warn("Detector", "以下内容疑似标题但格式不匹配，已跳过", { count: skipped.length, items: skipped });
            }

            return chapters;
        },
        findReferenceTitle: function (doc, start) {
            // 从文档后半部分开始搜索，避免误检测目录中的“参考文献”
            var searchStart = Math.max(start + 1, Math.floor(doc.Paragraphs.Count * 0.7));
            for (var i = searchStart; i <= doc.Paragraphs.Count; i++) {
                var para = doc.Paragraphs.Item(i);
                var rawTxt = para.Range.Text;
                var txt = Utils.cleanText(rawTxt);
                // 排除目录行
                if (Utils.isTocLine(rawTxt, para)) continue;
                if (txt === "参考文献") return i;
            }
            return -1;
        },
        findAcknowledgement: function (doc, start) {
            // 从文档后半部分开始搜索
            var searchStart = Math.max(start + 1, Math.floor(doc.Paragraphs.Count * 0.7));
            for (var i = searchStart; i <= doc.Paragraphs.Count; i++) {
                var para = doc.Paragraphs.Item(i);
                var rawTxt = para.Range.Text;
                var txt = Utils.cleanText(rawTxt);
                // 排除目录行
                if (Utils.isTocLine(rawTxt, para)) continue;
                if (txt === "致谢" || txt === "致  谢") return i;
            }
            return -1;
        },
        findAll: function (doc, L) {
            var pos = { integrity: -1, abstract: -1, keywords: -1, englishTitle: -1, abstractEn: -1, keywordsEn: -1, toc: -1, chapter1: -1, chapters: [], reference: -1, acknowledgement: -1 };
            pos.integrity = this.findIntegrityDeclaration(doc);
            pos.abstract = this.findAbstractTitle(doc);
            if (pos.abstract > 0) {
                pos.keywords = this.findKeywords(doc, pos.abstract);
                pos.englishTitle = this.findEnglishTitle(doc, pos.abstract);
                pos.abstractEn = this.findAbstractEn(doc, pos.abstract);
                pos.keywordsEn = this.findKeywordsEn(doc, pos.abstractEn);
                pos.toc = this.findTocTitle(doc, pos.abstract);
                pos.chapter1 = this.findFirstChapter(doc, pos.abstract, L);
                pos.reference = this.findReferenceTitle(doc, pos.abstract);
                pos.acknowledgement = this.findAcknowledgement(doc, pos.abstract);
                if (pos.chapter1 > 0) {
                    // 章节搜索范围：从第一章到参考文献（或致谢，或文档末尾）
                    var end = pos.reference > 0 ? pos.reference : (pos.acknowledgement > 0 ? pos.acknowledgement : doc.Paragraphs.Count);
                    pos.chapters = this.findAllChapters(doc, pos.chapter1, end, L);
                }
            }
            if (L) L.info("Detector", "位置检测完成", pos);
            return pos;
        },
        detectType: function (para) {
            // 使用 getFullText 获取完整文本（含自动编号），确保能识别带自动编号的标题
            var txt = Utils.cleanText(Utils.getFullText(para));
            var rawTxt = para.Range.Text;
            if (Utils.hasImage(para)) return "image";
            if (Utils.isEmpty(para)) return "empty";
            if (Utils.isTocLine(rawTxt, para)) return "tocLine";

            // 一级标题：多种格式（第X章/第1章/1 标题/一、标题）
            // 标题不应包含标点符号
            if (!/[，。；：！？,.;:!?]/.test(txt)) {
                // 第X章（中文数字）
                if (/^第[一二三四五六七八九十百]+章/.test(txt)) return "heading1";
                // 第1章（阿拉伯数字）
                if (/^第\d+章/.test(txt)) return "heading1";
                // 1 绪论 / 1.绪论（排除1.1格式）
                if (/^\d+[.\s、]\s*[\u4e00-\u9fa5]/.test(txt) && !/^\d+\.\d+/.test(txt)) return "heading1";
                // 一 绪论 / 一、绪论
                if (/^[一二三四五六七八九十][、\s]\s*[\u4e00-\u9fa5]/.test(txt)) return "heading1";
                // 独立标题：结论、总结、结束语、总结与展望、结论与展望（支持中间有空格）
                if (/^结\s*论$/.test(txt) || /^总\s*结$/.test(txt) || /^结\s*束\s*语$/.test(txt) || 
                    /^总\s*结\s*与\s*展\s*望$/.test(txt) || /^结\s*论\s*与\s*展\s*望$/.test(txt)) return "heading1";
            }

            // 二级标题：X.X 格式（如 1.1 xxx）
            // 排除 @xxx 开头的（代码注解，如 @PostMapping）
            if (/^\d+\.\d+[^\d\.]/.test(txt) && !Utils.endsWithPunctuation(txt) && !/^\d+\.\d+\s*@/.test(txt)) {
                // 确保只有一个点（排除 1.1.1）
                var dotCount = (txt.match(/\./g) || []).length;
                if (dotCount === 1 || /^\d+\.\d+[^\d]/.test(txt)) {
                    return "heading2";
                }
            }

            // 三级标题：X.X.X 格式（如 1.1.1 xxx）
            if (/^\d+\.\d+\.\d+[^\d\.]/.test(txt) && !Utils.endsWithPunctuation(txt)) {
                return "heading3";
            }

            // 四级标题：(X) 或 X) 格式
            if ((/^[\(（]\d+[\)）]/.test(txt) || /^\d+[\)）]/.test(txt)) && !Utils.endsWithPunctuation(txt) && txt.length < 50) {
                return "heading4";
            }

            // 续表题注：以"续表"开头 + 编号
            if (/^续表\s*[\d\-\.]+/.test(txt) && txt.length < 50 && !/[，；,;]/.test(txt)) {
                return "continuedCaption";
            }

            // 题注：图X-X、表X-X、代码X-X 格式
            // 条件：以"图/表/代码"开头 + 编号 + 长度<50 + 不含句中标点 + 不以"如下"结尾（那是正文引导句）
            if (/^(图|表|代码)\s*[\d\-\.]+/.test(txt) && txt.length < 50 && !/[，；,;]/.test(txt) && !/如下$/.test(txt)) {
                return "caption";
            }

            return "body";
        }
    };

    var Formatter = {
        applyFont: function (range, style) {
            try {
                range.Font.Name = style.font;
                range.Font.NameAscii = style.fontEn || style.font;
                range.Font.NameOther = style.fontEn || style.font;
                range.Font.Size = style.size;
                range.Font.Bold = style.bold || false;
            } catch (e) { }
        },
        applyParagraph: function (para, style) {
            try {
                var pf = para.Range.ParagraphFormat;
                if (style.align !== undefined) pf.Alignment = style.align;
                if (style.lineSpacingRule !== undefined) pf.LineSpacingRule = style.lineSpacingRule;
                if (style.lineSpacing !== undefined) pf.LineSpacing = style.lineSpacing;
                if (style.spaceBefore !== undefined) pf.SpaceBefore = style.spaceBefore;
                if (style.spaceAfter !== undefined) pf.SpaceAfter = style.spaceAfter;
                if (style.hangingIndent !== undefined) {
                    // 悬挂缩进：左缩进N字符 + 首行缩进-N字符
                    pf.LeftIndent = 0;
                    pf.FirstLineIndent = 0;
                    pf.CharacterUnitLeftIndent = style.hangingIndent;
                    pf.CharacterUnitFirstLineIndent = -style.hangingIndent;
                } else if (style.firstLineIndent !== undefined) {
                    pf.CharacterUnitFirstLineIndent = style.firstLineIndent;
                    pf.FirstLineIndent = 0;
                    pf.CharacterUnitLeftIndent = 0;
                    pf.LeftIndent = 0;
                }
                if (style.widowControl !== undefined) pf.WidowControl = style.widowControl;
                if (style.keepTogether !== undefined) pf.KeepTogether = style.keepTogether;
                if (style.keepWithNext !== undefined) pf.KeepWithNext = style.keepWithNext;
                if (style.outlineLevel !== undefined) pf.OutlineLevel = style.outlineLevel;
                pf.CharacterUnitRightIndent = 0;
                pf.RightIndent = 0;
            } catch (e) { }
        },
        apply: function (para, style) {
            this.applyFont(para.Range, style);
            this.applyParagraph(para, style);
        },
        // 设置大纲级别和样式（用于目录识别）
        setOutlineLevel: function (para, level) {
            try {
                // 1. 设置OutlineLevel
                para.Range.ParagraphFormat.OutlineLevel = level;
                // 2. 设置样式名为内置标题样式（目录基于样式名生成）
                if (level === 1) {
                    para.Style = "标题 1";
                } else if (level === 2) {
                    para.Style = "标题 2";
                } else if (level === 3) {
                    para.Style = "标题 3";
                }
            } catch (e) { }
        },
        // 关键词混排：中文“关键词”黑体不加粗，英文“Keywords”加粗
        applyKeywordsMix: function (para, style, keyword, isChinese, needBold) {
            try {
                // 先应用整体格式
                this.apply(para, style);

                var rng = para.Range;
                var txt = rng.Text;
                // 同时支持中英文冒号
                var colonPos = txt.indexOf("：");
                if (colonPos < 0) colonPos = txt.indexOf(":");
                if (colonPos > 0) {
                    // 先取消整段加粗（原文可能已全段加粗）
                    rng.Font.Bold = false;
                    // 设置关键词/Keywords部分的字体
                    var start = rng.Start;
                    var keyRng = rng.Duplicate;
                    keyRng.SetRange(start, start + colonPos);  // 不含冒号
                    if (isChinese) {
                        keyRng.Font.Name = "黑体";  // 中文“关键词”用黑体
                    }
                    if (needBold) {
                        keyRng.Font.Bold = true;  // 英文Keywords加粗
                    }
                }
            } catch (e) { }
        }
    };

    var PageManager = {
        setupMargins: function (doc, cfg) {
            try {
                var ps = doc.PageSetup;
                ps.TopMargin = cfg.page.top;
                ps.BottomMargin = cfg.page.bottom;
                ps.LeftMargin = cfg.page.left;
                ps.RightMargin = cfg.page.right;
                ps.HeaderDistance = cfg.page.headerDistance;
                ps.FooterDistance = cfg.page.footerDistance;
            } catch (e) { }
        },
        // 智能分节符处理：清理+重建，确保严格3节
        setupSections: function (doc, pos, L, Detector) {
            var beforeCount = doc.Sections.Count;
            L.debug("PageManager", "分节符处理开始", { currentSections: beforeCount });

            // 辅助函数：获取段落内容（截取前30字符）
            function getParaText(doc, idx) {
                try {
                    var t = doc.Paragraphs.Item(idx).Range.Text;
                    return t.replace(/[\r\n\t]/g, '').substring(0, 30);
                } catch (e) { return ""; }
            }

            // 1. 遍历所有节，在每个节开头插入临时分页符（防止删除分节符时段落合并）
            // 注意：跳过诚信声明书和第一章所在的节，因为这两个位置会在步骤4重新插入分节符
            var sectionCount = doc.Sections.Count;
            L.debug("PageManager", "开始插入临时分页符", { sectionCount: sectionCount });
            
            // 找出第一章所在的节号（需要跳过，因为步骤4会在目录末尾插入新分节符）
            // 注意：不跳过诚信声明书所在的节，因为需要保持封面和诚信声明书分开
            var skipSections = {};
            for (var s = 1; s <= sectionCount; s++) {
                try {
                    var secRange = doc.Sections.Item(s).Range;
                    var secStart = secRange.Start;
                    var secEnd = secRange.End;
                    // 只检查第一章是否在这个节
                    if (pos.chapter1 > 0) {
                        var ch1Start = doc.Paragraphs.Item(pos.chapter1).Range.Start;
                        if (ch1Start >= secStart && ch1Start < secEnd) {
                            skipSections[s] = "chapter1";
                        }
                    }
                } catch (e) {}
            }
            L.debug("PageManager", "需要跳过的节", { skipSections: JSON.stringify(skipSections) });
            
            for (var s = sectionCount; s > 1; s--) {
                // 跳过诚信声明书和第一章所在的节
                if (skipSections[s]) {
                    L.debug("PageManager", "跳过节" + s + "（" + skipSections[s] + "）");
                    continue;
                }
                try {
                    var section = doc.Sections.Item(s);
                    var firstPara = section.Range.Paragraphs.Item(1);
                    var rng = firstPara.Range;
                    rng.Collapse(1);  // wdCollapseStart
                    rng.InsertBreak(7);  // wdPageBreak
                    L.debug("PageManager", "插入临时分页符：节" + s);
                } catch (e) {
                    L.debug("PageManager", "插入临时分页符失败：节" + s, { error: e.message });
                }
            }

            // 2. 使用 Find.Replace 删除所有分节符（每个节开头已有临时分页符保护）
            try {
                var find = doc.Content.Find;
                find.ClearFormatting();
                find.Replacement.ClearFormatting();
                find.Text = "^b";              // 分节符
                find.Replacement.Text = "";    // 直接删除（步骤1已保护关键位置）
                find.Forward = true;
                find.Wrap = 0;                 // wdFindStop
                find.Format = false;
                find.MatchCase = false;
                find.MatchWholeWord = false;
                find.MatchWildcards = false;
                find.Execute(undefined, undefined, undefined, undefined, undefined,
                    undefined, undefined, undefined, undefined, undefined, 2); // wdReplaceAll = 2
                L.debug("PageManager", "Find.Replace删除分节符", { remaining: doc.Sections.Count });
            } catch (e) {
                L.debug("PageManager", "Find.Replace删除失败", { error: e.message });
            }
            L.debug("PageManager", "已清理所有分节符", { sections: doc.Sections.Count });

            // 3. 重新检测位置（因为删除和插入可能导致偏移）
            var newPos = Detector.findAll(doc, L);
            L.debug("PageManager", "删除后重新检测", {
                integrity: newPos.integrity,
                chapter1: newPos.chapter1
            });

            // 4. 从后往前插入分节符（在上一段回车符前插入）
            // 4.1 第一章前插入（第2→3节）
            if (newPos.chapter1 > 1) {
                var tocLastPara = newPos.chapter1 - 1;
                L.debug("PageManager", "准备插入分节符1", {
                    tocLastPara: tocLastPara,
                    tocLastText: getParaText(doc, tocLastPara),
                    chapter1Text: getParaText(doc, newPos.chapter1)
                });
                try {
                    var tocParaRange = doc.Paragraphs.Item(tocLastPara).Range;
                    // 先清理该段落的分页符（避免分页符+分节符同处一页）
                    var tocTxt = tocParaRange.Text;
                    if (tocTxt.indexOf('\f') >= 0) {
                        // 用 Find.Execute 删除分页符，避免破坏段落边界
                        tocParaRange.Find.ClearFormatting();
                        tocParaRange.Find.Replacement.ClearFormatting();
                        tocParaRange.Find.Text = "^m";
                        tocParaRange.Find.Replacement.Text = "";
                        tocParaRange.Find.Forward = true;
                        tocParaRange.Find.Wrap = 0;
                        tocParaRange.Find.Execute(undefined, undefined, undefined, undefined, undefined,
                            undefined, undefined, undefined, undefined, undefined, 2);
                        L.debug("PageManager", "清理目录末尾分页符");
                        tocParaRange = doc.Paragraphs.Item(tocLastPara).Range;  // 重新获取
                    }
                    var insertPos1 = tocParaRange.End - 1;
                    var insertRange1 = doc.Range(insertPos1, insertPos1);
                    insertRange1.InsertBreak(2);  // wdSectionBreakNextPage
                    L.debug("PageManager", "插入分节符1完成：位置" + insertPos1);
                } catch (e) { L.error("PageManager", "第一章前插入分节符失败", { error: e.message }); }
            }

            // 4.2 封面末尾插入分节符（第1→2节）- 查找"二〇二x年xx月"格式的段落
            if (newPos.integrity > 1) {
                // 查找封面日期段落（格式：二〇二x年xx月）
                var coverLastPara = -1;
                for (var pi = 1; pi < newPos.integrity; pi++) {
                    try {
                        var pTxt = getParaText(doc, pi);
                        // 匹配日期格式：二〇二x年xx月 或 20xx年xx月
                        if (/^二〇二[〇一二三四五六七八九]年/.test(pTxt) || /^20\d{2}年\d{1,2}月$/.test(pTxt)) {
                            coverLastPara = pi;
                            L.debug("PageManager", "找到封面日期段落P" + pi, { text: pTxt });
                        }
                    } catch (e) { }
                }
                // 如果没找到日期段落，使用integrity-1
                if (coverLastPara < 0) {
                    coverLastPara = newPos.integrity - 1;
                    L.debug("PageManager", "未找到日期段落，使用默认位置P" + coverLastPara);
                }

                L.debug("PageManager", "准备插入分节符2", {
                    coverLastPara: coverLastPara,
                    coverLastText: getParaText(doc, coverLastPara),
                    integrityText: getParaText(doc, newPos.integrity)
                });
                try {
                    var coverParaRange = doc.Paragraphs.Item(coverLastPara).Range;
                    var insertPos2 = coverParaRange.End - 1;
                    var insertRange2 = doc.Range(insertPos2, insertPos2);
                    insertRange2.InsertBreak(2);  // wdSectionBreakNextPage
                    L.debug("PageManager", "插入分节符2完成：位置" + insertPos2);
                } catch (e) { L.error("PageManager", "封面末尾插入分节符失败", { error: e.message }); }
            }

            // 5. 清理步骤1插入的临时分页符（通过Find.Replace删除连续的分页符）
            try {
                var find2 = doc.Content.Find;
                
                // 5.1 先清理直接相邻的 ^m^m
                find2.ClearFormatting();
                find2.Replacement.ClearFormatting();
                find2.Text = "^m^m";
                find2.Replacement.Text = "^m";
                find2.Forward = true;
                find2.Wrap = 0;
                find2.MatchWildcards = false;
                find2.Execute(undefined, undefined, undefined, undefined, undefined,
                    undefined, undefined, undefined, undefined, undefined, 2);
                
                // 5.2 再清理中间隔一个字符的 ^m^?^m（使用通配符）
                find2.ClearFormatting();
                find2.Replacement.ClearFormatting();
                find2.Text = "^m^?^m";
                find2.Replacement.Text = "^m";
                find2.Forward = true;
                find2.Wrap = 0;
                find2.MatchWildcards = true;
                find2.Execute(undefined, undefined, undefined, undefined, undefined,
                    undefined, undefined, undefined, undefined, undefined, 2);
                find2.MatchWildcards = false;
                
                L.debug("PageManager", "清理临时分页符完成");
            } catch (e) { }

            var afterCount = doc.Sections.Count;
            L.info("PageManager", "分节符处理完成", { before: beforeCount, after: afterCount });
        },
        setupHeaders: function (doc, cfg, L) {
            try {
                // 第一节（封面）：清除页眉内容和边框横线
                var sec1 = doc.Sections.Item(1);
                sec1.Headers.Item(1).LinkToPrevious = false;
                sec1.Headers.Item(1).Range.Delete();  // 清除内容
                // 清除页眉底部边框（横线）
                try {
                    sec1.Headers.Item(1).Range.ParagraphFormat.Borders.Item(-3).LineStyle = 0;  // wdBorderBottom = -3
                } catch (e) {}
                sec1.PageSetup.DifferentFirstPageHeaderFooter = false;

                // 第二节及以后：设置页眉内容和横线
                for (var i = 2; i <= doc.Sections.Count; i++) {
                    var sec = doc.Sections.Item(i);
                    sec.PageSetup.OddAndEvenPagesHeaderFooter = false;  // 取消奇偶页不同
                    var hdr = sec.Headers.Item(1);
                    hdr.LinkToPrevious = false;
                    hdr.Range.Text = cfg.header.content;
                    hdr.Range.Font.Name = cfg.header.font;
                    hdr.Range.Font.Size = cfg.header.size;
                    hdr.Range.ParagraphFormat.Alignment = cfg.header.align;
                    // 添加页眉底部横线
                    try {
                        hdr.Range.ParagraphFormat.Borders.Item(-3).LineStyle = 1;  // wdBorderBottom
                        hdr.Range.ParagraphFormat.Borders.Item(-3).LineWidth = 6;  // 0.5磅
                    } catch (e) {}
                }
                L.info("PageManager", "页眉设置完成");
            } catch (e) { L.error("PageManager", "页眉设置失败", { error: e.message }); }
        },
        setupPageNumbers: function (doc, L) {
            try {
                var cnt = doc.Sections.Count;
                if (cnt < 2) return;
                var f1 = doc.Sections.Item(1).Footers.Item(1);
                f1.LinkToPrevious = false;
                f1.Range.Delete();
                var f2 = doc.Sections.Item(2).Footers.Item(1);
                f2.LinkToPrevious = false;
                f2.Range.Delete();
                if (cnt >= 3) {
                    f2.PageNumbers.Add(1);
                    f2.PageNumbers.NumberStyle = 1;
                    f2.PageNumbers.RestartNumberingAtSection = true;
                    f2.PageNumbers.StartingNumber = 1;
                    var f3 = doc.Sections.Item(3).Footers.Item(1);
                    f3.LinkToPrevious = false;
                    f3.Range.Delete();
                    f3.PageNumbers.Add(1);
                    f3.PageNumbers.NumberStyle = 0;
                    f3.PageNumbers.RestartNumberingAtSection = true;
                    f3.PageNumbers.StartingNumber = 1;
                } else {
                    f2.PageNumbers.Add(1);
                    f2.PageNumbers.NumberStyle = 0;
                    f2.PageNumbers.RestartNumberingAtSection = true;
                    f2.PageNumbers.StartingNumber = 1;
                }
                L.info("PageManager", "页码设置完成");
            } catch (e) { L.error("PageManager", "页码设置失败", { error: e.message }); }
        }
    };

    var BreakManager = {
        hasBreakBefore: function (doc, idx) {
            if (idx <= 1) return false;
            try { return Utils.hasPageBreak(doc.Paragraphs.Item(idx - 1)); }
            catch (e) { return false; }
        },
        // 检查段落前是否有分节符（上一段落末尾是否为分节符）
        hasSectionBreakBefore: function (doc, idx) {
            if (idx <= 1) return false;
            try {
                var prevPara = doc.Paragraphs.Item(idx - 1);
                var txt = prevPara.Range.Text;
                // 分节符字符：14(分栏符), 12(分页符也可能), 或者检查 Section 边界
                for (var i = 0; i < txt.length; i++) {
                    var ch = txt.charCodeAt(i);
                    if (ch === 12 || ch === 14) return true;  // 分页符或分节符
                }
                // 额外检查：当前段落是否是某节的第一段
                var para = doc.Paragraphs.Item(idx);
                var paraStart = para.Range.Start;
                for (var s = 2; s <= doc.Sections.Count; s++) {
                    var secStart = doc.Sections.Item(s).Range.Start;
                    if (Math.abs(paraStart - secStart) < 5) return true;  // 容差5字符
                }
                return false;
            } catch (e) { return false; }
        },
        ensureBreak: function (doc, idx, L) {
            if (this.hasBreakBefore(doc, idx)) {
                L.debug("BreakManager", "P" + idx + "上一段落已有分页符，跳过");
                return;
            }
            try {
                var para = doc.Paragraphs.Item(idx);
                var paraText = para.Range.Text;
                
                // 检查当前段落开头是否已有分页符（charCode 12）
                if (paraText.charCodeAt(0) === 12) {
                    L.debug("BreakManager", "P" + idx + "开头已有分页符，跳过");
                    return;
                }
                
                var displayText = paraText.replace(/[\r\n\t]/g, '').substring(0, 30);
                var rng = para.Range;
                rng.Collapse(1);
                rng.InsertBreak(7);
                L.debug("BreakManager", "插入分页符P" + idx, { text: displayText });
            } catch (e) { }
        },
        // 在指定段落末尾（回车符前）插入分页符字符
        ensureBreakAfter: function (doc, idx, L) {
            try {
                var para = doc.Paragraphs.Item(idx);
                var rng = para.Range;
                var paraText = rng.Text;
                
                // 检查段落是否已有分页符（charCode 12）
                if (paraText.indexOf(String.fromCharCode(12)) >= 0) {
                    L.debug("BreakManager", "P" + idx + "末尾已有分页符，跳过");
                    return;
                }
                
                var displayText = paraText.replace(/[\r\n\t]/g, '').substring(0, 30);
                // 在段落末尾（回车符前）插入分页符字符（不使用InsertBreak避免创建新段落）
                var insertPos = rng.End - 1;
                var insertRng = doc.Range(insertPos, insertPos);
                insertRng.InsertBreak(7);  // wdPageBreak
                L.debug("BreakManager", "插入分页符P" + idx + "末尾", { text: displayText });
            } catch (e) { }
        },
        handleAll: function (doc, pos, L) {
            // 从后往前插入分页符，避免位置偏移问题
            // 顺序：致谢 → 参考文献 → 末章...第二章 → 英文关键词末尾 → 中文关键词末尾 → 摘要
            // 第一章前不需要分页符（已有分节符）

            if (pos.acknowledgement > 0) this.ensureBreak(doc, pos.acknowledgement, L);
            if (pos.reference > 0) this.ensureBreak(doc, pos.reference, L);

            // 从末章往第二章插入（从后往前）
            for (var i = pos.chapters.length - 1; i >= 1; i--) {
                this.ensureBreak(doc, pos.chapters[i].index, L);
            }

            // 英文关键词末尾插入分页符（目录前）- 由步骤10更新目录后处理
            // 中文关键词末尾插入分页符（英文题目前）
            if (pos.keywords > 0) this.ensureBreakAfter(doc, pos.keywords, L);
            // 摘要前插入分页符（诚信声明书后有分节符，自动分页）
            if (pos.abstract > 0) this.ensureBreak(doc, pos.abstract, L);

            // 检查第一章前是否有分节符或分页符，如果都没有则插入分节符
            if (pos.chapter1 > 0) {
                var hasSection = this.hasSectionBreakBefore(doc, pos.chapter1);
                var hasPage = this.hasBreakBefore(doc, pos.chapter1);
                L.debug("BreakManager", "第一章前检查", { chapter1: pos.chapter1, hasSectionBreak: hasSection, hasPageBreak: hasPage });
                
                if (!hasSection && !hasPage) {
                    // 既没有分节符也没有分页符，在目录末尾插入分节符
                    try {
                        var tocLastPara = pos.chapter1 - 1;
                        var tocParaRange = doc.Paragraphs.Item(tocLastPara).Range;
                        var insertPos = tocParaRange.End - 1;
                        var insertRange = doc.Range(insertPos, insertPos);
                        insertRange.InsertBreak(2);  // wdSectionBreakNextPage
                        L.debug("BreakManager", "第一章前补充分节符", { tocLastPara: tocLastPara });
                    } catch (e) {
                        L.warn("BreakManager", "第一章前插入分节符失败", { error: e.message });
                    }
                } else if (hasPage && !hasSection) {
                    // 有分页符但没有分节符，清理分页符并插入分节符
                    try {
                        var ch1Para = doc.Paragraphs.Item(pos.chapter1);
                        var ch1Txt = ch1Para.Range.Text;
                        if (ch1Txt.charCodeAt(0) === 12) {
                            var ch1Rng = ch1Para.Range;
                            ch1Rng.Find.ClearFormatting();
                            ch1Rng.Find.Replacement.ClearFormatting();
                            ch1Rng.Find.Execute(String.fromCharCode(12), false, false, false, false, false, true, 1, false, "", 2);
                            L.debug("BreakManager", "清理第一章开头分页符并插入分节符");
                        }
                        // 插入分节符
                        var tocLastPara = pos.chapter1 - 1;
                        var tocParaRange = doc.Paragraphs.Item(tocLastPara).Range;
                        var insertPos = tocParaRange.End - 1;
                        var insertRange = doc.Range(insertPos, insertPos);
                        insertRange.InsertBreak(2);  // wdSectionBreakNextPage
                    } catch (e) { }
                }
                // 如果已有分节符，不做任何处理
            }

            L.info("BreakManager", "分页符处理完成（从后往前）");
        }
    };

    var Cleaner = {
        clearPageBreakBefore: function (doc, start, L) {
            var cleared = 0;
            for (var i = start; i <= doc.Paragraphs.Count; i++) {
                try {
                    var para = doc.Paragraphs.Item(i);
                    if (para.Range.ParagraphFormat.PageBreakBefore) {
                        para.Range.ParagraphFormat.PageBreakBefore = false;
                        cleared++;
                    }
                } catch (e) { }
            }
            if (cleared > 0) L.info("Cleaner", "清除PageBreakBefore属性", { count: cleared });
        },
        // 清理多余分节符（根据位置保留正确的分节符）
        cleanExtraSectionBreaks: function (doc, pos, L) {
            // 规范要求3节：封面 | 前置部分 | 正文
            // 保留：诚信声明书前的分节符 + 第一章前的分节符（每个位置只保留1个）
            var sectionCount = doc.Sections.Count;
            if (sectionCount <= 3) {
                L.info("Cleaner", "节数正常", { count: sectionCount });
                return 0;
            }

            // 获取关键位置（字符位置）
            var integrityStart = pos.integrity > 0 ? doc.Paragraphs.Item(pos.integrity).Range.Start : -1;
            var chapter1Start = pos.chapter1 > 0 ? doc.Paragraphs.Item(pos.chapter1).Range.Start : -1;
            L.debug("Cleaner", "分节符清理参考位置", {
                integrityPara: pos.integrity, integrityStart: integrityStart,
                chapter1Para: pos.chapter1, chapter1Start: chapter1Start
            });

            // 收集所有分节符位置，判断哪些该删除
            // 每个位置只保留1个分节符
            var toDelete = [];
            var keptIntegrity = false;  // 是否已保留诚信声明书处的分节符
            var keptChapter1 = false;   // 是否已保留第一章处的分节符

            for (var i = 2; i <= sectionCount; i++) {
                try {
                    var sec = doc.Sections.Item(i);
                    var secStart = sec.Range.Start;

                    // 判断这个分节符是否在正确位置（允许±100字符误差）
                    var isNearIntegrity = integrityStart > 0 && Math.abs(secStart - integrityStart) < 100;
                    var isNearChapter1 = chapter1Start > 0 && Math.abs(secStart - chapter1Start) < 100;

                    var keep = false;
                    if (isNearIntegrity && !keptIntegrity) {
                        keep = true;
                        keptIntegrity = true;
                    } else if (isNearChapter1 && !keptChapter1) {
                        keep = true;
                        keptChapter1 = true;
                    }

                    L.debug("Cleaner", "检查分节符" + i, {
                        secStart: secStart,
                        nearIntegrity: isNearIntegrity,
                        nearChapter1: isNearChapter1,
                        keep: keep
                    });

                    if (!keep) {
                        // 这个分节符不需要保留，标记删除
                        toDelete.push(i);
                    }
                } catch (e) { }
            }

            // 从后往前删除（避免索引偏移）
            var removed = 0;
            for (var j = toDelete.length - 1; j >= 0; j--) {
                try {
                    var secIdx = toDelete[j];
                    var sec = doc.Sections.Item(secIdx);
                    var rng = sec.Range;
                    rng.Collapse(1);
                    rng.MoveStart(1, -1);
                    rng.MoveEnd(1, 1);
                    var charCode = rng.Text.charCodeAt(0);
                    if (charCode === 12 || charCode === 14 || charCode === 28 || charCode === 29 || charCode === 30) {
                        rng.Delete();
                        removed++;
                    }
                } catch (e) { }
            }
            L.info("Cleaner", "清理多余分节符", { removed: removed, remaining: doc.Sections.Count });
            return removed;
        },
        // 清理连续多个分页符
        cleanDuplicatePageBreaks: function (doc, start, L) {
            var cleaned = 0;
            // 从后往前遍历，避免索引问题
            for (var i = doc.Paragraphs.Count; i >= start; i--) {
                try {
                    var para = doc.Paragraphs.Item(i);
                    var txt = para.Range.Text;
                    // 检查是否只包含分页符
                    var isOnlyBreak = true;
                    var breakCount = 0;
                    for (var j = 0; j < txt.length; j++) {
                        var ch = txt.charCodeAt(j);
                        if (ch === 12) breakCount++;
                        else if (ch !== 13 && ch !== 10 && ch !== 32 && ch !== 160) isOnlyBreak = false;
                    }
                    // 如果段落只有分页符且前一段也有分页符，删除当前段
                    if (isOnlyBreak && breakCount > 0 && i > start) {
                        var prevPara = doc.Paragraphs.Item(i - 1);
                        var prevTxt = prevPara.Range.Text;
                        var prevHasBreak = false;
                        for (var k = 0; k < prevTxt.length; k++) {
                            if (prevTxt.charCodeAt(k) === 12) { prevHasBreak = true; break; }
                        }
                        if (prevHasBreak) {
                            para.Range.Delete();
                            cleaned++;
                        }
                    }
                } catch (e) { }
            }
            if (cleaned > 0) L.info("Cleaner", "清理连续分页符", { count: cleaned });
            return cleaned;
        },
        clearBackground: function (doc) {
            try { doc.Content.Shading.BackgroundPatternColor = 16777215; } catch (e) { }
        },
        setFontBlack: function (doc) {
            try { doc.Content.Font.Color = 0; } catch (e) { }
        },
        // 设置全文字体黑色，但保留超链接蓝色
        setFontBlackExceptHyperlinks: function (doc, L) {
            try {
                // 先设置全文黑色
                doc.Content.Font.Color = 0;
                // 再将超链接设置为蓝色
                var hlCount = doc.Hyperlinks.Count;
                for (var i = 1; i <= hlCount; i++) {
                    try {
                        doc.Hyperlinks.Item(i).Range.Font.Color = 16711680;  // 蓝色 (BGR: 0xFF0000)
                    } catch (e) { }
                }
                if (L) L.info("Cleaner", "设置字体颜色", { black: "全文", blue: hlCount + "个超链接" });
            } catch (e) { }
        },
        // 清除表格内容的首行缩进
        clearTableIndent: function (doc, L) {
            try {
                var tableCount = doc.Tables.Count;
                if (tableCount === 0) return;
                for (var i = 1; i <= tableCount; i++) {
                    try {
                        var tbl = doc.Tables.Item(i);
                        var rng = tbl.Range;
                        // 清除表格内所有段落的首行缩进
                        rng.ParagraphFormat.CharacterUnitFirstLineIndent = 0;
                        rng.ParagraphFormat.FirstLineIndent = 0;
                        rng.ParagraphFormat.CharacterUnitLeftIndent = 0;
                        rng.ParagraphFormat.LeftIndent = 0;
                    } catch (e) { }
                }
                L.info("Cleaner", "清除表格缩进", { tables: tableCount });
            } catch (e) { }
        },
        // 清理指定范围内的所有分页符（从后往前，保护封面和分节符）
        clearAllPageBreaksInRange: function (doc, startPara, endPara, L) {
            var removed = 0;
            // 先收集所有节的第一段索引，这些段落需要保护
            var sectionFirstParas = {};
            for (var si = 1; si <= doc.Sections.Count; si++) {
                try {
                    var secFirstPara = doc.Sections.Item(si).Range.Paragraphs.Item(1);
                    // 找到这个段落在文档中的索引
                    for (var pi = 1; pi <= doc.Paragraphs.Count; pi++) {
                        if (doc.Paragraphs.Item(pi).Range.Start === secFirstPara.Range.Start) {
                            sectionFirstParas[pi] = true;
                            sectionFirstParas[pi - 1] = true; // 上一段也保护（可能包含分节符）
                            break;
                        }
                    }
                } catch (e) { }
            }

            // 从后往前遍历，避免索引偏移问题
            for (var i = endPara; i >= startPara; i--) {
                try {
                    // 保护节边界段落
                    if (sectionFirstParas[i]) continue;

                    var para = doc.Paragraphs.Item(i);
                    var txt = para.Range.Text;
                    
                    // 检查是否包含分页符（12）
                    var hasPageBreak = false;
                    for (var j = 0; j < txt.length; j++) {
                        if (txt.charCodeAt(j) === 12) { hasPageBreak = true; break; }
                    }
                    if (hasPageBreak) {
                        // 检查除分页符外是否还有其他内容
                        var contentWithoutBreak = txt.replace(/\f/g, '').replace(/[\r\n]/g, '');
                        
                        // 检查是否有分节符
                        var hasSectionBreak = false;
                        for (var s = 0; s < txt.length; s++) {
                            var sc = txt.charCodeAt(s);
                            if (sc === 14 || sc === 28 || sc === 29 || sc === 30) {
                                hasSectionBreak = true;
                                break;
                            }
                        }
                        
                        if (!hasSectionBreak && contentWithoutBreak.length === 0) {
                            // 只有分页符和回车，删除整个段落
                            para.Range.Delete();
                        } else {
                            // 有其他内容，用 Find.Execute 在段落范围内删除分页符
                            // 这样不会破坏段落边界
                            var rng = para.Range;
                            rng.Find.ClearFormatting();
                            rng.Find.Replacement.ClearFormatting();
                            // chr(12) = 分页符，用空字符串替换
                            rng.Find.Execute(String.fromCharCode(12), false, false, false, false, false, true, 1, false, "", 2);
                        }
                        removed++;
                    }
                } catch (e) { }
            }
            L.info("Cleaner", "清理分页符", { range: startPara + "-" + endPara, removed: removed });
            return removed;
        },
        // 调整图片尺寸（支持横竖屏配置）
        resizeImages: function (doc, cfg, L, pos) {
            try {
                if (!cfg.image) return;

                var count = doc.InlineShapes.Count;
                if (count === 0) return;

                // 获取页面可用宽度（页面宽度 - 左右边距）
                var pageSetup = doc.PageSetup;
                var pageWidth = pageSetup.PageWidth;
                var leftMargin = pageSetup.LeftMargin;
                var rightMargin = pageSetup.RightMargin;
                var availableWidth = pageWidth - leftMargin - rightMargin;

                var imgCfg = cfg.image;
                // 获取第一章位置，跳过之前的所有图片（封面/诚信声明书/摘要等前置部分）
                var skipBeforeStart = 0;
                if (pos && pos.chapter1 > 0) {
                    try { skipBeforeStart = doc.Paragraphs.Item(pos.chapter1).Range.Start; } catch (e) { }
                }
                var resized = 0;
                var skipped = 0;
                for (var i = 1; i <= count; i++) {
                    try {
                        var shape = doc.InlineShapes.Item(i);
                        // 只处理图片类型（wdInlineShapePicture = 3）
                        if (shape.Type !== 3) continue;

                        // 按位置跳过：第一章之前的图片不处理
                        if (skipBeforeStart > 0 && shape.Range.Start < skipBeforeStart) {
                            skipped++;
                            continue;
                        }

                        var w = shape.Width;
                        var h = shape.Height;
                        var aspectRatio = w / h;  // 宽高比

                        // 按宽高比选择配置：16:9(1.78)=100%, 4:3(1.33)/3:2(1.5)=80%, 竖图=50%
                        var sizeCfg;
                        var isLandscape = w >= h;
                        if (!isLandscape) {
                            sizeCfg = imgCfg.portrait;  // 竖图
                        } else if (aspectRatio >= 1.7) {
                            sizeCfg = imgCfg.widescreen;  // 16:9 宽屏
                        } else {
                            sizeCfg = imgCfg.standard;  // 4:3, 3:2 标准
                        }

                        var defaultWidth = availableWidth * sizeCfg.defaultWidth;
                        var minWidth = availableWidth * sizeCfg.minWidth;
                        var maxWidth = availableWidth * sizeCfg.maxWidth;

                        var newWidth = w;
                        var newHeight = h;
                        var ratio = h / w;  // 保持原始宽高比

                        if (w > maxWidth) {
                            newWidth = defaultWidth;
                            newHeight = newWidth * ratio;
                        } else if (w < minWidth) {
                            newWidth = defaultWidth;
                            newHeight = newWidth * ratio;
                        }

                        // 竖图高度限制：页面内容高度的75%
                        if (!isLandscape) {
                            var pageHeight = pageSetup.PageHeight;
                            var topMargin = pageSetup.TopMargin;
                            var bottomMargin = pageSetup.BottomMargin;
                            var availableHeight = pageHeight - topMargin - bottomMargin;
                            var maxHeight = availableHeight * 0.75;
                            if (newHeight > maxHeight) {
                                newHeight = maxHeight;
                                newWidth = newHeight / ratio;
                            }
                        }

                        // 设置图片所在段落：居中对齐、无首行缩进
                        try {
                            var paraRng = shape.Range.Paragraphs.Item(1);
                            paraRng.Alignment = 1;  // wdAlignParagraphCenter
                            paraRng.FirstLineIndent = 0;
                            paraRng.CharacterUnitFirstLineIndent = 0;
                        } catch (e2) { }

                        if (newWidth !== w || newHeight !== h) {
                            shape.Width = newWidth;
                            shape.Height = newHeight;
                            resized++;
                        }
                    } catch (e) { }
                }
                if (resized > 0 || skipped > 0) L.info("Cleaner", "调整图片尺寸", { total: count, skipped: skipped, resized: resized });
            } catch (e) { }
        },
        // 设置三线表格（复用 Utils.formatThreeLineTable）
        formatThreeLineTable: function (doc, L) {
            try {
                var tableCount = doc.Tables.Count;
                if (tableCount === 0) return;

                var formatted = 0;
                // 从第4个表格开始（跳过封面2个+诚信声明书1个）
                for (var i = 4; i <= tableCount; i++) {
                    try {
                        var tbl = doc.Tables.Item(i);
                        var rowCount = tbl.Rows.Count;
                        var colCount = tbl.Columns.Count;
                        var isCodeTable = (rowCount === 1 && colCount === 1);

                        if (isCodeTable) {
                            // 代码块表格：特殊处理
                            for (var b = -6; b <= -1; b++) {
                                try { tbl.Borders.Item(b).LineStyle = 0; } catch (e) {}
                            }
                            tbl.Rows.Item(1).Borders.Item(-1).LineStyle = 1;
                            tbl.Rows.Item(1).Borders.Item(-1).LineWidth = 12;
                            tbl.Rows.Item(1).Borders.Item(-3).LineStyle = 1;
                            tbl.Rows.Item(1).Borders.Item(-3).LineWidth = 12;
                            tbl.Range.Font.NameFarEast = "宋体";
                            tbl.Range.Font.NameAscii = "Times New Roman";
                            tbl.Range.Font.Size = 12;
                            tbl.Range.ParagraphFormat.Alignment = 3;
                            tbl.Range.ParagraphFormat.CharacterUnitFirstLineIndent = 2;
                        } else {
                            // 数据表格：复用 Utils
                            Utils.formatThreeLineTable(tbl);
                        }

                        // 清除背景颜色（通用）
                        tbl.Range.Shading.BackgroundPatternColor = 16777215;
                        try {
                            tbl.Rows.Item(1).Shading.BackgroundPatternColor = 16777215;
                            for (var c = 1; c <= colCount; c++) {
                                tbl.Cell(1, c).Shading.BackgroundPatternColor = 16777215;
                            }
                        } catch (e2) { }

                        formatted++;
                    } catch (e) { }
                }
                if (formatted > 0) L.info("Cleaner", "设置三线表", { total: tableCount, formatted: formatted });
            } catch (e) { }
        },
        // 处理跨页表格续表
        handleContinuedTables: function (doc, cfg, L) {
            try {
                var tableCount = doc.Tables.Count;
                if (tableCount === 0) return;

                var processed = 0;
                // 从后往前遍历，避免索引偏移
                for (var t = tableCount; t >= 4; t--) {
                    try {
                        var tbl = doc.Tables.Item(t);
                        var rowCount = tbl.Rows.Count;
                        if (rowCount < 2) continue;  // 至少要有表头和数据行

                        // 检测表格是否跨页
                        var firstRowPage = tbl.Rows.Item(1).Range.Information(3);  // wdActiveEndPageNumber
                        var lastRowPage = tbl.Rows.Item(rowCount).Range.Information(3);

                        if (firstRowPage === lastRowPage) continue;  // 不跨页，跳过

                        // 获取表格上方的题注编号
                        var tableNum = "";
                        try {
                            var tblRange = tbl.Range;
                            var tblStart = tblRange.Start;
                            // 查找表格前的段落
                            for (var p = 1; p <= doc.Paragraphs.Count; p++) {
                                var para = doc.Paragraphs.Item(p);
                                if (para.Range.End >= tblStart) {
                                    // 往前找题注
                                    for (var pp = p - 1; pp >= Math.max(1, p - 3); pp--) {
                                        var prevTxt = doc.Paragraphs.Item(pp).Range.Text.replace(/[\r\n]/g, '');
                                        var match = prevTxt.match(/^表\s*([\d\-\.]+)/);
                                        if (match) {
                                            tableNum = match[1];
                                            break;
                                        }
                                    }
                                    break;
                                }
                            }
                        } catch (e) { }

                        if (!tableNum) continue;  // 没找到编号，跳过

                        // 找到分页点：遍历行找页码变化的位置
                        var splitRowIdx = -1;
                        var currentPage = firstRowPage;
                        for (var r = 2; r <= rowCount; r++) {
                            var rowPage = tbl.Rows.Item(r).Range.Information(3);
                            if (rowPage > currentPage) {
                                splitRowIdx = r;
                                break;
                            }
                        }

                        if (splitRowIdx < 2) continue;

                        // 拆分表格
                        tbl.Split(splitRowIdx);
                        
                        // 获取新表格（拆分后是下一个表格）
                        var newTbl = doc.Tables.Item(t + 1);

                        // 在新表格开头插入标题行（复制原表头）
                        newTbl.Rows.Add(newTbl.Rows.Item(1));
                        // 复制原表格第一行内容到新表格第一行
                        try {
                            var srcRow = tbl.Rows.Item(1);
                            var dstRow = newTbl.Rows.Item(1);
                            for (var c = 1; c <= tbl.Columns.Count; c++) {
                                dstRow.Cells.Item(c).Range.Text = srcRow.Cells.Item(c).Range.Text.replace(/[\r\n]/g, '');
                            }
                        } catch (e) { }

                        // 在新表格上方插入续表题注
                        var newTblRange = newTbl.Range;
                        newTblRange.Collapse(1);  // 折叠到开头
                        newTblRange.InsertParagraphBefore();
                        newTblRange.InsertBefore("续表 " + tableNum);
                        
                        // 格式化续表题注
                        try {
                            var captionPara = newTbl.Range;
                            captionPara.Collapse(1);
                            captionPara.MoveStart(1, -1);  // 移动到前一段落
                            var captionRange = captionPara.Paragraphs.Item(1).Range;
                            captionRange.Font.Name = cfg.continuedCaption.font;
                            captionRange.Font.Size = cfg.continuedCaption.size;
                            captionRange.ParagraphFormat.Alignment = cfg.continuedCaption.align;
                            captionRange.ParagraphFormat.FirstLineIndent = 0;
                            captionRange.ParagraphFormat.CharacterUnitFirstLineIndent = 0;
                        } catch (e) { }

                        // 重新设置新表格的三线表格式
                        try {
                            var newRowCount = newTbl.Rows.Count;
                            // 清除所有边框
                            newTbl.Borders.Item(-1).LineStyle = 0;
                            newTbl.Borders.Item(-3).LineStyle = 0;
                            newTbl.Borders.Item(-2).LineStyle = 0;
                            newTbl.Borders.Item(-4).LineStyle = 0;
                            newTbl.Borders.Item(-5).LineStyle = 0;
                            newTbl.Borders.Item(-6).LineStyle = 0;
                            // 第一行顶部粗线
                            newTbl.Rows.Item(1).Borders.Item(-1).LineStyle = 1;
                            newTbl.Rows.Item(1).Borders.Item(-1).LineWidth = 12;
                            // 第一行底部细线
                            newTbl.Rows.Item(1).Borders.Item(-3).LineStyle = 1;
                            newTbl.Rows.Item(1).Borders.Item(-3).LineWidth = 6;
                            // 最后一行底部粗线
                            newTbl.Rows.Item(newRowCount).Borders.Item(-3).LineStyle = 1;
                            newTbl.Rows.Item(newRowCount).Borders.Item(-3).LineWidth = 12;
                        } catch (e) { }

                        processed++;
                        L.debug("Cleaner", "处理续表", { tableNum: tableNum, splitRow: splitRowIdx });

                    } catch (e) { }
                }

                if (processed > 0) L.info("Cleaner", "续表处理完成", { count: processed });
            } catch (e) { }
        },
        // 格式化目录内容
        formatTOC: function (doc, cfg, L) {
            try {
                if (doc.TablesOfContents.Count === 0) return;

                var toc = doc.TablesOfContents.Item(1);
                var tocCfg = cfg.toc;

                // 修复：目录更新前确保所有标题的大纲级别正确
                L.debug("Cleaner", "目录更新前检查并修复标题大纲级别");
                var tocEnd = toc.Range.End;  // 目录结束位置，跳过目录内的行
                var fixed1 = 0, fixed2 = 0, fixed3 = 0;
                for (var ci = 1; ci <= doc.Paragraphs.Count; ci++) {
                    try {
                        var cp = doc.Paragraphs.Item(ci);
                        // 跳过目录范围内的段落
                        if (cp.Range.Start < tocEnd) continue;
                        // 跳过表格内的段落
                        if (Utils.isInTable(cp)) continue;
                        
                        var cTxt = cp.Range.Text.replace(/[\r\n\f]/g, '').trim();
                        // 跳过目录行（含Tab和页码）
                        if (cTxt.indexOf('\t') >= 0) continue;
                        
                        var outline = 10;
                        try { outline = cp.Range.ParagraphFormat.OutlineLevel; } catch (e2) { }
                        
                        // 一级标题：第X章
                        if (/^第[一二三四五六七八九十]+章\s/.test(cTxt)) {
                            if (outline !== 1) {
                                cp.Range.ParagraphFormat.OutlineLevel = 1;
                                try { cp.Style = doc.Styles.Item("标题 1"); } catch (e2) { }
                                fixed1++;
                                L.info("Cleaner", "修复一级标题P" + ci, { text: cTxt.substring(0, 15), from: outline, to: 1 });
                            }
                            // 复用 Formatter 设置完整格式
                            Formatter.apply(cp, cfg.heading1);
                        }
                        // 二级标题：X.X 格式（如 2.1, 3.2）
                        else if (/^\d+\.\d+\s/.test(cTxt) && !/^\d+\.\d+\.\d+/.test(cTxt)) {
                            if (outline !== 2) {
                                cp.Range.ParagraphFormat.OutlineLevel = 2;
                                try { cp.Style = doc.Styles.Item("标题 2"); } catch (e2) { }
                                fixed2++;
                                L.info("Cleaner", "修复二级标题P" + ci, { text: cTxt.substring(0, 15), from: outline, to: 2 });
                            }
                            Formatter.apply(cp, cfg.heading2);
                        }
                        // 三级标题：X.X.X 格式（如 3.1.1）
                        else if (/^\d+\.\d+\.\d+\s/.test(cTxt)) {
                            if (outline !== 3) {
                                cp.Range.ParagraphFormat.OutlineLevel = 3;
                                try { cp.Style = doc.Styles.Item("标题 3"); } catch (e2) { }
                                fixed3++;
                                L.info("Cleaner", "修复三级标题P" + ci, { text: cTxt.substring(0, 15), from: outline, to: 3 });
                            }
                            Formatter.apply(cp, cfg.heading3);
                        }
                        // 不符合任何标题格式但有标题大纲级别的段落，重置为正文级别
                        // 修复：@PostMapping 等代码注解被错误识别为标题的问题
                        else if (outline >= 1 && outline <= 9) {
                            cp.Range.ParagraphFormat.OutlineLevel = 10;
                            try { cp.Style = doc.Styles.Item(-1); } catch (e2) { }  // wdStyleNormal
                            L.info("Cleaner", "重置伪标题P" + ci, { text: cTxt.substring(0, 20), from: outline, to: 10 });
                        }
                    } catch (e) { }
                }
                if (fixed1 + fixed2 + fixed3 > 0) {
                    L.info("Cleaner", "修复标题大纲级别完成", { heading1: fixed1, heading2: fixed2, heading3: fixed3 });
                }

                // 设置目录级别
                toc.UpperHeadingLevel = tocCfg.upperLevel;
                toc.LowerHeadingLevel = tocCfg.lowerLevel;

                // 更新目录
                toc.Update();

                // 设置目录内容格式
                var rng = toc.Range;
                rng.Font.Name = tocCfg.font;
                rng.Font.NameAscii = "Times New Roman";
                rng.Font.Size = tocCfg.size;
                rng.ParagraphFormat.Alignment = tocCfg.align;
                rng.ParagraphFormat.LineSpacingRule = 5;
                rng.ParagraphFormat.LineSpacing = tocCfg.lineSpacing;

                // 重新设置目录标题格式（目录标题可能被toc.Range覆盖）
                var tocStart = toc.Range.Start;
                var tocEnd = toc.Range.End;
                // 查找目录标题段落
                for (var ti = 1; ti <= doc.Paragraphs.Count; ti++) {
                    var tp = doc.Paragraphs.Item(ti);
                    if (tp.Range.Start > tocEnd) break;
                    var tTxt = tp.Range.Text.replace(/[\r\n]/g, '').trim();
                    if (tTxt === "目录" || tTxt === "目  录") {
                        // 复用 Formatter 设置目录标题格式
                        Formatter.apply(tp, cfg.tocTitle);
                        L.debug("Cleaner", "设置目录标题格式", { para: ti });
                        break;
                    }
                }

                L.info("Cleaner", "格式化目录", { level: tocCfg.upperLevel + "-" + tocCfg.lowerLevel });
            } catch (e) {
                if (L) L.debug("Cleaner", "目录格式化失败", { error: e.message });
            }
        },
        // 清理空段落（从后往前删除，避免索引偏移）
        removeEmptyParagraphs: function (doc, startPara, L) {
            var removed = 0;
            var total = doc.Paragraphs.Count;

            // 从后往前遍历
            for (var i = total; i >= startPara; i--) {
                try {
                    var para = doc.Paragraphs.Item(i);
                    var rng = para.Range;
                    var txt = rng.Text;

                    // 跳过表格内的段落（不安全删除）
                    if (rng.Tables.Count > 0) continue;

                    // 跳过包含分页符或分节符的段落
                    if (txt.indexOf('\f') >= 0) continue;  // 分页符/分节符

                    // 检查是否为纯空段落（只有回车符）
                    var cleanTxt = txt.replace(/[\r\n\t ]/g, '');
                    if (cleanTxt.length === 0) {
                        para.Range.Delete();
                        removed++;
                    }
                } catch (e) { }
            }

            if (removed > 0) L.info("Cleaner", "清理空段落", { removed: removed });
            return removed;
        },
        // 替换英文双引号为中文双引号（仅当引号内容主要是中文时）
        replaceQuotes: function (doc, L) {
            var rng = doc.Content;
            rng.Find.ClearFormatting();
            rng.Find.Text = '"*"';  // 匹配 "xxx"
            rng.Find.Forward = true;
            rng.Find.Wrap = 0;  // wdFindStop
            rng.Find.MatchWildcards = true;

            var count = 0;
            while (rng.Find.Execute()) {
                try {
                    var content = rng.Text;
                    if (content.length < 2) continue;
                    var inner = content.slice(1, -1);  // 去掉首尾引号
                    // 中文字符占比 > 50% 才替换
                    var cnMatch = inner.match(/[\u4e00-\u9fa5]/g);
                    var cnCount = cnMatch ? cnMatch.length : 0;
                    if (inner.length > 0 && cnCount > inner.length * 0.5) {
                        rng.Text = "\u201c" + inner + "\u201d";  // "xxx"
                        count++;
                    }
                } catch (e) { }
                rng.Collapse(0);  // wdCollapseEnd
            }
            if (count > 0) L.info("Cleaner", "替换中文引号", { count: count });
            return count;
        }
    };

    // Verifier: 验证模块
    var CitationManager = {
        // 解析引用编号 "1,2,3" 或 "1-3"
        parseNumbers: function (str) {
            var nums = [];
            var parts = str.split(',');
            for (var i = 0; i < parts.length; i++) {
                var part = parts[i].trim();
                if (part.indexOf('-') > 0) {
                    var range = part.split('-');
                    for (var j = parseInt(range[0]); j <= parseInt(range[1]); j++) {
                        nums.push(j);
                    }
                } else {
                    nums.push(parseInt(part));
                }
            }
            return nums;
        },
        // 查找正文中所有引用标记 [x]
        findAllCitations: function (doc, startPara, endPara) {
            var citations = [];
            for (var i = startPara; i <= endPara; i++) {
                try {
                    var para = doc.Paragraphs.Item(i);
                    var rng = para.Range;
                    var text = rng.Text;
                    var paraStart = rng.Start;
                    // 手动匹配 [数字] 模式
                    var pos = 0;
                    while (pos < text.length) {
                        var bracketStart = text.indexOf('[', pos);
                        if (bracketStart < 0) break;
                        var bracketEnd = text.indexOf(']', bracketStart);
                        if (bracketEnd < 0) break;
                        var content = text.substring(bracketStart + 1, bracketEnd);
                        // 检查是否为纯数字或逗号分隔（不匹配x-y图表编号格式）
                        // 匹配: [1], [1,2,3] 不匹配: [5-1], [J], [M] 等
                        if (/^\d+(?:,\d+)*$/.test(content)) {
                            citations.push({
                                para: i,
                                paraStart: paraStart,
                                localStart: bracketStart,
                                localEnd: bracketEnd + 1,
                                text: '[' + content + ']',
                                numbers: this.parseNumbers(content)
                            });
                        }
                        pos = bracketEnd + 1;
                    }
                } catch (e) { }
            }
            return citations;
        },
        // 检测是否是参考文献条目（参考V2：通过文献类型标识符）
        isReferenceEntry: function (txt) {
            var types = ["[J]", "[M]", "[D]", "[C]", "[N]", "[EB/OL]", "[P]", "[R]", "[S]", "[Z]"];
            for (var i = 0; i < types.length; i++) {
                if (txt.indexOf(types[i]) >= 0) return true;
            }
            return false;
        },
        // 为参考文献条目创建书签（参考V2：通过文献类型标识符检测）
        createRefBookmarks: function (doc, refStart, refEnd, L) {
            var bookmarks = [];
            var refNum = 1;
            if (L) L.debug("Citation", "参考文献范围", { start: refStart, end: refEnd });
            for (var i = refStart; i <= refEnd; i++) {
                try {
                    var para = doc.Paragraphs.Item(i);
                    var text = para.Range.Text;
                    var cleanTxt = text.replace(/[\r\n]/g, '').trim();
                    // 跳过空段落
                    if (cleanTxt.length === 0) continue;
                    // 遇到致谢则停止
                    if (cleanTxt === "致谢" || cleanTxt === "致  谢") break;
                    // 检测是否是参考文献条目（含[J]/[M]/[D]等标识符）
                    var isRef = this.isReferenceEntry(text);
                    if (L) L.debug("Citation", "P" + i + " isRef=" + isRef, { text: cleanTxt.substring(0, 50) });
                    if (!isRef) continue;

                    var bmName = "Ref_" + refNum;
                    // 删除已存在的同名书签
                    try { if (doc.Bookmarks.Exists(bmName)) doc.Bookmarks.Item(bmName).Delete(); } catch (e) { }
                    // 创建新书签
                    doc.Bookmarks.Add(bmName, para.Range);
                    bookmarks.push({ num: refNum, bookmark: bmName, para: i });
                    refNum++;
                } catch (e) { }
            }
            if (L) L.info("Citation", "创建参考文献书签", { count: bookmarks.length });
            return bookmarks;
        },
        // 使用真正的交叉引用（InsertCrossReference）+ 上标格式
        formatCitation: function (doc, citation, refItemIndex, L) {
            try {
                var start = citation.paraStart + citation.localStart;
                var end = citation.paraStart + citation.localEnd;
                var citRng = doc.Range(start, end);
                var origText = citRng.Text;
                var origLen = origText.length;  // [1]的长度

                // 记录引用前面的文字（用于后续定位）
                var prefixLen = Math.min(citation.localStart, 5);  // 最多取5个字符
                var prefixStart = citation.paraStart + citation.localStart - prefixLen;
                var prefixRng = doc.Range(prefixStart, start);
                var prefixText = prefixRng.Text;

                if (L) L.debug("Citation", "处理[" + refItemIndex + "]", {
                    origText: origText,
                    prefix: prefixText,
                    start: start,
                    prefixStart: prefixStart
                });

                // 删除原有文本[x]
                citRng.Delete();

                // 选中插入位置
                citRng = doc.Range(start, start);
                citRng.Select();

                // 插入交叉引用
                if (refItemIndex > 0) {
                    Application.Selection.InsertCrossReference(
                        0,              // wdRefTypeNumberedItem
                        -4,             // wdNumberFullContext  
                        refItemIndex,   // 编号项索引
                        true,           // InsertAsHyperlink
                        false           // IncludePosition
                    );

                    // 直接对Selection设置上标（InsertCrossReference后Selection就是插入的内容）
                    try {
                        Application.Selection.Font.Superscript = true;
                        Application.Selection.Font.Size = 9;
                        if (L) L.debug("Citation", "上标设置成功", { refItemIndex: refItemIndex });
                    } catch (e3) {
                        if (L) L.debug("Citation", "上标设置失败", { error: e3.message });
                    }
                }
            } catch (e) {
                if (L) L.debug("Citation", "交叉引用错误", { error: e.message, num: citation.numbers[0] });
            }
        },
        // 获取编号项列表（用于交叉引用）
        getNumberedItems: function (doc, L) {
            try {
                // wdRefTypeNumberedItem = 0
                var items = doc.GetCrossReferenceItems(0);
                // WPS返回的可能是数组（length）或COM对象（Count）
                var cnt = 0;
                if (items) {
                    if (typeof items.Count !== 'undefined') cnt = items.Count;
                    else if (typeof items.length !== 'undefined') cnt = items.length;
                }
                
                // 查找参考文献编号的起始偏移量（找第一个以 [ 开头的项目）
                var refOffset = 0;
                for (var i = 1; i <= cnt; i++) {
                    try {
                        var itemText = "";
                        if (typeof items.Item !== 'undefined') itemText = items.Item(i);
                        else if (items[i]) itemText = items[i];
                        else if (items[i-1]) itemText = items[i-1];  // 0-indexed array
                        
                        if (itemText && /^\s*\[/.test(String(itemText))) {
                            refOffset = i - 1;  // 偏移量 = 索引 - 1（因为参考文献[1]对应索引i）
                            if (L) L.debug("Citation", "找到参考文献起始位置", { index: i, offset: refOffset, text: String(itemText).substring(0, 30) });
                            break;
                        }
                    } catch (e2) { }
                }
                
                if (L) L.debug("Citation", "GetCrossReferenceItems成功", { count: cnt, refOffset: refOffset });
                return { items: items, count: cnt, refOffset: refOffset };
            } catch (e) {
                if (L) L.debug("Citation", "GetCrossReferenceItems失败", { error: e.message });
                return { items: null, count: 0, refOffset: 0 };
            }
        },
        // 为参考文献条目清理硬编码编号并应用自动编号
        applyRefNumbering: function (doc, pos, cfg, L) {
            if (pos.reference <= 0 || pos.acknowledgement <= 0) return 0;

            var totalParas = doc.Paragraphs.Count;
            var startIdx = Math.min(pos.reference + 1, totalParas);
            var endIdx = Math.min(pos.acknowledgement, totalParas + 1);

            if (startIdx >= endIdx) {
                L.debug("Citation", "参考文献范围无效", { start: startIdx, end: endIdx });
                return 0;
            }

            var style = cfg.referenceBody;
            var refParas = [];
            L.debug("Citation", "参考文献范围", { start: startIdx, end: endIdx - 1 });

            // 第一遍：清理硬编码编号，收集段落索引
            for (var i = startIdx; i < endIdx; i++) {
                try {
                    var para = doc.Paragraphs.Item(i);
                    var txt = para.Range.Text.replace(/[\r\n]/g, '').trim();
                    if (txt.length === 0) continue;
                    if (txt === "致谢" || txt === "致  谢") break;
                    // 排除参考文献标题（防止被错误编号）
                    if (txt === "参考文献" || txt === "参 考 文 献") continue;

                    // 清除现有的列表格式
                    try { para.Range.ListFormat.RemoveNumbers(); } catch(e1) {}

                    // 去除硬编码的编号 [1] 或 ［1］
                    var newTxt = txt;
                    if (txt.charAt(0) == "[" || txt.charAt(0) == "［") {
                        var endBracket = txt.indexOf("]");
                        if (endBracket < 0) endBracket = txt.indexOf("］");
                        if (endBracket > 0 && endBracket < 10) {
                            newTxt = txt.substring(endBracket + 1).replace(/^\s+/, "");
                            para.Range.Text = newTxt + "\r";
                        }
                    }
                    refParas.push(i);
                } catch (e) {
                    L.debug("Citation", "参考文献处理失败P" + i, { error: e.message });
                }
            }

            L.info("Citation", "清理硬编码编号", { count: refParas.length });

            // 第二遍：创建列表模板并应用自动编号
            if (refParas.length > 0) {
                try {
                    var listTemp = doc.ListTemplates.Add(false);
                    if (listTemp) {
                        var lvl = listTemp.ListLevels.Item(1);
                        lvl.NumberFormat = "[%1]";
                        lvl.NumberStyle = 0;
                        lvl.StartAt = 1;
                        lvl.Alignment = 0;
                        lvl.TextPosition = 0;
                        lvl.TabPosition = 0;
                        lvl.NumberPosition = 0;
                        try { lvl.TrailingCharacter = 2; } catch(e2) {}

                        for (var j = 0; j < refParas.length; j++) {
                            try {
                                var p = doc.Paragraphs.Item(refParas[j]);
                                p.Style = -1;
                                p.Range.ListFormat.ApplyListTemplate(listTemp, (j > 0));

                                p.Range.Font.NameFarEast = style.font;
                                p.Range.Font.NameAscii = style.fontEn;
                                p.Range.Font.Name = style.fontEn;
                                p.Range.Font.Size = style.size;
                                p.Range.Font.Bold = false;
                                p.Range.Font.Color = 0;

                                var pf = p.Range.ParagraphFormat;
                                pf.Alignment = 3;
                                pf.SpaceBefore = 0;
                                pf.SpaceAfter = 0;
                                pf.LineSpacingRule = 0;
                                pf.WidowControl = false;
                                pf.KeepTogether = false;
                                pf.PageBreakBefore = false;
                                pf.KeepWithNext = false;
                                pf.CharacterUnitLeftIndent = 0;
                                pf.CharacterUnitFirstLineIndent = -2;
                            } catch(e3) {}
                        }
                        L.info("Citation", "应用自动编号", { count: refParas.length });
                    }
                } catch (e) {
                    L.error("Citation", "自动编号失败", { error: e.message });
                }
            }

            return refParas.length;
        },
        // 已处理的引用位置缓存（避免重复处理同一位置）
        _processedPositions: {},
        
        // 清空缓存（每次processAll开始时调用）
        clearProcessedCache: function() {
            this._processedPositions = {};
        },
        
        // 检查位置是否已处理
        isPositionProcessed: function(pos) {
            return this._processedPositions[pos] === true;
        },
        
        // 标记位置已处理
        markPositionProcessed: function(pos) {
            this._processedPositions[pos] = true;
        },
        
        // 使用Find.Execute精确替换单个引用标记（只替换第一个匹配）
        // 返回: { found: boolean, newStart: number } - newStart用于跳过已处理内容
        // refOffset: 参考文献在编号项列表中的偏移量
        replaceCitationByFind: function (doc, searchStart, searchEnd, num, refOffset, L) {
            var searchRng = doc.Range(searchStart, searchEnd);
            var searchText = "[" + num + "]";
            
            searchRng.Find.ClearFormatting();
            searchRng.Find.Text = searchText;
            searchRng.Find.Forward = true;
            searchRng.Find.Wrap = 0;  // wdFindStop
            searchRng.Find.MatchWildcards = false;
            
            // 循环查找直到找到未处理的位置
            while (searchRng.Find.Execute()) {
                if (searchRng.Start >= searchEnd) return { found: false, newStart: searchStart };
                
                var foundStart = searchRng.Start;
                // 检查该位置是否已处理
                if (this.isPositionProcessed(foundStart)) {
                    // 已处理，跳过继续搜索
                    searchRng.Collapse(0);  // wdCollapseEnd
                    continue;
                }
                
                // 标记该位置已处理
                this.markPositionProcessed(foundStart);
                
                searchRng.Delete();  // 删除 [x]
                
                // 插入交叉引用
                var insertRng = doc.Range(foundStart, foundStart);
                insertRng.Select();
                var insertedLength = 0;
                try {
                    // 使用偏移量计算正确的交叉引用索引
                    var actualIndex = num + (refOffset || 0);
                    // wdNumberFullContext = -4，保留完整格式包括方括号
                    Application.Selection.InsertCrossReference(0, -4, actualIndex, true, false);
                    insertedLength = Application.Selection.End - foundStart;
                    
                    // 立即更新刚插入的域（只更新这一个域，不影响其他域）
                    var fieldRng = doc.Range(foundStart, foundStart + insertedLength);
                    if (fieldRng.Fields.Count > 0) {
                        try { fieldRng.Fields.Item(1).Update(); } catch (e3) {}
                    }
                    
                    // 设置上标格式
                    fieldRng.Font.Superscript = true;
                    fieldRng.Font.Size = 12;  // 小四
                } catch (e) {
                    // 交叉引用失败，插入原文本并设为上标
                    insertRng.InsertAfter(searchText);
                    insertRng.Font.Superscript = true;
                    insertRng.Font.Size = 12;  // 小四
                    insertedLength = searchText.length;
                }
                // 返回新的搜索起点
                return { found: true, newStart: foundStart + insertedLength + 1 };
            }
            
            return { found: false, newStart: searchStart };
        },
        // 解除参考文献引用的REF域（仅处理[数字]格式）
        unlinkCitationFields: function (doc, searchStart, searchEnd, L) {
            try {
                var unlinked = 0;
                // 从后往前遍历，避免索引变化
                for (var j = doc.Fields.Count; j >= 1; j--) {
                    try {
                        var field = doc.Fields.Item(j);
                        var fieldStart = field.Result.Start;
                        if (fieldStart < searchStart || fieldStart > searchEnd) continue;
                        
                        var code = field.Code.Text;
                        // 只处理REF域
                        if (!/REF\s+/i.test(code)) continue;
                        
                        var result = field.Result.Text;
                        // 只处理[数字]格式的参考文献引用，解除链接转为纯文本
                        if (/^\[\d+\]/.test(result)) {
                            field.Unlink();
                            unlinked++;
                        }
                    } catch (e) { }
                }
                if (unlinked > 0) {
                    L.info("Citation", "处理旧交叉引用域", { unlinked: unlinked });
                }
                return unlinked;
            } catch (e) {
                L.debug("Citation", "解除域链接失败", { error: e.message });
                return 0;
            }
        },
        // 处理所有引用
        processAll: function (doc, pos, cfg, L) {
            if (!cfg.citation || !cfg.citation.enabled) return;

            // 清空已处理位置缓存
            this.clearProcessedCache();

            // 1. 获取搜索范围（第一章到参考文献之间）
            var ch1Para = doc.Paragraphs.Item(pos.chapter1);
            var refPara = doc.Paragraphs.Item(pos.reference);
            var searchStart = ch1Para.Range.Start;
            var searchEnd = refPara.Range.Start;

            // 2. 解除旧的参考文献交叉引用域（转为纯文本[x]）
            this.unlinkCitationFields(doc, searchStart, searchEnd, L);

            // 3. 为参考文献条目应用自动编号
            var refCount = this.applyRefNumbering(doc, pos, cfg, L);

            // 3. 根据配置决定是否使用交叉引用
            if (cfg.citation.useCrossReference) {
                // 使用交叉引用（慢，但有超链接功能）
                var result = this.getNumberedItems(doc, L);
                L.debug("Citation", "编号项列表", { count: result.count });

                var citations = this.findAllCitations(doc, pos.chapter1, pos.reference - 1);
                var existingNums = {};
                for (var i = 0; i < citations.length; i++) {
                    var nums = citations[i].numbers;
                    for (var j = 0; j < nums.length; j++) {
                        existingNums[nums[j]] = true;
                    }
                }
                var numsToProcess = [];
                for (var n in existingNums) {
                    if (existingNums.hasOwnProperty(n)) numsToProcess.push(parseInt(n));
                }
                numsToProcess.sort(function(a, b) { return b - a; });
                L.debug("Citation", "待处理编号", { nums: numsToProcess.join(",") });

                var totalCount = 0;
                var refOffset = result.refOffset || 0;
                for (var k = 0; k < numsToProcess.length; k++) {
                    var num = numsToProcess[k];
                    // 检查偏移后的索引是否有效
                    if (num + refOffset > result.count) continue;
                    var numCount = 0;
                    var currentStart = searchStart;
                    var maxIterations = 100;  // 防止死循环的安全限制
                    while (maxIterations-- > 0) {
                        var ret = this.replaceCitationByFind(doc, currentStart, searchEnd, num, refOffset, L);
                        if (!ret.found) break;
                        numCount++;
                        currentStart = ret.newStart;  // 从新位置继续搜索
                        searchEnd = doc.Paragraphs.Item(pos.reference).Range.Start;
                    }
                    if (numCount > 0) {
                        L.debug("Citation", "替换[" + num + "]", { count: numCount });
                        totalCount += numCount;
                    }
                }
                L.info("Citation", "交叉引用完成", { count: totalCount });
                
                // 5. 不再更新全文域（避免影响图片/表格交叉引用）
                // 每个参考文献交叉引用在插入时已单独更新
                
                // 6. 重新设置上标格式
                this.ensureSuperscript(doc, searchStart, searchEnd, L);
            }
        },
        // 确保所有引用设置为上标（最终步骤）
        ensureSuperscript: function (doc, searchStart, searchEnd, L) {
            var searchRng = doc.Range(searchStart, searchEnd);
            searchRng.Find.ClearFormatting();
            searchRng.Find.Text = "\\[[0-9]{1,2}\\]";
            searchRng.Find.Forward = true;
            searchRng.Find.Wrap = 0;
            searchRng.Find.MatchWildcards = true;

            var count = 0;
            while (searchRng.Find.Execute()) {
                if (searchRng.Start >= searchEnd) break;
                count++;
                try {
                    searchRng.Font.Superscript = true;
                    searchRng.Font.Size = 12;  // 小四
                    searchRng.Font.Underline = 0;
                } catch (e) { }
                searchRng.Collapse(0);
            }
            L.info("Citation", "确保上标格式", { count: count });
        }
    };

    // 代码块处理模块：将[CODE][/CODE]标记转换为表格
    var CodeBlockManager = {
        // 查找所有[CODE]...[/CODE]块（支持标记与内容在同一行）
        findCodeBlocks: function (doc) {
            var blocks = [];
            var count = doc.Paragraphs.Count;
            var i = 1;
            while (i <= count) {
                try {
                    var para = doc.Paragraphs.Item(i);
                    var txt = para.Range.Text.replace(/[\r\n]/g, '');
                    // 检查是否包含[CODE]标记（可以是单独一行，也可以是开头）
                    if (txt.indexOf("[CODE]") >= 0) {
                        var startPara = i;
                        var endPara = -1;
                        // 检查同一段落是否也包含[/CODE]（整个代码块在一个段落内）
                        if (txt.indexOf("[/CODE]") > txt.indexOf("[CODE]")) {
                            endPara = i;
                        } else {
                            // 在后续段落中查找[/CODE]
                            for (var j = i + 1; j <= count; j++) {
                                var p2 = doc.Paragraphs.Item(j);
                                var t2 = p2.Range.Text.replace(/[\r\n]/g, '');
                                if (t2.indexOf("[/CODE]") >= 0) {
                                    endPara = j;
                                    break;
                                }
                            }
                        }
                        if (endPara >= startPara) {
                            blocks.push({ start: startPara, end: endPara });
                            i = endPara + 1;
                            continue;
                        }
                    }
                } catch (e) { }
                i++;
            }
            return blocks;
        },
        // 处理单个代码块
        processBlock: function (doc, block, L) {
            try {
                // 收集代码内容（支持标记与内容在同一行）
                var codeLines = [];
                for (var i = block.start; i <= block.end; i++) {
                    var para = doc.Paragraphs.Item(i);
                    var txt = para.Range.Text.replace(/[\r\n]+$/, '');
                    
                    if (i === block.start && i === block.end) {
                        // [CODE]和[/CODE]在同一段落
                        var match = txt.match(/\[CODE\]([\s\S]*?)\[\/CODE\]/);
                        if (match) codeLines.push(match[1]);
                    } else if (i === block.start) {
                        // 第一段：去掉[CODE]标记，保留后面的内容
                        var afterCode = txt.replace(/^.*?\[CODE\]/, '');
                        if (afterCode.trim()) codeLines.push(afterCode);
                    } else if (i === block.end) {
                        // 最后一段：去掉[/CODE]标记，保留前面的内容
                        var beforeEnd = txt.replace(/\[\/CODE\].*$/, '');
                        if (beforeEnd.trim()) codeLines.push(beforeEnd);
                    } else {
                        // 中间段落：保留全部内容
                        codeLines.push(txt);
                    }
                }
                var codeText = codeLines.join('\n');
                if (!codeText.trim()) return false;

                // 获取[CODE]行的Range用于插入表格
                var startPara = doc.Paragraphs.Item(block.start);
                var insertRange = startPara.Range;

                // 删除[CODE]到[/CODE]的所有段落（从后往前删）
                for (var i = block.end; i >= block.start; i--) {
                    doc.Paragraphs.Item(i).Range.Delete();
                }

                // 在原位置插入表格（1行1列）
                var tbl = doc.Tables.Add(insertRange, 1, 1);

                // 设置表格边框：只有上下框线（二线表）
                // WPS边框常量：-1=Top, -2=Left, -3=Bottom, -4=Right, -5=Horizontal, -6=Vertical
                // wdLineStyleSingle=1, wdLineStyleNone=0
                tbl.Borders.Item(-1).LineStyle = 1;  // Top: 有线
                tbl.Borders.Item(-2).LineStyle = 0;  // Left: 无线
                tbl.Borders.Item(-3).LineStyle = 1;  // Bottom: 有线
                tbl.Borders.Item(-4).LineStyle = 0;  // Right: 无线
                try { tbl.Borders.Item(-5).LineStyle = 0; } catch (e) { }  // Horizontal: 无线
                try { tbl.Borders.Item(-6).LineStyle = 0; } catch (e) { }  // Vertical: 无线

                // 设置单元格内容
                var cell = tbl.Cell(1, 1);
                cell.Range.Text = codeText;

                // 设置格式：宋体+TNR小四，两端对齐，首行缩进2字符
                var rng = cell.Range;
                rng.Font.Name = "宋体";
                rng.Font.NameAscii = "Times New Roman";
                rng.Font.Size = 12;  // 小四
                rng.ParagraphFormat.Alignment = 3;  // wdAlignParagraphJustify
                rng.ParagraphFormat.CharacterUnitFirstLineIndent = 2;

                return true;
            } catch (e) {
                if (L) L.debug("CodeBlock", "处理失败", { error: e.message });
                return false;
            }
        },
        // 处理所有代码块
        processAll: function (doc, L) {
            var blocks = this.findCodeBlocks(doc);
            if (blocks.length === 0) {
                L.info("CodeBlock", "未找到[CODE]标记");
                return;
            }
            L.info("CodeBlock", "找到代码块", { count: blocks.length });

            // 从后往前处理（避免位置偏移）
            var success = 0;
            for (var i = blocks.length - 1; i >= 0; i--) {
                if (this.processBlock(doc, blocks[i], L)) success++;
            }
            L.info("CodeBlock", "代码块处理完成", { success: success });
        }
    };

    // 续表处理模块（V2）
    var ContinuationTableManager = {
        // 获取表格信息（通过遍历每行检测页码变化）
        getTableInfo: function (tbl) {
            try {
                var rowCount = tbl.Rows.Count;
                var colCount = 1;
                try { colCount = tbl.Columns.Count; } catch (e) {
                    try { colCount = tbl.Rows.Item(1).Cells.Count; } catch (e2) {}
                }
                
                var startPage = -1, endPage = -1, lastPage = -1;
                var pageChanges = [];
                
                for (var r = 1; r <= rowCount; r++) {
                    try {
                        var row = tbl.Rows.Item(r);
                        var cell = row.Cells.Item(1);
                        var page = cell.Range.Information(3);  // wdActiveEndPageNumber
                        
                        if (startPage === -1) startPage = page;
                        endPage = page;
                        
                        if (lastPage !== -1 && page > lastPage) {
                            pageChanges.push({ row: r, fromPage: lastPage, toPage: page });
                        }
                        lastPage = page;
                    } catch (e) { }
                }
                
                var caption = this.findTableCaption(tbl);
                return { rowCount: rowCount, colCount: colCount, startPage: startPage, endPage: endPage, pageChanges: pageChanges, caption: caption || "未命名表格" };
            } catch (e) { return null; }
        },
        
        // 查找表格标题
        findTableCaption: function (tbl) {
            try {
                var tblStart = tbl.Range.Start;
                var doc = tbl.Range.Document;
                for (var i = 1; i <= doc.Paragraphs.Count; i++) {
                    var para = doc.Paragraphs.Item(i);
                    if (para.Range.Start >= tblStart) {
                        if (i > 1) {
                            var prevPara = doc.Paragraphs.Item(i - 1);
                            var txt = prevPara.Range.Text.replace(/^\s+|\s+$/g, "").replace(/\r/g, "");
                            if (/^表\s*\d+[-－]\d+/.test(txt) || /^表\s*\d+\.\d+/.test(txt)) {
                                return txt.substring(0, 50);
                            }
                        }
                        break;
                    }
                }
            } catch (e) {}
            return null;
        },
        
        // 查找表格前的题注段落
        findTableCaptionParagraph: function (tbl, doc) {
            try {
                var tblStart = tbl.Range.Start;
                for (var i = 1; i <= doc.Paragraphs.Count; i++) {
                    var para = doc.Paragraphs.Item(i);
                    if (para.Range.Start >= tblStart) {
                        if (i > 1) {
                            var prevPara = doc.Paragraphs.Item(i - 1);
                            var txt = prevPara.Range.Text.replace(/^\s+|\s+$/g, "").replace(/\r/g, "");
                            if (/^表\s*\d+[-－]\d+/.test(txt) || /^表\s*\d+\.\d+/.test(txt)) {
                                return prevPara;
                            }
                        }
                        break;
                    }
                }
            } catch (e) {}
            return null;
        },
        
        // 获取续表标题
        getContinuationCaption: function (originalCaption) {
            if (!originalCaption) return "续表";
            if (/^表\s*\d/.test(originalCaption)) {
                return originalCaption.replace(/^表/, "续表");
            }
            return "续表";
        },
        
        // 设置三线表格式（调用 Utils 封装方法）
        formatAsThreeLineTable: function (tbl) {
            Utils.formatThreeLineTable(tbl);
        },
        
        // 分割跨页表格
        splitCrossPageTable: function (doc, tbl, tableInfo, L, cfg) {
            var continuationCount = 0;
            try {
                var rowCount = tbl.Rows.Count;
                var colCount = tableInfo.colCount;
                var headerRows = 1;
                
                // 获取分页点
                var splitRows = [];
                if (tableInfo.pageChanges && tableInfo.pageChanges.length > 0) {
                    for (var pci = 0; pci < tableInfo.pageChanges.length; pci++) {
                        splitRows.push(tableInfo.pageChanges[pci].row);
                    }
                }
                if (splitRows.length === 0) {
                    L.debug("Continuation", "无法确定分页点");
                    return { success: false, continuationCount: 0 };
                }
                
                var splitRow = splitRows[0];
                L.debug("Continuation", "分页点", { row: splitRow, caption: tableInfo.caption });
                
                if (splitRow <= headerRows || splitRow > rowCount) {
                    return { success: false, continuationCount: 0 };
                }
                
                // 分割后行数太少，改为顶到下一页
                var remainingRows = splitRow - 1;
                if (remainingRows + 1 < 3) {
                    var captionPara = this.findTableCaptionParagraph(tbl, doc);
                    if (captionPara) {
                        var insertPos = doc.Range(captionPara.Range.Start, captionPara.Range.Start);
                        insertPos.InsertBreak(7);
                        L.debug("Continuation", "题注+表格顶到下一页");
                        return { success: true, continuationCount: 0 };
                    }
                    return { success: false, continuationCount: 0 };
                }
                
                // 保存需要移动的行数据
                var rowsToMove = [];
                for (var r = splitRow; r <= rowCount; r++) {
                    var rowData = [];
                    try {
                        var row = tbl.Rows.Item(r);
                        for (var c = 1; c <= row.Cells.Count; c++) {
                            try {
                                var cellText = row.Cells.Item(c).Range.Text;
                                cellText = cellText.replace(/\r\x07/g, "").replace(/\x07/g, "");
                                rowData.push(cellText);
                            } catch (e) { rowData.push(""); }
                        }
                        rowsToMove.push(rowData);
                    } catch (e) {}
                }
                
                // 保存表头数据
                var headerData = [];
                for (var h = 1; h <= headerRows; h++) {
                    var hRowData = [];
                    try {
                        var hRow = tbl.Rows.Item(h);
                        for (var hc = 1; hc <= hRow.Cells.Count; hc++) {
                            try {
                                var hCellText = hRow.Cells.Item(hc).Range.Text;
                                hCellText = hCellText.replace(/\r\x07/g, "").replace(/\x07/g, "");
                                hRowData.push(hCellText);
                            } catch (e) { hRowData.push(""); }
                        }
                        headerData.push(hRowData);
                    } catch (e) {}
                }
                
                // 删除原表格中需要移动的行
                for (var d = rowCount; d >= splitRow; d--) {
                    try { tbl.Rows.Item(d).Delete(); } catch (e) {}
                }
                
                // 重新设置原表格三线表格式
                this.formatAsThreeLineTable(tbl);
                
                // 插入续表题注（避免插入多余空段落）
                var tblEnd = tbl.Range.End;
                var continuationCaption = this.getContinuationCaption(tableInfo.caption);
                var insertRange = doc.Range(tblEnd, tblEnd);
                // 只插入题注内容+换行，不在前面加空行
                insertRange.InsertAfter(continuationCaption + "\r");
                
                // 设置续表题注格式（题注紧跟在表格后）
                var captionStart = tblEnd;
                var captionEnd = captionStart + continuationCaption.length;
                // 使用配置设置续表题注格式
                var ccCfg = cfg && cfg.continuedCaption ? cfg.continuedCaption : { font: "宋体", fontEn: "Times New Roman", size: 10.5, align: 0, lineSpacingRule: 5, lineSpacing: 15 };
                try {
                    var captionRng = doc.Range(captionStart, captionEnd);
                    captionRng.Font.NameFarEast = ccCfg.font || "宋体";
                    captionRng.Font.NameAscii = ccCfg.fontEn || "Times New Roman";
                    captionRng.Font.Size = ccCfg.size || 10.5;
                    captionRng.Font.Bold = ccCfg.bold || false;
                    captionRng.ParagraphFormat.Alignment = ccCfg.align !== undefined ? ccCfg.align : 0;
                    captionRng.ParagraphFormat.FirstLineIndent = 0;
                    captionRng.ParagraphFormat.CharacterUnitFirstLineIndent = 0;
                    captionRng.ParagraphFormat.LineSpacingRule = ccCfg.lineSpacingRule || 5;
                    captionRng.ParagraphFormat.LineSpacing = ccCfg.lineSpacing || 15;
                    try { captionRng.ParagraphFormat.OutlineLevel = 10; } catch (e) {}
                } catch (e) {}
                
                // 创建新表格
                var newTableRows = headerRows + rowsToMove.length;
                var newTblPos = captionEnd + 1;
                var newTblRange = doc.Range(newTblPos, newTblPos);
                
                try {
                    var newTbl = doc.Tables.Add(newTblRange, newTableRows, colCount);
                    
                    // 复制原表格宽度设置
                    try {
                        newTbl.PreferredWidthType = tbl.PreferredWidthType;
                        newTbl.PreferredWidth = tbl.PreferredWidth;
                    } catch (e) {}
                    
                    // 复制每列宽度
                    for (var cw = 1; cw <= colCount; cw++) {
                        try { 
                            newTbl.Columns.Item(cw).Width = tbl.Columns.Item(cw).Width; 
                        } catch (e) {}
                    }
                    
                    // 填充表头
                    for (var hi = 0; hi < headerData.length; hi++) {
                        for (var hj = 0; hj < headerData[hi].length && hj < colCount; hj++) {
                            try { newTbl.Cell(hi + 1, hj + 1).Range.Text = headerData[hi][hj]; } catch (e) {}
                        }
                    }
                    
                    // 填充数据行
                    for (var ri = 0; ri < rowsToMove.length; ri++) {
                        for (var rj = 0; rj < rowsToMove[ri].length && rj < colCount; rj++) {
                            try { newTbl.Cell(headerRows + ri + 1, rj + 1).Range.Text = rowsToMove[ri][rj]; } catch (e) {}
                        }
                    }
                    
                    // 设置表格内容格式（避免继承标题样式）
                    try {
                        var tblRange = newTbl.Range;
                        tblRange.Font.NameFarEast = "宋体";
                        tblRange.Font.NameAscii = "Times New Roman";
                        tblRange.Font.Size = 10.5;  // 五号
                        tblRange.Font.Bold = false;
                        tblRange.ParagraphFormat.Alignment = 1;  // 居中
                        tblRange.ParagraphFormat.OutlineLevel = 10;  // 正文级别
                        tblRange.ParagraphFormat.FirstLineIndent = 0;
                        tblRange.ParagraphFormat.CharacterUnitFirstLineIndent = 0;
                    } catch (e) {}
                    
                    this.formatAsThreeLineTable(newTbl);
                    continuationCount++;
                    L.debug("Continuation", "续表创建成功", { rows: newTableRows, cols: colCount });
                } catch (e) {
                    L.debug("Continuation", "创建续表失败", { error: e.message });
                }
                
                return { success: true, continuationCount: continuationCount };
            } catch (e) {
                L.debug("Continuation", "分割失败", { error: e.message });
                return { success: false, continuationCount: 0 };
            }
        },
        
        // 处理所有跨页表格
        processAll: function (doc, L, cfg) {
            var totalCrossPage = 0, totalProcessed = 0, totalContinuation = 0;
            var maxIterations = 10;
            
            for (var iteration = 0; iteration < maxIterations; iteration++) {
                try { doc.Repaginate(); } catch (e) {}
                
                var tableCount = doc.Tables.Count;
                if (tableCount === 0) break;
                
                var crossPageCount = 0, processedCount = 0, continuationCount = 0;
                
                // 从前往后处理（续表在原表后面，这样新续表自然会被下一轮检查）
                var i = 1;
                while (i <= tableCount) {
                    try {
                        var tbl = doc.Tables.Item(i);
                        var tableInfo = this.getTableInfo(tbl);
                        if (!tableInfo) { i++; continue; }
                        
                        var isCrossPage = (tableInfo.pageChanges && tableInfo.pageChanges.length > 0) || (tableInfo.endPage > tableInfo.startPage);
                        if (isCrossPage) {
                            crossPageCount++;
                            var result = this.splitCrossPageTable(doc, tbl, tableInfo, L, cfg);
                            if (result.success) {
                                processedCount++;
                                continuationCount += result.continuationCount;
                                try { doc.Repaginate(); } catch (e) {}
                                // 更新表格总数（可能新增了续表）
                                tableCount = doc.Tables.Count;
                                // 处理成功后，下一个表格（续表）会在 i+1，继续循环自然会检查
                            }
                        }
                    } catch (e) {}
                    i++;
                }
                
                totalCrossPage += crossPageCount;
                totalProcessed += processedCount;
                totalContinuation += continuationCount;
                
                if (crossPageCount === 0) break;
            }
            
            // 格式化所有续表题注
            if (totalContinuation > 0) {
                var ccCfg = cfg && cfg.continuedCaption ? cfg.continuedCaption : { font: "宋体", fontEn: "Times New Roman", size: 10.5, align: 0, lineSpacingRule: 5, lineSpacing: 15 };
                for (var p = 1; p <= doc.Paragraphs.Count; p++) {
                    try {
                        var para = doc.Paragraphs.Item(p);
                        var txt = para.Range.Text.replace(/^\s+|\s+$/g, "").replace(/\r/g, "");
                        if (/^续表\s*[\d\-\.]+/.test(txt) && txt.length < 50) {
                            para.Range.Font.NameFarEast = ccCfg.font || "宋体";
                            para.Range.Font.NameAscii = ccCfg.fontEn || "Times New Roman";
                            para.Range.Font.Size = ccCfg.size || 10.5;
                            para.Range.Font.Bold = ccCfg.bold || false;
                            para.Format.Alignment = ccCfg.align !== undefined ? ccCfg.align : 0;
                            para.Format.FirstLineIndent = 0;
                            para.Format.CharacterUnitFirstLineIndent = 0;
                            para.Format.LineSpacingRule = ccCfg.lineSpacingRule || 5;
                            para.Format.LineSpacing = ccCfg.lineSpacing || 15;
                        }
                    } catch (e) {}
                }
                L.info("Continuation", "续表处理完成", { detected: totalCrossPage, processed: totalProcessed, created: totalContinuation });
            } else {
                L.info("Continuation", "未检测到跨页表格");
            }
            
            return { crossPageCount: totalCrossPage, processedCount: totalProcessed, continuationCount: totalContinuation };
        }
    };

    var Verifier = {
        // 检查段落是否是某个节的第一段
        isSectionStart: function (doc, paraIndex) {
            if (paraIndex <= 1) return false;
            try {
                var para = doc.Paragraphs.Item(paraIndex);
                var paraStart = para.Range.Start;
                for (var si = 1; si <= doc.Sections.Count; si++) {
                    var secFirstPara = doc.Sections.Item(si).Range.Paragraphs.Item(1);
                    if (secFirstPara.Range.Start === paraStart) return true;
                }
            } catch (e) { }
            return false;
        },
        // 检查指定位置前是否有分页符（包括当前段落开头和上一段落末尾）
        // 注意：分节符（wdSectionBreakNextPage）也是字符码12，需要区分
        hasPageBreakBefore: function (doc, paraIndex, skipSectionCheck) {
            if (paraIndex <= 1) return false;
            try {
                // 如果是节的第一段，分节符本身会换页，返回特殊值表示"有分节符"
                if (!skipSectionCheck && this.isSectionStart(doc, paraIndex)) {
                    return "section";  // 分节符导致的换页
                }
                
                // 检查当前段落开头是否有分页符
                var curPara = doc.Paragraphs.Item(paraIndex);
                var curTxt = curPara.Range.Text;
                if (curTxt.charCodeAt(0) === 12) return true;
                
                // 检查上一段落是否有分页符
                var prevPara = doc.Paragraphs.Item(paraIndex - 1);
                return Utils.hasPageBreak(prevPara);
            } catch (e) { return false; }
        },
        // 验证所有关键位置的分页符
        verifyPageBreaks: function (doc, pos, L) {
            var results = [];
            var checks = [
                { name: "摘要", para: pos.abstract, expected: true },
                { name: "英文题目", para: pos.englishTitle, expected: true },
                { name: "目录", para: pos.toc, expected: true },
                { name: "第一章", para: pos.chapter1, expected: false }, // 分节符已换页
                { name: "参考文献", para: pos.reference, expected: true },
                { name: "致谢", para: pos.acknowledgement, expected: true }
            ];
            // 添加章节检查（第二章起）
            for (var i = 1; i < pos.chapters.length; i++) {
                checks.push({ name: "第" + (i + 1) + "章", para: pos.chapters[i].index, expected: true });
            }
            // 执行检查
            for (var j = 0; j < checks.length; j++) {
                var c = checks[j];
                if (c.para > 0) {
                    var has = this.hasPageBreakBefore(doc, c.para);
                    // "section" 表示有分节符（已换页），视为通过
                    var pass = (has === "section") || (has === c.expected) || (c.expected && has === true);
                    var actualStr = has === "section" ? "有分节符" : (has ? "有" : "无");
                    results.push({ name: c.name, para: c.para, expected: c.expected, actual: has, pass: pass });
                    if (!pass) {
                        L.warn("Verifier", c.name + " P" + c.para, { expected: c.expected ? "有分页符" : "无分页符", actual: actualStr });
                    }
                }
            }
            // 统计
            var passed = 0;
            for (var k = 0; k < results.length; k++) if (results[k].pass) passed++;
            L.info("Verifier", "验证完成", { total: results.length, passed: passed, failed: results.length - passed });
            return results;
        }
    };

    return { Utils: Utils, Detector: Detector, Formatter: Formatter, PageManager: PageManager, BreakManager: BreakManager, Cleaner: Cleaner, CitationManager: CitationManager, CodeBlockManager: CodeBlockManager, ContinuationTableManager: ContinuationTableManager, Verifier: Verifier };
}

function _runFormatting(doc, cfg, L, M, D) {
    // D = DEBUG配置，可选
    var snapshot = { before: {}, after: {} };

    L.step(1, "检查文档保护");
    if (doc.ProtectionType != -1) { doc.Unprotect(); L.info("Main", "已解除保护"); }

    L.step(2, "检测文档结构");
    var pos = M.Detector.findAll(doc, L);

    // 前置条件检测
    var missing = [];
    if (pos.integrity <= 0) missing.push("诚信声明书");
    if (pos.abstract <= 0) missing.push("摘要");
    if (pos.toc <= 0) missing.push("目录");
    if (pos.chapter1 <= 0) missing.push("第一章");
    if (missing.length > 0) {
        throw new Error("缺少必要内容：" + missing.join("、") + "\n请补充后重试");
    }

    // 可选项警告
    if (pos.reference <= 0) L.warn("Main", "未找到参考文献");
    if (pos.acknowledgement <= 0) L.warn("Main", "未找到致谢");

    // DEBUG: 记录前置快照
    if (D && D.enabled) {
        if (D.position.sectionBreaks) snapshot.before.sections = M.Utils.getSectionBreaks(doc);
        if (D.position.pageBreaks) {
            snapshot.before.pageBreaks = M.Utils.getPageBreaks(doc);
            if (D.position.showDetail) {
                snapshot.before.pageBreaksDetail = M.Utils.getPageBreaksDetail(doc, D.position.detailLimit);
                L.debug("DEBUG", "分页符详情(前)", snapshot.before.pageBreaksDetail);
            }
        }
    }

    L.step(3, "设置页面");
    M.PageManager.setupMargins(doc, cfg);

    // 智能分节符处理
    M.PageManager.setupSections(doc, pos, L, M.Detector);
    pos = M.Detector.findAll(doc, L);

    L.step(4, "格式化前置部分");
    _formatFrontMatter(doc, pos, cfg, L, M, D);

    L.step(5, "格式化正文");
    var endMain = pos.reference > 0 ? pos.reference : (pos.acknowledgement > 0 ? pos.acknowledgement : doc.Paragraphs.Count);
    if (pos.chapter1 > 0) _formatMainContent(doc, pos.chapter1, endMain, cfg, L, M, D);

    L.step(6, "格式化参考文献和致谢");
    if (pos.reference > 0) _formatReferences(doc, pos.reference, pos.acknowledgement > 0 ? pos.acknowledgement : doc.Paragraphs.Count, cfg, L, M, D);
    if (pos.acknowledgement > 0) _formatAcknowledgement(doc, pos.acknowledgement, cfg, L, M, D);

    L.step(7, "清理并重建分页符");
    // 7.1 获取第2节起始位置（保护封面）
    var section2Start = 1;
    try {
        if (doc.Sections.Count >= 2) {
            section2Start = doc.Sections.Item(2).Range.Paragraphs.Item(1).Range.Start;
            // 找到对应的段落索引
            for (var si = 1; si <= doc.Paragraphs.Count; si++) {
                if (doc.Paragraphs.Item(si).Range.Start >= section2Start) {
                    section2Start = si;
                    break;
                }
            }
        }
    } catch (e) { section2Start = pos.abstract > 0 ? pos.abstract - 5 : 1; }
    L.debug("Main", "清理起点", { section2Start: section2Start });

    // 7.2 清理范围内所有分页符（从后往前）
    var cleanEnd = pos.acknowledgement > 0 ? pos.acknowledgement + 20 : doc.Paragraphs.Count;
    M.Cleaner.clearAllPageBreaksInRange(doc, section2Start, cleanEnd, L);

    // 7.3 重新检测位置（清理后位置会变）
    pos = M.Detector.findAll(doc, L);

    // 7.4 从后往前插入分页符
    M.BreakManager.handleAll(doc, pos, L);

    // 7.5 清理连续分页符（临时分页符+新插入的分页符可能导致重复）
    try {
        var find = doc.Content.Find;
        var totalReplaced = 0;
        
        // 7.5.1 先清理直接相邻的 ^m^m
        find.ClearFormatting();
        find.Replacement.ClearFormatting();
        find.Text = "^m^m";
        find.Replacement.Text = "^m";
        find.Forward = true;
        find.Wrap = 0;
        find.MatchWildcards = false;
        var count1 = 0;
        while (find.Execute(undefined, undefined, undefined, undefined, undefined,
            undefined, undefined, undefined, undefined, undefined, 2)) {
            count1++;
            if (count1 > 50) break;
        }
        totalReplaced += count1;
        
        // 7.5.2 已删除：^m^?^m 模式会误删分节符
        
        if (totalReplaced > 0) L.debug("Cleaner", "清理连续分页符", { count: count1 });
    } catch (e) { }

    L.step(8, "清理格式");
    // 使用 Find.Replace 删除分节符后不再需要此函数
    // M.Cleaner.cleanExtraSectionBreaks(doc, pos, L);
    L.info("Cleaner", "节数正常", { count: doc.Sections.Count });
    // 从诚信声明书或摘要开始清理
    var cleanStart = pos.integrity > 0 ? pos.integrity : pos.abstract;
    M.Cleaner.clearPageBreakBefore(doc, cleanStart, L);
    M.Cleaner.clearTableIndent(doc, L);
    if (cfg.features.resizeImages) M.Cleaner.resizeImages(doc, cfg, L, pos);
    if (cfg.features.formatTables) M.Cleaner.formatThreeLineTable(doc, L);
    M.Cleaner.clearBackground(doc);
    M.Cleaner.setFontBlack(doc);
    if (cfg.features.replaceQuotes) M.Cleaner.replaceQuotes(doc, L);

    L.step(9, "设置页眉页码");
    M.PageManager.setupHeaders(doc, cfg, L);
    M.PageManager.setupPageNumbers(doc, L);

    L.step(10, "更新目录");
    if (cfg.features.formatTOC) M.Cleaner.formatTOC(doc, cfg, L);

    // 目录更新后，在英文关键词末尾补充分页符（目录前）
    pos = M.Detector.findAll(doc, L);
    if (pos.keywordsEn > 0) {
        M.BreakManager.ensureBreakAfter(doc, pos.keywordsEn, L);
    }

    L.step(11, "处理文献引用");
    if (cfg.features.processCitation) {
        // 重新检测位置（步骤10插入分页符后位置变化）
        pos = M.Detector.findAll(doc, L);
        M.CitationManager.processAll(doc, pos, cfg, L);
    }

    L.step(12, "处理代码块");
    if (cfg.features.processCodeBlocks) M.CodeBlockManager.processAll(doc, L);

    L.step(13, "处理续表");
    if (cfg.features.handleContinuedTables) M.ContinuationTableManager.processAll(doc, L, cfg);

    // 设置全文字体黑色（包括超链接上标）
    M.Cleaner.setFontBlack(doc);

    L.step(14, "清理空段落");
    // 从第2节开始清理（保护封面）
    var cleanStart = 1;
    try { cleanStart = doc.Sections.Item(2).Range.Paragraphs.Item(1).Range.Start; cleanStart = doc.Range(cleanStart, cleanStart).Paragraphs.Item(1).Range.End; } catch (e) { }
    for (var p = 1; p <= doc.Paragraphs.Count; p++) { try { if (doc.Paragraphs.Item(p).Range.Start >= cleanStart) { cleanStart = p; break; } } catch (e) { } }
    M.Cleaner.removeEmptyParagraphs(doc, cleanStart, L);

    // 14.5 重新检测并补充分页符（清理空段落可能导致分页符丢失）
    pos = M.Detector.findAll(doc, L);
    M.BreakManager.handleAll(doc, pos, L);

    // 步骤15: 验证分页符位置
    L.step(15, "验证规范");
    pos = M.Detector.findAll(doc, L);  // 最终位置检测
    M.Verifier.verifyPageBreaks(doc, pos, L);

    // DEBUG: 记录后置快照并对比
    if (D && D.enabled) {
        if (D.position.sectionBreaks) {
            snapshot.after.sections = M.Utils.getSectionBreaks(doc);
            L.info("DEBUG", "分节符对比", { before: snapshot.before.sections.length, after: snapshot.after.sections.length });
            if (D.position.showDetail) {
                var secDetail = M.Utils.getSectionBreaksDetail(doc);
                for (var si = 0; si < secDetail.length; si++) {
                    L.debug("DEBUG", "分节符" + secDetail[si].section, { firstPara: secDetail[si].firstPara });
                }
            }
        }
        if (D.position.pageBreaks) {
            snapshot.after.pageBreaks = M.Utils.getPageBreaks(doc);
            L.info("DEBUG", "分页符对比", { before: snapshot.before.pageBreaks.length, after: snapshot.after.pageBreaks.length });
            if (D.position.showDetail) {
                var beforeDetail = M.Utils.getPageBreaksDetail(doc, D.position.detailLimit);
                L.debug("DEBUG", "分页符详情(后)", beforeDetail);
            }
        }
    }

    return pos;
}

function _formatFrontMatter(doc, pos, cfg, L, M, D) {
    // 诚信声明书标题
    if (pos.integrity > 0) {
        var p = doc.Paragraphs.Item(pos.integrity);
        if (D && D.enabled) _logFormatChange(L, M, p, "诚信声明书标题", pos.integrity);
        M.Formatter.apply(p, cfg.integrityTitle);
        // 诚信声明书内容（到摘要前）
        var intEnd = pos.abstract > 0 ? pos.abstract : pos.integrity + 20;
        for (var k = pos.integrity + 1; k < intEnd; k++) {
            var pk = doc.Paragraphs.Item(k);
            if (!M.Utils.isEmpty(pk)) {
                M.Formatter.apply(pk, cfg.integrityBody);
            }
        }
    }
    // 摘要标题（“摘  要”行，黑体小四）
    if (pos.abstract > 0) {
        var p = doc.Paragraphs.Item(pos.abstract);
        if (D && D.enabled && D.format.abstractTitle) _logFormatChange(L, M, p, "摘要标题", pos.abstract);
        M.Formatter.apply(p, cfg.abstractTitle);
    }
    // 摘要页中文题目：若“摘  要”下一段是短段且不以“摘要”“关键词”开头，按模板黑体小二号居中
    var absBodyStart = pos.abstract + 1;
    if (pos.abstract > 0 && cfg.abstractPageChineseTitle) {
        var nextIdx = pos.abstract + 1;
        if (nextIdx !== pos.keywords && nextIdx <= doc.Paragraphs.Count) {
            try {
                var nextPara = doc.Paragraphs.Item(nextIdx);
                var nextTxt = M.Utils.cleanText(nextPara.Range.Text);
                if (nextTxt.length > 0 && nextTxt.length <= 50 && nextTxt.indexOf("关键词") !== 0 && nextTxt.indexOf("摘要") !== 0 && nextTxt.indexOf("：") < 0) {
                    M.Formatter.apply(nextPara, cfg.abstractPageChineseTitle);
                    absBodyStart = pos.abstract + 2;
                }
            } catch (e) {}
        }
    }
    // 摘要正文（宋体小四，固定值25磅）
    var absEnd = pos.keywords > 0 ? pos.keywords : (pos.englishTitle > 0 ? pos.englishTitle : pos.abstract + 10);
    var absBodyCount = 0;
    for (var i = absBodyStart; i < absEnd; i++) {
        var p = doc.Paragraphs.Item(i);
        if (!M.Utils.isEmpty(p)) {
            if (D && D.enabled && D.format.abstractBody && absBodyCount < D.sampleCount) {
                _logFormatChange(L, M, p, "摘要正文", i);
                absBodyCount++;
            }
            M.Formatter.apply(p, cfg.abstractBody);
        }
    }
    // 关键词（“关键词”三个字黑体不加粗）
    if (pos.keywords > 0) {
        var p = doc.Paragraphs.Item(pos.keywords);
        if (D && D.enabled && D.format.keywords) _logFormatChange(L, M, p, "关键词", pos.keywords);
        M.Formatter.applyKeywordsMix(p, cfg.keywords, "关键词", true, false);  // 黑体不加粗
    }
    // 英文题目（清除大纲级别，避免被识别为标题）
    if (pos.englishTitle > 0) {
        var p = doc.Paragraphs.Item(pos.englishTitle);
        if (D && D.enabled && D.format.englishTitle) _logFormatChange(L, M, p, "英文题目", pos.englishTitle);
        try { p.Range.ParagraphFormat.OutlineLevel = 10; } catch (e) { }  // 设为正文级别
        try { p.Format.Style = "正文"; } catch (e) { }  // 清除可能的标题样式
        M.Formatter.apply(p, cfg.englishTitle);
    }
    // ABSTRACT
    if (pos.abstractEn > 0) {
        var p = doc.Paragraphs.Item(pos.abstractEn);
        if (D && D.enabled && D.format.abstractEn) _logFormatChange(L, M, p, "ABSTRACT标题", pos.abstractEn);
        M.Formatter.apply(p, cfg.abstractEnTitle);
        var enEnd = pos.keywordsEn > 0 ? pos.keywordsEn : (pos.toc > 0 ? pos.toc : pos.abstractEn + 10);
        var enBodyCount = 0;
        for (var j = pos.abstractEn + 1; j < enEnd; j++) {
            var p2 = doc.Paragraphs.Item(j);
            if (!M.Utils.isEmpty(p2)) {
                if (D && D.enabled && D.format.abstractEnBody && enBodyCount < D.sampleCount) {
                    _logFormatChange(L, M, p2, "英文摘要正文", j);
                    enBodyCount++;
                }
                M.Formatter.apply(p2, cfg.abstractEnBody);
            }
        }
    }
    // 英文关键词（混排：Keywords:加粗）
    if (pos.keywordsEn > 0) {
        var p = doc.Paragraphs.Item(pos.keywordsEn);
        if (D && D.enabled && D.format.keywordsEn) _logFormatChange(L, M, p, "英文关键词", pos.keywordsEn);
        M.Formatter.applyKeywordsMix(p, cfg.keywordsEn, "Keywords", false, true);  // 加粗
    }
    // 目录标题：由 formatTOC 统一处理，避免重复设置后被覆盖
}

// 记录格式变化（前后对比）
function _logFormatChange(L, M, para, typeName, paraIndex) {
    var before = M.Utils.getParaFormat(para);
    if (before && L) {
        L.debug("FORMAT", typeName + " P" + paraIndex, before);
    }
}

function _formatMainContent(doc, start, end, cfg, L, M, D) {
    // 向前扫描：检查 start 之前是否有只有自动编号的空段落（如"第一章"+分页符）
    // 这种段落不在格式化范围内，但需要删除
    for (var pre = start - 1; pre > 0 && pre > start - 5; pre--) {
        try {
            var prePara = doc.Paragraphs.Item(pre);
            var preListStr = "";
            try { preListStr = prePara.Range.ListFormat.ListString || ""; } catch(e) {}
            // 如果有章节编号格式的自动编号
            if (preListStr && /^第[一二三四五六七八九十\d]+章/.test(preListStr)) {
                var preRawText = prePara.Range.Text.replace(/[\r\n\f\t\s]/g, '');
                if (preRawText.length === 0) {
                    // 空内容段落，删除
                    prePara.Range.Delete();
                    if (L) L.debug("LIST", "P" + pre + " 删除前置空编号段落", { listString: preListStr });
                    start--; // 调整起始位置
                }
            }
        } catch(e) {}
    }
    
    var counts = { heading1: 0, heading2: 0, heading3: 0, body: 0, caption: 0 };
    for (var i = start; i < end; i++) {
        var para = doc.Paragraphs.Item(i);

        // 跳过表格内的段落
        if (M.Utils.isInTable(para)) continue;

        var type = M.Detector.detectType(para);
        if (type === "empty" || type === "tocLine") continue;
        if (type === "image") { try { para.Range.ParagraphFormat.Alignment = 1; para.Range.ParagraphFormat.CharacterUnitFirstLineIndent = 0; } catch (e) { } continue; }

        var style;
        var outlineLevel = 10; // 10 = 正文
        var allowedLevels = cfg.headingLevels || [1, 2, 3];
        switch (type) {
            case "heading1": 
                if (allowedLevels.indexOf(1) >= 0) { style = cfg.heading1; outlineLevel = 1; }
                else { style = cfg.body; outlineLevel = 10; }
                break;
            case "heading2": 
                if (allowedLevels.indexOf(2) >= 0) { style = cfg.heading2; outlineLevel = 2; }
                else { style = cfg.body; outlineLevel = 10; }
                break;
            case "heading3": 
                if (allowedLevels.indexOf(3) >= 0) { style = cfg.heading3; outlineLevel = 3; }
                else { style = cfg.body; outlineLevel = 10; }
                break;
            case "heading4": 
                if (allowedLevels.indexOf(4) >= 0) { style = cfg.heading4; outlineLevel = 4; }
                else { style = cfg.body; outlineLevel = 10; }
                break;
            case "continuedCaption":
                // 续表题注：左对齐
                style = cfg.continuedCaption;
                break;
            case "caption":
                // 代码题注使用小四号，其他题注使用五号
                var capTxt = M.Utils.cleanText(para.Range.Text);
                if (/^代码\s*\d/.test(capTxt)) {
                    style = cfg.codeCaption;
                } else {
                    style = cfg.caption;
                }
                break;
            default: style = cfg.body;
        }

        // DEBUG: 一级标题样式（修改前）
        var styleNameBefore = "";
        var outlineBefore = 10;
        if (D && D.enabled && type === "heading1") {
            try { outlineBefore = para.Range.ParagraphFormat.OutlineLevel; } catch (e) { }
            try {
                // WPS桌面版：尝试 para.Format.Style 或直接属性
                if (para.Format && para.Format.Style) {
                    styleNameBefore = para.Format.Style.NameLocal || para.Format.Style.Name || String(para.Format.Style);
                } else if (para.Style) {
                    styleNameBefore = para.Style.NameLocal || para.Style.Name || String(para.Style);
                } else {
                    styleNameBefore = "无Style";
                }
            } catch (e) { styleNameBefore = "err:" + (e.message || e).substring(0, 30); }
            L.debug("STYLE", "heading1 P" + i + " 修改前", { styleName: styleNameBefore, outlineLevel: outlineBefore });
        }

        // DEBUG: 格式对比（修改前）
        if (D && D.enabled && D.format[type] && counts[type] < D.sampleCount) {
            _logFormatChange(L, M, para, type, i);
            counts[type]++;
        }

        // 处理自动编号：转换为纯文本，保留编号内容
        if (outlineLevel <= 4) {
            try {
                var listStr = para.Range.ListFormat.ListString || "";
                
                if (listStr) {
                    // 记录调试信息
                    var listType = 0;
                    try { listType = para.Range.ListFormat.ListType; } catch (e2) {}
                    var rawText = para.Range.Text || "";
                    var fullText = M.Utils.getFullText(para);
                    
                    if (L) {
                        L.debug("LIST", "P" + i + " 自动编号转换", {
                            listType: listType,
                            listString: listStr,
                            rawText: rawText.substring(0, 30).replace(/\r/g, "\\r"),
                            fullText: fullText.substring(0, 30)
                        });
                    }
                    
                    // 检查段落是否有实际文本内容（排除分页符、回车等）
                    var rawTextClean = rawText.replace(/[\r\n\f\t\s]/g, '');
                    
                    if (rawTextClean.length === 0) {
                        // 空内容段落（只有分页符/回车+自动编号），直接删除该段落
                        // 因为编号内容已经在下一个段落通过 InsertBefore 插入了
                        para.Range.Delete();
                        if (L) L.debug("LIST", "P" + i + " 删除空编号段落");
                    } else {
                        // 有内容的段落，删除编号后插入纯文本
                        // 检查是否以分页符开头（charCode 12）
                        var startsWithPageBreak = (rawText.charCodeAt(0) === 12);
                        
                        para.Range.ListFormat.RemoveNumbers();
                        
                        if (startsWithPageBreak) {
                            // 分页符在开头：用 ^m 删除所有分页符，分页符由 BreakManager 统一处理
                            // 注意：RemoveNumbers() 后需要重新获取 Range
                            var paraRange = doc.Paragraphs.Item(i).Range;
                            paraRange.Find.ClearFormatting();
                            paraRange.Find.Replacement.ClearFormatting();
                            paraRange.Find.Text = "^m";
                            paraRange.Find.Replacement.Text = "";
                            paraRange.Find.Forward = true;
                            paraRange.Find.Wrap = 0;
                            paraRange.Find.Execute(undefined, undefined, undefined, undefined, undefined,
                                undefined, undefined, undefined, undefined, undefined, 2);  // wdReplaceAll
                            // 只插入编号，不含分页符（避免段落拆分）
                            // 再次重新获取 Range
                            doc.Paragraphs.Item(i).Range.InsertBefore(listStr + " ");
                            if (L) L.debug("LIST", "P" + i + " 删除开头分页符并插入编号");
                        } else {
                            // 正常情况：直接在开头插入编号
                            para.Range.InsertBefore(listStr + " ");
                        }
                    }
                }
            } catch (e) { }
        }

        // 设置大纲级别：在配置中的级别设置标题样式，否则设为正文级别
        if (outlineLevel <= 4) {
            M.Formatter.setOutlineLevel(para, outlineLevel);
        } else {
            // 正文段落：设为正文级别(10)
            try { para.Range.ParagraphFormat.OutlineLevel = 10; } catch (e) { }
        }

        // 再应用自定义格式（覆盖样式的默认格式）
        M.Formatter.apply(para, style);

        // DEBUG: 一级标题样式（修改后）
        if (D && D.enabled && type === "heading1") {
            var styleNameAfter = "";
            var outlineAfter = 10;
            try { outlineAfter = para.Range.ParagraphFormat.OutlineLevel; } catch (e) { }
            try {
                if (para.Format && para.Format.Style) {
                    styleNameAfter = para.Format.Style.NameLocal || para.Format.Style.Name || String(para.Format.Style);
                } else if (para.Style) {
                    styleNameAfter = para.Style.NameLocal || para.Style.Name || String(para.Style);
                } else {
                    styleNameAfter = "无Style";
                }
            } catch (e) { styleNameAfter = "err:" + (e.message || e).substring(0, 30); }
            L.debug("STYLE", "heading1 P" + i + " 修改后", { styleName: styleNameAfter, outlineLevel: outlineAfter });
        }
    }
}

function _formatReferences(doc, start, end, cfg, L, M, D) {
    var p = doc.Paragraphs.Item(start);
    if (D && D.enabled && D.format.reference) _logFormatChange(L, M, p, "参考文献标题", start);
    M.Formatter.setOutlineLevel(p, 1);  // 设置为一级大纲，并使用"标题 1"样式
    M.Formatter.apply(p, cfg.referenceTitle);
    var refCount = 0;
    for (var i = start + 1; i < end; i++) {
        var p = doc.Paragraphs.Item(i);
        if (!M.Utils.isEmpty(p)) {
            if (D && D.enabled && D.format.reference && refCount < D.sampleCount) {
                _logFormatChange(L, M, p, "参考文献条目", i);
                refCount++;
            }
            M.Formatter.apply(p, cfg.referenceBody);
        }
    }
}

function _formatAcknowledgement(doc, start, cfg, L, M, D) {
    var p = doc.Paragraphs.Item(start);
    if (D && D.enabled && D.format.acknowledgement) _logFormatChange(L, M, p, "致谢标题", start);
    M.Formatter.setOutlineLevel(p, 1);  // 设置为一级大纲，并使用"标题 1"样式
    M.Formatter.apply(p, cfg.ackTitle);
    var ackCount = 0;
    for (var i = start + 1; i <= doc.Paragraphs.Count; i++) {
        var p = doc.Paragraphs.Item(i);
        if (!M.Utils.isEmpty(p)) {
            if (D && D.enabled && D.format.acknowledgement && ackCount < D.sampleCount) {
                _logFormatChange(L, M, p, "致谢内容", i);
                ackCount++;
            }
            M.Formatter.apply(p, cfg.ackBody);
        }
    }
}

function _generateReport(L, doc, pos) {
    return {
        meta: { version: "GXWGY v1", document: doc.Name, time: new Date().toISOString(), duration: (new Date() - L.stats.startTime) + "ms" },
        structure: { paragraphs: doc.Paragraphs.Count, sections: doc.Sections.Count, positions: pos },
        stats: L.stats,
        logs: L.entries
    };
}

function _showReport(report) {
    // 生成完整日志内容
    var lines = [];
    lines.push("========================================");
    lines.push("  广西外国语学院本科毕设论文 格式化报告 (GXWGY v1)");
    lines.push("========================================");
    lines.push("");
    lines.push("文档: " + report.meta.document);
    lines.push("时间: " + report.meta.time);
    lines.push("耗时: " + report.meta.duration);
    lines.push("");
    lines.push("--- 文档结构 ---");
    lines.push("段落数: " + report.structure.paragraphs);
    lines.push("节数: " + report.structure.sections);
    lines.push("");
    lines.push("--- 位置检测 ---");
    var p = report.structure.positions;
    lines.push("诚信声明书: P" + p.integrity);
    lines.push("摘要: P" + p.abstract);
    lines.push("关键词: P" + p.keywords);
    lines.push("英文题目: P" + p.englishTitle);
    lines.push("ABSTRACT: P" + p.abstractEn);
    lines.push("目录: P" + p.toc);
    lines.push("第一章: P" + p.chapter1);
    lines.push("参考文献: P" + p.reference);
    lines.push("致谢: P" + p.acknowledgement);
    lines.push("");
    lines.push("--- 章节列表 (" + p.chapters.length + ") ---");
    for (var i = 0; i < p.chapters.length; i++) {
        lines.push("  P" + p.chapters[i].index + ": " + p.chapters[i].text);
    }
    lines.push("");
    lines.push("--- 执行统计 ---");
    lines.push("错误: " + report.stats.errors);
    lines.push("警告: " + report.stats.warnings);
    lines.push("");
    lines.push("--- 执行日志 (" + report.logs.length + "条) ---");
    for (var j = 0; j < report.logs.length; j++) {
        var log = report.logs[j];
        var dataStr = log.d ? " " + JSON.stringify(log.d) : "";
        lines.push("[" + log.t + "ms] [" + log.l + "] " + log.m + ": " + log.msg + dataStr);
    }
    lines.push("");
    lines.push("========================================");

    // 创建新文档显示日志
    var logDoc = Application.Documents.Add();
    logDoc.Content.Text = lines.join("\r\n");
    logDoc.Content.Font.Name = "Consolas";
    logDoc.Content.Font.Size = 10;

    // 简短弹窗提示
    alert("格式化完成！\n\n耗时: " + report.meta.duration + "\n节数: " + report.structure.sections + "\n章节: " + p.chapters.length + "\n\n详细日志已在新文档中显示");
}

/**
 * 独立的续表处理函数 V2
 * 用于单独处理跨页表格，添加续表题注
 */
function HandleContinuedTables() {
    var doc = Application.ActiveDocument;
    if (!doc) {
        alert("请先打开文档！");
        return;
    }

    Application.ScreenUpdating = false;
    var L = _createLogger();
    var M = _createModules(L);
    var cfg = _createConfig();  // 获取配置

    L.info("Main", "开始处理续表 V2");
    
    try {
        var result = M.ContinuationTableManager.processAll(doc, L, cfg);
        
        Application.ScreenUpdating = true;
        
        if (result.crossPageCount > 0) {
            alert("续表处理完成！\n\n" +
                  "检测到跨页表格: " + result.crossPageCount + " 个\n" +
                  "成功处理: " + result.processedCount + " 个\n" +
                  "创建续表: " + result.continuationCount + " 个");
        } else {
            alert("未发现需要处理的跨页表格");
        }
    } catch (e) {
        Application.ScreenUpdating = true;
        L.error("Main", "续表处理失败", { error: e.message });
        alert("续表处理失败: " + e.message);
    }
}
