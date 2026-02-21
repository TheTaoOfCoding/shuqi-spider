package tao.coding.flow;

import lombok.extern.slf4j.Slf4j;
import tao.coding.util.Assert;

import java.net.http.HttpClient;
import java.util.Arrays;
import java.util.Optional;

import static tao.coding.entity.Tao.TAO;
import static tao.coding.util.ScopedExecutor.*;

@Slf4j
public class FlowEngine implements AutoCloseable {
    // 是否启用调试模式
    public static final boolean IS_DEBUG = false;
    // 是否启用测试模式
    public static final boolean IS_TEST = true;
    // 是否启用本地解密
    public static final boolean USE_NATIVE = true;
    // 删除文件标识
    public static final boolean NEED_DELETE = true;
    // 默认文件夹路径 e.g. D:/斗破苍穹
    public static final String FOLDER_FORMATTER = "D:/%s";


    // 每个线程默认处理的章节数量
    public static final Integer DEFAULT_CAPACITY = 5;
    // http客户端（使用原生虚拟线程池）
    public static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().executor(delegate()).build();
    // 单例模式：静态实例对象，使用 volatile 修饰，防止指令重排导致的 NPE 问题
    private static volatile FlowEngine DEFAULT_FLOW_ENGINE;

    private FlowEngine() {
        Assert.isTrue(DEFAULT_FLOW_ENGINE, Assert::isNull, () -> new IllegalStateException("Don’t judge each day by the harvest you reap but by the seeds that you plant. — Robert Louis Stevenson"));
    }

    // 花式 dcl 单例
    public static FlowEngine getDefaultFlowEngine() {
        return Optional.ofNullable(DEFAULT_FLOW_ENGINE).orElseGet(() -> {
            synchronized (FlowEngine.class) {
                DEFAULT_FLOW_ENGINE = Optional.ofNullable(DEFAULT_FLOW_ENGINE).orElseGet(FlowEngine::new);
                return DEFAULT_FLOW_ENGINE;
            }
        });
    }

    // 启动流程引擎，设置书籍名称的作用域变量
    public void start(String... bookNames) {
        Arrays.stream(bookNames)
                .parallel()
                .forEach(bookName -> ScopedValue.where(ScopedExecutors.KEY, bookName).run(this::start0));
    }

    // 组装串联流程
    private void start0() {
        try {
            log.info("""
                    \u001B[93m敕令：「
                                                                  天地自然，秽气分散！
                                                                  洞中玄虚，晃朗太元！
                                                                  焚香启告，迳达九天！
                                                                  今开法坛：
                                                                  一请，三清道祖垂慈！
                                                                  二请，四御天尊降鉴！
                                                                  三请，雷部将帅听宣！
                                                                  四请，五营神兵列阵！
                                                                  坛场肃靖，万神拱卫！
                                                                  急急如律令！！！
                                                                」\u001B[0m
                    """);
            // 无名天地之始
            var tao = TAO;

            // The Tao gives birth to the One.
            var bidFlow = Flow.Flows.bidFlow();
            log.info("\u001B[93m敕令：「一笔天地动，风雷随法涌。」\u001B[0m");

            // The One gives birth to the Two.
            var chapterFlow = Flow.Flows.chapterFlow();
            log.info("\u001B[93m敕令：「二笔祖师剑，神威降尘寰。」\u001B[0m");

            // The Two gives birth to the Three.
            var contentListFlow = Flow.Flows.contentListFlow();
            log.info("\u001B[93m敕令：「三笔凶神灭，煞气皆溃裂。」\u001B[0m");

            // The Three gives birth to the ten thousand things.
            var mergeFlow = Flow.Flows.mergeFlow();
            log.info("\u001B[93m敕令：「四笔煞无形，乾坤朗朗清。」\u001B[0m");

            // 起始亦是终，始于道，亦终于道
            tao = bidFlow.thenAsync(chapterFlow)
                    .thenAsync(contentListFlow)
                    .thenAsync(mergeFlow)
                    .start(tao);
            log.info("\u001B[93m敕令：「笔收星芒，符镇八荒，朱砂既凝，邪魔永丧。」\u001B[0m");
        } catch (Exception e) {
            log.error("\u001B[91m敕令：「心念不纯，符窍无光！僭请神明，触怒天罡！伏请三清垂慈，赦宥愚诚！」\u001B[0m");
            throw e;
        }
    }

    public void end() {
        log.info("\u001B[92m敕令：「香云奉送，祖师归坛；神兵返驾，各归玄庭！弟子稽首，再沐恩光！散坛！」\u001B[0m");
        HTTP_CLIENT.close();
        shutdown();
    }

    @Override
    public void close() {
        end();
    }
}