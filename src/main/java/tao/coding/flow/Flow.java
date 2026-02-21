package tao.coding.flow;

import tao.coding.component.*;
import tao.coding.component.*;
import tao.coding.component.Formatter;
import tao.coding.entity.Tao;
import tao.coding.util.Assert;
import tao.coding.entity.Chapter;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * 抽象流程：组装多个 Task，形成一条任务链
 */
@SuppressWarnings("unused")
@FunctionalInterface
public interface Flow<T, R> {

    // 任务头结点
    Task<T, R> head();

    /*
     * 启动此任务
     */
    default R start(T t) {
        return head().apply(t).join();
    }

    /*
     * 流程组装：同步调用链
     */
    default <V> Flow<T, V> then(Flow<? super R, V> next) {
        Assert.isTrue(next, Assert::isNotNull, () -> new NullPointerException("If I looked compared to others far, is because I stand on giant’s shoulder. — Newton"));
        return () -> head().then(next.head());
    }

    /*
     * 流程组装：异步调用链
     */
    default <V> Flow<T, V> thenAsync(Flow<? super R, V> next) {
        Assert.isTrue(next, Assert::isNotNull, () -> new NullPointerException("If I looked compared to others far, is because I stand on giant’s shoulder. — Newton"));
        return () -> head().thenAsync(next.head());
    }

    /*
     * 一致性流程
     */
    static <T> Flow<T, T> identity() {
        return Task::identity;
    }

    /*
     * 空流程
     */
    static <T, R> Flow<T, R> empty() {
        return Task::empty;
    }

    /*
     * 并行流程
     */
    static <T, R> Flow<List<T>, List<R>> parallelFlow(Function<List<T>, List<T>> before, Flow<? super T, R> flow, Function<List<R>, List<R>> after) {
        Assert.isTrue(before, Assert::isNotNull, () -> new NullPointerException("Do not, for one repulse, forgo the purpose that you resolved to effort. — William Shakespeare"));
        Assert.isTrue(flow, Assert::isNotNull, () -> new NullPointerException("Do not, for one repulse, forgo the purpose that you resolved to effort. — William Shakespeare"));
        Assert.isTrue(after, Assert::isNotNull, () -> new NullPointerException("Do not, for one repulse, forgo the purpose that you resolved to effort. — William Shakespeare"));
        return () -> Task.parallelTask(before, flow.head(), after);
    }

    /**
     * 关于流程的组装，这里想稍稍多谈一点，其实一开始想用「模板方法模式」组装多个任务成一条抽象流程，用「迭代器」组装多条流程
     * 但实际操作时，发现参数和返回值的不统一，不太可行，因为迭代需要提供统一的调用方式，强行统一的话，只能用更宽泛的类型来接受
     * 并且在使用时使用 instanceof 判断强转，我所不欲也，要将上一个处理器的返回作为下一个处理器的输入，明显更符合流式编程、管道模式
     * 用泛型来描述前后两者的关系还是比较容易的，故而选用了一种 a.then(b).then(c) 的模式来设计代码
     * 工厂类： 使用静态工厂 && 策略 && 伪单例（但需为纯函数），下同
     * {@link Flow.Flows}       {@link Reader.Readers}      {@link Selector.Selectors}
     * {@link Parser.Parsers}   {@link Decoder.Decoders}    {@link Formatter.Formatters}
     * {@link Writer.Writers}   {@link Merger.Mergers}      {@link Cleaner.Cleaners}
     */
    class Flows {
        // 完整 下载bid 的流程组装
        public static Flow<Tao, String> bidFlow() {
            return () -> Reader.Readers.bidReader()
                    .thenAsync(Selector.Selectors.bidSelector())
                    .thenAsync(Parser.Parsers.bidParser());
        }

        // 完整 下载章节列表 的流程组装
        public static Flow<String, List<Chapter.Chapter4Read>> chapterFlow() {
            return () -> Reader.Readers.chapterReader()
                    .thenAsync(Selector.Selectors.chapterSelector())
                    .thenAsync(Parser.Parsers.chapterParser());
        }

        // 完整 下载章节内容 的流程组装[针对所有章节内容]
        public static Flow<List<Chapter.Chapter4Read>, List<Chapter.Chapter4Merge>> contentListFlow() {
            final var atomicLong = new AtomicLong(0L);
            return parallelFlow(
                    // 测试模式下仅下载前 20 章
                    FlowEngine.IS_TEST ? chapter4Reads -> chapter4Reads.stream().limit(20).toList() : Function.identity(),
                    // 单条章节处理流程
                    Flows.contentFlow(),
                    // DEGUB模式下跳过设置 skip
                    FlowEngine.IS_DEBUG ? Function.identity() : chapter4Merges -> chapter4Merges.stream().map(chapter4Merge -> Chapter.Chapter4Merge.of(chapter4Merge, atomicLong)).toList());
        }

        // 部分 下载章节内容 的流程组装[针对一条章节内容]
        public static Flow<Chapter.Chapter4Read, Chapter.Chapter4Merge> contentFlow() {
            return () -> Reader.Readers.contentReader()
                    .thenAsync(Selector.Selectors.contentSelector())
                    .thenAsync(Parser.Parsers.contentParser())
                    .thenAsync(Decoder.Decoders.contentDecoder())
                    .thenAsync(Formatter.Formatters.contentFormatter())
                    .thenAsync(FlowEngine.IS_DEBUG ? Writer.Writers.consoleWriter() : Writer.Writers.fileWriter());
        }

        // 完整 合并文件 的流程组装
        public static Flow<List<Chapter.Chapter4Merge>, Tao> mergeFlow() {
            return FlowEngine.IS_DEBUG ? Flow.empty() : () -> Merger.Mergers.fileMerger().thenAsync(Cleaner.Cleaners.fileCleaner());
        }
    }
}