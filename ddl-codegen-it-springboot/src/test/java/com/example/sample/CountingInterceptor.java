package com.example.sample;

import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

/**
 * MyBatis SQL 计数拦截器（IT 观测用，20260906-13）：统计各 statement 执行次数，
 * 用于断言 spring-cache 命中/失效（缓存命中不落库 = 计数不增）。
 */
@Intercepts({
    @Signature(type = Executor.class, method = "query",
            args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
    @Signature(type = Executor.class, method = "update",
            args = {MappedStatement.class, Object.class})})
public final class CountingInterceptor implements Interceptor {

    private final Map<String, Integer> counts = new HashMap<>();

    /** 语句 id 未段（如 findByName）→ 出现次数。 */
    public int count(String methodSimpleName) {
        Integer value = counts.get(methodSimpleName);
        return value == null ? 0 : value;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        if (args[0] instanceof MappedStatement) {
            String id = ((MappedStatement)args[0]).getId();
            String simple = id.substring(id.lastIndexOf('.') + 1);
            counts.merge(simple, 1, Integer::sum);
        }
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    public void reset() {
        counts.clear();
    }

    /** 查询类（含 select）语句总数。 */
    public int selectCount() {
        int total = 0;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getKey().startsWith("find")) {
                total += entry.getValue();
            }
        }
        return total;
    }

    @Override
    public void setProperties(java.util.Properties properties) {
        // no-op
    }

}
