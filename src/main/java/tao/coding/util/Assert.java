package tao.coding.util;

import java.util.Collection;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * 断言工具类
 */
@SuppressWarnings("all")
public class Assert {

    private Assert() {
    }

    public static <T, E extends RuntimeException> void isTrue(T t, Predicate<T> predicate, Supplier<E> exSupplier) {
        Objects.requireNonNull(predicate, "predicate cannot be null.");
        Objects.requireNonNull(exSupplier, "exSupplier cannot be null.");
        // 断言结果
        boolean asserted = predicate.test(t);
        // 要求断言必须通过
        ifNotThrow(asserted, exSupplier);
    }

    public static <T, E extends RuntimeException> void isFalse(T t, Predicate<T> predicate, Supplier<E> exSupplier) {
        Objects.requireNonNull(predicate, "predicate cannot be null.");
        Objects.requireNonNull(exSupplier, "exSupplier cannot be null.");
        // 断言结果
        boolean asserted = predicate.test(t);
        // 要求断言必须不通过
        ifThrow(asserted, exSupplier);
    }

    public static <T, U, E extends RuntimeException> void isTrue(T left, U right, BiPredicate<T, U> predicate, Supplier<E> exSupplier) {
        Objects.requireNonNull(predicate, "predicate cannot be null.");
        Objects.requireNonNull(exSupplier, "exSupplier cannot be null.");
        // 断言结果
        boolean asserted = predicate.test(left, right);
        // 要求断言必须通过
        ifNotThrow(asserted, exSupplier);
    }

    public static <T, U, E extends RuntimeException> void isFalse(T left, U right, BiPredicate<T, U> predicate, Supplier<E> exSupplier) {
        Objects.requireNonNull(predicate, "predicate cannot be null.");
        Objects.requireNonNull(exSupplier, "exSupplier cannot be null.");
        // 断言结果
        boolean asserted = predicate.test(left, right);
        // 要求断言必须不通过
        ifThrow(asserted, exSupplier);
    }

    /*
     * 快速抛出异常的方法
     */
    public static <E extends RuntimeException> void ifThrow(boolean asserted, Supplier<E> exSupplier) {
        if (asserted) throw exSupplier.get();
    }

    public static <E extends RuntimeException> void ifNotThrow(boolean asserted, Supplier<E> exSupplier) {
        ifThrow(!asserted, exSupplier);
    }

    /*
     * 断言时的常用方法,提供静态方法引用
     */
    public static class Predicates {
        // 对象为空
        public static <T> boolean isNull(T t) {
            return Objects.isNull(t);
        }

        // 对象非空
        public static <T> boolean isNotNull(T t) {
            return Objects.nonNull(t);
        }

        // str 非空
        public static boolean strNotBlank(String str) {
            return isNotNull(str) && !str.isBlank();
        }

        // 集合非空
        public static <T> boolean collectionNotEmpty(Collection<T> collection) {
            return isNotNull(collection) && !collection.isEmpty();
        }

        // 数组非空
        public static <T> boolean arrayNotEmpty(T[] array) {
            return isNotNull(array) && array.length > 0;
        }

        // str 正则匹配
        public static boolean strRgx(String str, String regex) {
            return isNotNull(str)
                    && isNotNull(regex)
                    && Pattern.matches(regex, str);
        }

        // 对象相等
        public static <T, U> boolean isEq(T left, U right) {
            return Objects.equals(left, right);
        }

        // 对象不相等
        public static <T, U> boolean isNotEq(T left, U right) {
            return !isEq(left, right);
        }

        // 比较后相等,不做判空校验，逻辑不允许
        public static <T extends Comparable<T>> boolean isOrderEq(T left, T right) {
            return left.compareTo(right) == 0;
        }

        // 比较后不相等,不做判空校验，逻辑不允许
        public static <T extends Comparable<T>> boolean isOrderNotEq(T left, T right) {
            return !isOrderEq(left, right);
        }

        // 比较后大于,不做判空校验，逻辑不允许
        public static <T extends Comparable<T>> boolean isOrderGt(T left, T right) {
            return left.compareTo(right) > 0;
        }

        // 比较后大于等于,不做判空校验，逻辑不允许
        public static <T extends Comparable<T>> boolean isOrderGe(T left, T right) {
            return left.compareTo(right) >= 0;
        }

        // 比较后小于,不做判空校验，逻辑不允许
        public static <T extends Comparable<T>> boolean isOrderLt(T left, T right) {
            return left.compareTo(right) < 0;
        }

        // 比较后小于等于,不做判空校验，逻辑不允许
        public static <T extends Comparable<T>> boolean isOrderLe(T left, T right) {
            return left.compareTo(right) <= 0;
        }
    }
}