package com.ticketingsystem.yuzhonblog.service;

import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 敏感词过滤器 — 基于 houbb/sensitive-word DFA 引擎 + 多层自定义防御
 * 防御能力：DFA词库(7万+) | 全角/繁体/形近字 | 倒写检测 | 数字谐音 | 混淆攻击
 */
@Slf4j
@Service
public class SensitiveWordFilter {

    private SensitiveWordBs sensitiveWordBs;

    // ===== 数字/符号谐音绕过 =====
    private static final String[][] NUM_SYMBOL_BYPASS = {
            {"操1你妈", "操你妈"}, {"操你1妈", "操你妈"}, {"操1你1妈", "操你妈"},
            {"傻1逼", "傻逼"}, {"傻@逼", "傻逼"}, {"傻#逼", "傻逼"}, {"傻!逼", "傻逼"},
            {"傻*逼", "傻逼"}, {"傻.逼", "傻逼"}, {"傻,逼", "傻逼"}, {"傻_逼", "傻逼"},
            {"傻-逼", "傻逼"}, {"傻~逼", "傻逼"}, {"傻`逼", "傻逼"},
            {"s@b", "sb"}, {"s#b", "sb"}, {"s!b", "sb"}, {"s.b", "sb"}, {"s_b", "sb"}, {"s-b", "sb"},
            {"2@b", "2b"}, {"2#b", "2b"}, {"2!b", "2b"},
            {"f@ck", "fuck"}, {"f#ck", "fuck"}, {"f!ck", "fuck"}, {"f*ck", "fuck"}, {"f.ck", "fuck"},
            {"s#it", "shit"}, {"s!it", "shit"}, {"s*it", "shit"},
            {"b!tch", "bitch"}, {"b#tch", "bitch"}, {"b*tch", "bitch"},
            {"a$$", "ass"}, {"@$$", "ass"}, {"a##", "ass"},
            {"操@你@妈", "操你妈"}, {"操#你#妈", "操你妈"}, {"操!你!妈", "操你妈"},
            {"草@你@马", "操你妈"}, {"草#你#马", "操你妈"},
    };

    // ===== 拼音全写绕过 =====
    private static final String[] PINYIN_FULL = {
            "caonima", "caonm", "caoni", "caonimei", "caonime", "caosini", "caosinima",
            "gannima", "ganni", "ganniniang", "ganiniang", "ganima",
            "rinima", "rinm", "rini", "rinimei",
            "shabi", "shab", "sabi", "shabiyang", "shadiao", "shagou", "shadongxi",
            "jianbi", "jianb", "jianren", "jianhuo",
            "biaozi", "biaoz", "biaoziyangde",
            "gouri", "gouride", "goudongxi", "gouniangyangde", "gouniang",
            "tamade", "tamad", "tmd", "tmde",
            "nimabi", "nimab", "nimade", "nimalegb",
            "wocaonima", "wocaonm", "wocao", "wocaoni", "wkao",
            "nmsl", "nm$l", "nmslwsnd", "wdnmd",
            "fuckyou", "fckyou", "fkyou", "fku",
            "motherfucker", "mtherfcker", "mofo",
            "sonofbitch", "sonofabitch", "sob",
            "goddamn", "goddam", "gddmn",
            "fuckoff", "fckoff", "fkoff",
    };

    // ===== 结构化检测正则 =====
    private static final Pattern PHONE_PATTERN = Pattern.compile("1[3-9]\\d{9}");
    private static final Pattern URL_PATTERN = Pattern.compile("https?://|www\\d*\\.|\\.(com|cn|net|top|xyz|cc|vip|org|io|info|biz)");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("@\\w+\\.");
    private static final Pattern LONG_NUM_PATTERN = Pattern.compile("\\d{13,}");

    // ===== 混淆攻击关键模式 =====
    private static final String[] CRITICAL_PATTERNS = {
            "caonima", "caonm", "caoni", "gannima", "gannm", "ganni",
            "rinima", "rinm", "rini", "shabi", "shab", "sabi", "shadiao",
            "jianbi", "jianb", "jianren", "biaozi", "gouri", "gouride",
            "tamade", "tamad", "nimabi", "nimab", "wocaonima", "wocaonm",
            "nmsl", "wdnmd", "fuckyou", "fckyou", "motherfucker", "mtherfcker",
            "sonofbitch", "goddamn", "fuckoff",
    };

