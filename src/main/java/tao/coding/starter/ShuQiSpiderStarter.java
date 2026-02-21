import tao.coding.component.Reader;
import tao.coding.flow.FlowEngine;

/**
 * 本程序在编时，该网站大多数书籍均为免费阅读，后该网站调整运营策略，全文章需要 VIP校验，仅前 20 章可免费阅读
 * 最新流程：    Reader[载]  →  Selector[择]  →  Parser[析]  →  Decoder[译]  →  Formatter[椠]  →  Writer[录]  →  Merger[撰]  →  Cleaner[涤]
 * 下载VIP章节需在 {@link Reader#read0 } 中添加对应 cookie 认证
 * 默认写入文件路径 {@link FlowEngine#FOLDER_FORMATTER } 可修改默认位置，修改前缀即可
 * 是否启用清理行为 {@link FlowEngine#NEED_DELETE } 默认启用，合并完成时会清理零散章节
 * 是否启用测试模式 {@link FlowEngine#IS_TEST } 默认启用，测试模式下仅下载前 20 章内容
 * 是否启用调试模式 {@link FlowEngine#IS_DEBUG } 默认禁止，调试模式下，会将抓取的章节内容输出至控制台，不会写入文件
 * 是否启用本地解密模式 {@link FlowEngine#USE_NATIVE } 默认启用，使用java本地解密方法免加载js脚本，免排队更快
 */
void main() {
    try (var engine = FlowEngine.getDefaultFlowEngine()) {
        engine.start("斗破苍穹", "武动乾坤", "大主宰", "元尊");
    }
}