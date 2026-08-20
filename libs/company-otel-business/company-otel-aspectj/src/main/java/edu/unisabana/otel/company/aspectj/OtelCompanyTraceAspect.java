package edu.unisabana.otel.company.aspectj;

import edu.unisabana.otel.company.core.BusinessSpanRunner;
import edu.unisabana.otel.company.core.CompanyOtelConfig;
import edu.unisabana.otel.company.core.OtelCompanyTrace;
import edu.unisabana.otel.company.core.SimpleAttributeResolver;
import edu.unisabana.otel.company.jbossmdc.OtelJbossMdc;
import io.opentelemetry.api.GlobalOpenTelemetry;
import java.lang.reflect.Method;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

@Aspect
public final class OtelCompanyTraceAspect {
    private final BusinessSpanRunner runner;

    public OtelCompanyTraceAspect() {
        this(new BusinessSpanRunner(
                GlobalOpenTelemetry.getTracer(
                        BusinessSpanRunner.INSTRUMENTATION_SCOPE,
                        BusinessSpanRunner.LIBRARY_VERSION),
                CompanyOtelConfig.loadDefault(),
                new SimpleAttributeResolver(),
                OtelJbossMdc.BINDER));
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

    @Around("execution(* *(..)) && ("
            + "@annotation(jakarta.ws.rs.GET) || "
            + "@annotation(jakarta.ws.rs.POST) || "
            + "@annotation(jakarta.ws.rs.PUT) || "
            + "@annotation(jakarta.ws.rs.DELETE) || "
            + "@annotation(jakarta.ws.rs.PATCH) || "
            + "@annotation(jakarta.ws.rs.HEAD) || "
            + "@annotation(jakarta.ws.rs.OPTIONS))")
    public Object bindMdcForJaxRsResource(ProceedingJoinPoint joinPoint) throws Throwable {
        try (var ignored = OtelJbossMdc.putCurrentSpan()) {
            return joinPoint.proceed();
        }
    }
}
