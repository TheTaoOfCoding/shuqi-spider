package tao.coding.entity;

/**
 * 不可道
 * 道之曷清？
 * 言之何明？
 */
public interface Ineffable {
    // 常不言道者，尝言道
    default void tryExplain() {
        /*
         * 老子（赵老爷）素质三问
         * 「
         *      你言道吗？
         *      你配言道？
         *      你敢言道？
         * 」
         */
        throw new UnsupportedOperationException("The Tao that can be trodden is not the enduring and unchanging Tao. — Lao Tzu");// 道可道，非常道
    }
}