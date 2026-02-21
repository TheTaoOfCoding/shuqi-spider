package tao.coding.entity;

/**
 * 章节内容对象：ChapterContent 为密文，尚需解密，属性为什么首字母大写？？因为人家返回的就是这样
 */
public record Content(String state, String message, String ChapterContent) {
}