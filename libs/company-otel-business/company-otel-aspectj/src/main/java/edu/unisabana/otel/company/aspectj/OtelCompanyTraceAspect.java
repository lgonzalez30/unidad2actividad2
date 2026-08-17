package edu.unisabana.otel.company.aspectj;

import edu.unisabana.otel.company.core.BusinessSpanRunner;
import edu.unisabana.otel.company.core.OtelCompanyTrace;
import java.lang.reflect.Method;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

@Aspect
public final class OtelCompanyTraceAspect {
    private final BusinessSpanRunner runner;

    public OtelCompanyTraceAspect() {
        this(BusinessSpanRunner.createDefault());
    }

    OtelCompanyTraceAspect(BusinessSpanRunner runner) {
        this.runner = runner;
    }

    @Around("execution(@edu.unisabana.otel.company.core.OtelCompanyTrace * *(..)) && @annotation(annotation)")
    public Object trace(ProceedingJoinPoint joinPoint, OtelCompanyTrace annotation) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String[] parameterNames = signature.getParameterNames();
        return runner.run(annotation, method, joinPoint.getArgs(), parameterNames, joinPoint::proceed);
    }
}
