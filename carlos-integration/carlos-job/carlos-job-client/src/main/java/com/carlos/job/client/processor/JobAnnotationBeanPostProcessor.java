package com.carlos.job.client.processor;

import cn.hutool.core.util.StrUtil;
import com.carlos.core.exception.ComponentException;
import com.carlos.job.client.annotation.AutoRegisterJob;
import com.carlos.job.client.annotation.ScheduleJob;
import com.carlos.job.client.config.JobClientProperties;
import com.carlos.job.client.handler.JobHandler;
import com.carlos.job.client.handler.JobResult;
import com.carlos.job.client.handler.MethodJobHandler;
import com.carlos.job.client.register.JobAutoRegisterPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * <p>
 * 注解任务处理器登记：扫描所有 Bean 上 {@link ScheduleJob} 标注的方法并登记为 {@link JobHandler}
 * </p>
 *
 * <p>只写自己的容器内映射，不直接改 {@code JobHandlerRegistry} —— BPP 的执行期早于普通单例，
 * 在 BPP 里取 {@code JobHandlerRegistry} 会把它依赖的所有 {@code JobHandler} Bean 提前创建，
 * 可能触发 {@code BeanCurrentlyInCreationException}。合并动作由
 * {@code JobHandlerRegistry.afterSingletonsInstantiated()} 完成。</p>
 *
 * <p>非法的注解声明默认<b>启动即失败</b>（{@code carlos.job.client.fail-fast-on-invalid=false} 可降级为
 * 记 ERROR 并跳过）—— 注解是开发者的显式意图，静默跳过只会在凌晨表现为任务处理器不存在。</p>
 *
 * @author Carlos
 * @date 2026年9月23日
 */
@Slf4j
public class JobAnnotationBeanPostProcessor implements BeanPostProcessor, BeanFactoryAware, EnvironmentAware {

    private static final Pattern HANDLER_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{1,100}$");