    // ===== 分类提示消息 =====
    private static final String MSG_CIVIL = "请文明交流";
    private static final String MSG_GAMBLE = "请勿发布赌博相关信息";
    private static final String MSG_SCAM = "请勿发布违法广告信息";
    private static final String MSG_POLITICAL = "请遵守法律法规";
    private static final String MSG_ADS = "请勿发布广告信息";
    private static final String MSG_LEWD = "请勿发布违规内容";
    private static final String MSG_URL = "请勿包含网址链接";
    private static final String MSG_EMAIL = "请勿包含邮箱地址";
    private static final String MSG_PHONE = "请勿包含手机号码";
    private static final String MSG_LONGNUM = "请勿包含长数字串";
    private static final String MSG_SPAM = "请勿重复发送相同内容";
    private static final String MSG_GIBBERISH = "请勿发送过长的乱码内容";

    @PostConstruct
    public void init() {
        sensitiveWordBs = SensitiveWordBs.newInstance()
                .ignoreCase(true)
                .ignoreWidth(true)
                .ignoreNumStyle(true)
                .ignoreChineseStyle(true)
                .ignoreEnglishStyle(true)
                .ignoreRepeat(true)
                .enableNumCheck(true)
                .enableEmailCheck(true)
                .enableUrlCheck(true)
                .enableIpv4Check(true)
                .init();

        addCustomWords();
        log.info("SensitiveWordFilter initialized with multi-layer defense");
    }

    private void addCustomWords() {
        String[] customWords = {
                // ===== 赌博 =====
                "赌博", "赌场", "赌钱", "赌球", "赌马", "赌狗", "赌局", "赌友",
                "博彩", "菠菜", "老虎机", "百家乐", "捕鱼游戏", "炸金花", "牛牛", "斗牛",
                "下注", "投注", "压注", "加注", "梭哈", "赌注", "赌资", "赌金",
                "棋牌", "电玩城", "娱乐城", "赌城", "真人视讯", "真人荷官",
                "彩票", "时时彩", "六合彩", "北京赛车", "快三", "11选5",
                // ===== 诈骗/违法 =====
                "代办", "代办证件", "代办学历", "代办签证", "代办贷款", "代办驾照",
                "刻章", "刻公章", "办证", "办假证", "假证",
                "开发票", "代开发票", "增值税发票",
                "套现", "养卡", "提额", "花呗", "白条", "借呗", "信用卡",
                "贷款", "网贷", "借贷", "放款", "小额贷", "无抵押", "秒到账", "黑户",
                "刷单", "刷信誉", "刷钻", "刷信用", "刷量", "刷粉", "刷流量", "刷榜", "刷好评",
                "传销", "拉人头", "发展下线", "金字塔", "庞氏", "资金盘", "互助盘",
                "中奖", "中大奖", "恭喜中奖", "幸运用户", "免费领取", "0元购",
                "兼职", "日结", "日赚", "在家赚钱", "手机赚钱",
                "洗钱", "洗黑钱", "跑分", "跑分平台",
                // ===== 广告营销 =====
                "微商", "代理", "招代理", "招加盟", "诚招代理",
                "减肥", "瘦身", "丰胸", "壮阳", "增粗", "延时",
                "股票", "炒股", "荐股", "牛股", "涨停", "内幕消息",
                "比特币", "虚拟币", "数字货币", "挖矿", "矿机", "区块链", "空投", "币圈",
                "外汇", "黄金", "期货", "原油", "现货", "贵金属",
                "课程", "培训", "考证", "保过", "包过", "包拿证",
                "包治", "根治", "秘方", "祖传", "特效药",
                // ===== 政治敏感 =====
                "卖国", "精日", "分裂国家", "颠覆政权",
                "反党", "反共", "反华", "反中", "反政府",
                "台独", "港独", "藏独", "疆独", "东突",
                "法轮功", "法轮", "falun", "Falun",
                "六四", "天安门事件", "坦克人",
                // ===== 色情低俗 =====
                "约炮", "约p", "约P", "约pao", "打炮", "炮友",
                "嫖娼", "嫖客", "招嫖", "卖淫",
                "包养", "求包养", "被包养",
                "大保健", "全套", "半套", "莞式", "水疗会所", "丝足",
                "看片", "簧片", "a片", "A片",
                "裸聊", "裸体", "裸照",
                "黄色", "黄片", "黄网", "色情",
                "一夜情", "成人", "成人电影", "成人用品", "成人网站",
                "性交", "性爱", "做爱", "自慰",
                "强奸", "轮奸", "猥亵",
                "萝莉", "幼女", "幼齿",
                "鸡巴", "阴茎", "阴道", "操逼", "日批",
                // ===== 辱骂 =====
                "操你妈", "操你", "我操", "操蛋", "操逼", "操尼玛",
                "草泥马", "草拟吗", "草你妈", "草尼玛",
                "肏", "肏你", "肏尼玛",
                "日你", "日你妈", "日你全家", "狗日的",
                "干你", "干你妈", "干你娘", "干尼玛",
                "傻逼", "傻b", "傻B", "煞笔", "傻杯", "傻叉", "傻比", "傻批", "傻缺",
                "弱智", "智障", "脑残", "脑瘫", "白痴", "蠢货", "蠢猪", "废物",
                "贱人", "贱货", "贱逼", "贱婢", "贱种",
                "婊子", "婊砸", "婊子养的", "臭婊子",
                "狗逼", "狗比", "狗东西", "狗娘养的", "狗杂种", "狗屎",
                "畜生", "禽兽", "杂种", "野种",
                "烂人", "烂货", "烂逼",
                "死全家", "死妈", "死爹",
                "龟孙", "龟儿子", "王八蛋",
                "cnm", "cnmb", "cnmlgb", "nmsl", "nmb", "wqnmlgb", "rnm", "mlgb",
                "sb", "s b", "s.b", "s-b", "2b", "2B",
                "fuck", "shit", "damn", "bitch", "asshole", "bastard", "whore", "slut", "cunt",
                "motherfucker", "nigger", "retard", "dick", "cock", "pussy",
                // ===== 引流/联系方式 =====
                "加我qq", "加我QQ", "加扣扣", "扣扣号", "qq号", "QQ号",
                "加我微信", "加v信", "加V信", "加我v", "加我V", "加vx", "加VX",
                "加好友", "添加好友", "加我好友", "加群", "进群", "拉群", "入群",
                "扫码", "扫一扫", "扫码加", "扫码领取", "扫码进群",
                "私聊我", "找我私聊", "联系我", "加我私聊",
                "qq群", "Q群", "微信群", "电报群",
        };
        sensitiveWordBs.addWord(Arrays.asList(customWords));
    }

    /**
     * 内容检测入口（跳过结构化检测，允许URL/邮箱/电话等技术内容）。
     * 适用于文章正文、项目描述等技术内容。
     */
    public String checkContent(String text) {
        if (text == null || text.isBlank()) return null;

        String raw = text.toLowerCase().replaceAll("[\\s　\\r\\n\\t]", "");
        if (raw.isEmpty()) return null;

        // 跳过结构化检测，直接进入DFA引擎
        boolean found = sensitiveWordBs.contains(text);
        if (found) {
            List<String> words = sensitiveWordBs.findAll(text);
            String hitWord = words.isEmpty() ? "" : words.get(0);
            return classifyHit(hitWord, raw);
        }

        String reversed = new StringBuilder(raw).reverse().toString();
        if (sensitiveWordBs.contains(reversed)) return MSG_CIVIL;

        for (String[] pair : NUM_SYMBOL_BYPASS) {
            if (raw.contains(pair[0])) return pair[1];
        }

        return null;
    }

    /**
     * 主检测入口。返回 null 表示通过，非 null 为拒绝原因。
     */
    public String check(String text) {
        if (text == null || text.isBlank()) return null;

        String raw = text.toLowerCase().replaceAll("[\\s　\\r\\n\\t]", "");
        if (raw.isEmpty()) return null;

        // ---------- 第1层：结构化检测 ----------
        String r1 = checkStructural(raw);
        if (r1 != null) return r1;

        // ---------- 第2层：DFA 引擎检测（内置 7万+ 词典 + 全角/繁体/形近字/重复压缩） ----------
        boolean found = sensitiveWordBs.contains(text);
        if (found) {
            List<String> words = sensitiveWordBs.findAll(text);
            String hitWord = words.isEmpty() ? "" : words.get(0);
            return classifyHit(hitWord, raw);
        }

        // ---------- 第3层：倒写检测 ----------
        String reversed = new StringBuilder(raw).reverse().toString();
        if (sensitiveWordBs.contains(reversed)) return MSG_CIVIL;

        // ---------- 第4层：数字/符号谐音绕过 ----------
        for (String[] pair : NUM_SYMBOL_BYPASS) {
            String normalized = pair[0].toLowerCase();
            if (raw.contains(normalized)) {
                return classifyHit(pair[1], raw);
            }
        }

        // ---------- 第5层：拼音全写绕过 ----------
        String lettersOnly = raw.replaceAll("[^a-z]", "");
        if (lettersOnly.length() >= 3) {
            for (String py : PINYIN_FULL) {
                if (lettersOnly.contains(py)) return MSG_CIVIL;
            }
        }

        // ---------- 第6层：混淆攻击检测 ----------
        String r6 = checkObfuscated(raw);
        if (r6 != null) return r6;

        return null;
    }