    private static final Pattern JOB_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,100}$");

    private static final Pattern JOB_GROUP_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,64}$");

    private static final Pattern SERVICE_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9-]{1,64}$");

    /**
     * 框架/JDK 类不参与扫描（它们不可能带本框架注解，纯属性能优化）
     */
    private static final String[] SKIP_CLASS_PREFIXES = {
        "java.", "javax.", "jakarta.", "jdk.", "sun.", "org.springframework.", "org.apache.",
        "org.mybatis.", "com.fasterxml.", "com.zaxxer.", "io.swagger."
    };

    /**
     * 处理器名称 -> 处理器（由 {@code JobHandlerRegistry} 合并）
     */
    private final Map<String, JobHandler> annotatedHandlers = new ConcurrentHashMap<>();

    /**
     * 处理器名称 -> 自动注册计划
     */
    private final Map<String, JobAutoRegisterPlan> autoRegisterPlans = new ConcurrentHashMap<>();

    /**
     * 处理器名称 -> 目标方法全名（冲突提示用，同时保证"先注册者"语义与 Map 迭代顺序无关）
     */
    private final Map<String, String> handlerLocations = new ConcurrentHashMap<>();

    /**
     * 已确认不含注解的类（避免同类的多个 Bean 重复做方法扫描）
     */
    private final Set<Class<?>> nonAnnotatedClasses = ConcurrentHashMap.newKeySet();

    private BeanFactory beanFactory;

    private Environment environment;

    private boolean failFast = true;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
        this.failFast = environment.getProperty(JobClientProperties.PREFIX + ".fail-fast-on-invalid",
            Boolean.class, Boolean.TRUE);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        Class<?> userClass = ClassUtils.getUserClass(bean.getClass());
        if (isSkipped(userClass)) {
            return bean;
        }
        Map<Method, ScheduleJob> methods = MethodIntrospector.selectMethods(userClass,
            (MethodIntrospector.MetadataLookup<ScheduleJob>) method ->
                AnnotatedElementUtils.findMergedAnnotation(method, ScheduleJob.class));
        if (methods.isEmpty()) {
            nonAnnotatedClasses.add(userClass);
            return bean;
        }
        methods.forEach((method, scheduleJob) -> handleMethod(beanName, userClass, method, scheduleJob));
        return bean;
    }

    /**
     * 已登记的注解处理器（由 {@code JobHandlerRegistry} 合并后清空由此处持有）
     *
     * @return 处理器名称 -> 处理器
     */
    public Map<String, JobHandler> annotatedHandlers() {
        return annotatedHandlers;
    }

    /**
     * 自动注册计划
     *
     * @return 处理器名称 -> 注册计划
     */
    public Map<String, JobAutoRegisterPlan> autoRegisterPlans() {
        return autoRegisterPlans;
    }

    private void handleMethod(String beanName, Class<?> userClass, Method method, ScheduleJob scheduleJob) {
        String location = userClass.getName() + "#" + method.getName();
        String handlerName = StrUtil.blankToDefault(scheduleJob.value(), method.getName());
        if (!HANDLER_NAME_PATTERN.matcher(handlerName).matches()) {
            reject("任务处理器名称不合法: name=" + handlerName + ", " + location + "（仅允许字母、数字、下划线，长度 1-100）");
            return;
        }
        if (!isSupportedSignature(method)) {
            reject("方法签名不支持: " + location + "，仅允许 (void|JobResult) m() 或 (void|JobResult) m(String params)");
            return;
        }
        String occupiedBy = handlerLocations.putIfAbsent(handlerName, location);
        if (occupiedBy != null) {
            // 同一个方法被扫描两次是幂等的：原型作用域 Bean 每个实例都会走一次 BPP，
            // @RefreshScope 的代理与目标类也会各走一次，此时 location 完全相同，直接忽略。
            // 只有"位置不同却同名"才是真冲突。
            if (location.equals(occupiedBy)) {
                return;
            }
            reject("任务处理器名称冲突: name=" + handlerName + "，已被 " + occupiedBy + " 占用，跳过 " + location);
            return;
        }
        boolean withParams = method.getParameterCount() == 1;
        annotatedHandlers.put(handlerName, new MethodJobHandler(beanFactory, beanName, handlerName, method, withParams));
        log.info("注册注解任务处理器: name={}, target={}, bean={}", handlerName, location, beanName);
        AutoRegisterJob autoRegister = AnnotatedElementUtils.findMergedAnnotation(method, AutoRegisterJob.class);
        if (autoRegister == null) {
            return;
        }
        JobAutoRegisterPlan plan = buildPlan(handlerName, beanName, location, autoRegister);
        if (plan != null) {
            autoRegisterPlans.put(handlerName, plan);
            log.info("登记任务自动注册计划: name={}, jobName={}, serviceName={}, scheduleType={}, scheduleConf={}",
                handlerName, plan.getJobName(), plan.getServiceName(), plan.getScheduleType(), plan.getScheduleConf());
        }
    }

    /**
     * 校验方法签名：public 实例方法，返回 void 或 JobResult，参数为空或单个 String
     */
    private boolean isSupportedSignature(Method method) {
        int modifiers = method.getModifiers();
        if (Modifier.isStatic(modifiers) || Modifier.isAbstract(modifiers) || !Modifier.isPublic(modifiers)) {
            return false;
        }
        if (method.getParameterCount() > 1) {
            return false;
        }
        if (method.getParameterCount() == 1 && !String.class.equals(method.getParameterTypes()[0])) {
            return false;
        }
        Class<?> returnType = method.getReturnType();
        return void.class.equals(returnType) || JobResult.class.equals(returnType);
    }

    private JobAutoRegisterPlan buildPlan(String handlerName, String beanName, String location,
                                          AutoRegisterJob annotation) {
        String scheduleConf = StrUtil.blankToDefault(annotation.scheduleConf(), annotation.cron());
        if (StrUtil.isBlank(scheduleConf)) {
            reject("注解任务未声明调度配置: " + location + "，@AutoRegisterJob 必须指定 cron 或 scheduleConf");
            return null;
        }
        if (scheduleConf.length() > 120) {
            reject("调度配置长度不能超过 120: " + location);
            return null;
        }
        if (annotation.scheduleType() < 1 || annotation.scheduleType() > 3) {
            reject("调度类型取值 1-CRON 2-固定间隔 3-一次性: " + location);
            return null;
        }
        String jobName = StrUtil.blankToDefault(annotation.jobName(), handlerName);
        if (!JOB_NAME_PATTERN.matcher(jobName).matches()) {
            reject("任务名称不合法: name=" + jobName + ", " + location + "（仅允许字母、数字、下划线、中划线，长度 1-100）");
            return null;
        }
        if (StrUtil.isNotBlank(annotation.jobGroup()) && !JOB_GROUP_PATTERN.matcher(annotation.jobGroup()).matches()) {
            reject("任务分组不合法: group=" + annotation.jobGroup() + ", " + location);
            return null;
        }
        String serviceName = StrUtil.blankToDefault(annotation.serviceName(),
            environment.getProperty("spring.application.name", ""));
        if (StrUtil.isBlank(serviceName) || !SERVICE_NAME_PATTERN.matcher(serviceName).matches()) {
            reject("目标服务名不合法: serviceName=" + serviceName + ", " + location
                + "（请在注解中显式指定 serviceName，或确认 spring.application.name 已配置）");
            return null;
        }
        if (!isWithinLength(location, "params", annotation.params(), 4000)
            || !isWithinLength(location, "description", annotation.description(), 250)
            || !isWithinLength(location, "author", annotation.author(), 64)
            || !isWithinLength(location, "alarmReceivers", annotation.alarmReceivers(), 500)) {
            return null;
        }
        if (StrUtil.isNotBlank(annotation.status())
            && !"NORMAL".equals(annotation.status()) && !"PAUSED".equals(annotation.status())) {
            reject("任务状态仅允许 NORMAL 或 PAUSED: " + location);
            return null;
        }
        if (!isWithinRange(location, "misfireStrategy", annotation.misfireStrategy(), -1, 3)
            || !isWithinRange(location, "concurrentMode", annotation.concurrentMode(), 0, 2)
            || !isWithinRange(location, "timeoutSeconds", annotation.timeoutSeconds(), -1, 86400)
            || !isWithinRange(location, "failRetryCount", annotation.failRetryCount(), -1, 10)) {
            return null;
        }
        return new JobAutoRegisterPlan()
            .setHandlerName(handlerName)
            .setBeanName(beanName)
            .setMethodLocation(location)
            .setJobName(jobName)
            .setJobGroup(annotation.jobGroup())
            .setServiceName(serviceName)
            .setScheduleType(annotation.scheduleType())
            .setScheduleConf(scheduleConf)
            .setParams(annotation.params())
            .setDescription(annotation.description())
            .setMisfireStrategy(annotation.misfireStrategy())
            .setConcurrentMode(annotation.concurrentMode())
            .setTimeoutSeconds(annotation.timeoutSeconds())
            .setFailRetryCount(annotation.failRetryCount())
            .setStatus(annotation.status())
            .setAuthor(annotation.author())
            .setAlarmReceivers(annotation.alarmReceivers());
    }

    private boolean isWithinLength(String location, String field, String value, int max) {
        if (value != null && value.length() > max) {
            reject(field + " 长度不能超过 " + max + ": " + location);
            return false;
        }
        return true;
    }

    private boolean isWithinRange(String location, String field, int value, int min, int max) {
        if (value < min || value > max) {
            reject(field + " 取值 " + min + "-" + max + ": " + location);
            return false;
        }
        return true;
    }

    private boolean isSkipped(Class<?> userClass) {
        if (nonAnnotatedClasses.contains(userClass)) {
            return true;
        }
        // JDK 动态代理类要留着：注解可能声明在接口方法上
        if (Proxy.isProxyClass(userClass)) {
            return false;
        }
        if (userClass.isInterface() || userClass.isAnnotation() || userClass.isSynthetic()
            || userClass.isPrimitive() || userClass.isArray()) {
            return true;
        }
        String className = userClass.getName();
        for (String prefix : SKIP_CLASS_PREFIXES) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 注解声明非法：默认抛出终止启动，降级模式下记 ERROR 并跳过
     */
    private void reject(String message) {
        if (failFast) {
            throw new ComponentException("carlos-job 注解配置错误: " + message);
        }
        log.error("carlos-job 注解配置错误（已跳过）: {}", message);
    }

}