    /**
     * 替换敏感词为 **（非阻断场景）
     */
    public String filter(String text) {
        if (text == null) return null;
        return sensitiveWordBs.replace(text);
    }

    /**
     * 查找所有敏感词
     */
    public List<String> findAll(String text) {
        if (text == null || text.isBlank()) return List.of();
        return sensitiveWordBs.findAll(text);
    }

    /**
     * 运行时追加词库
     */
    public void addWord(String... words) {
        sensitiveWordBs.addWord(Arrays.asList(words));
    }

    /**
     * 移除词（白名单）
     */
    public void removeWord(String... words) {
        sensitiveWordBs.removeWord(Arrays.asList(words));
    }

    // ==================== 内部方法 ====================

    private String checkStructural(String raw) {
        if (URL_PATTERN.matcher(raw).find()) return MSG_URL;
        if (EMAIL_PATTERN.matcher(raw).find()) return MSG_EMAIL;
        if (PHONE_PATTERN.matcher(raw).find()) return MSG_PHONE;
        if (LONG_NUM_PATTERN.matcher(raw).find()) return MSG_LONGNUM;
        if (raw.matches(".*(.)\\1{9,}.*")) return MSG_SPAM;
        if (raw.replaceAll("[^a-z]", "").length() > 200) return MSG_GIBBERISH;
        return null;
    }

    private String checkObfuscated(String norm) {
        for (String p : CRITICAL_PATTERNS) {
            if (norm.contains(p)) return MSG_CIVIL;
        }
        return null;
    }

    private String classifyHit(String hitWord, String fullText) {
        String hw = hitWord.toLowerCase();
        String ft = fullText.toLowerCase();

        String[] gambling = {"赌博", "赌场", "赌钱", "赌球", "博彩", "老虎机", "百家乐", "捕鱼", "下注", "投注", "棋牌", "彩票", "六合彩"};
        for (String w : gambling) {
            if (hw.contains(w) || ft.contains(w)) return MSG_GAMBLE;
        }

        String[] scam = {"代办", "开发票", "刻章", "办证", "刷单", "传销", "中奖", "免费领取", "兼职", "日结", "洗钱", "跑分", "套现", "贷款", "网贷"};
        for (String w : scam) {
            if (hw.contains(w) || ft.contains(w)) return MSG_SCAM;
        }

        String[] political = {"反党", "反华", "台独", "港独", "藏独", "疆独", "法轮", "六四", "天安门", "卖国", "精日", "分裂", "颠覆"};
        for (String w : political) {
            if (hw.contains(w) || ft.contains(w)) return MSG_POLITICAL;
        }

        String[] ads = {"微商", "代理", "减肥", "瘦身", "股票", "炒股", "比特币", "虚拟币", "课程", "培训", "壮阳", "丰胸", "期货", "外汇"};
        for (String w : ads) {
            if (hw.contains(w) || ft.contains(w)) return MSG_ADS;
        }

        String[] lewd = {"约炮", "嫖", "卖淫", "包养", "裸聊", "色情", "黄片", "一夜情", "性交", "做爱", "自慰", "强奸", "鸡巴", "阴茎"};
        for (String w : lewd) {
            if (hw.contains(w) || ft.contains(w)) return MSG_LEWD;
        }

        String[] contact = {"加我", "加群", "扫码", "私聊", "联系我", "qq", "微信", "vx", "wx"};
        for (String w : contact) {
            if (hw.contains(w) || ft.contains(w)) return "请勿发布引流信息";
        }

        return MSG_CIVIL;
    }
}
