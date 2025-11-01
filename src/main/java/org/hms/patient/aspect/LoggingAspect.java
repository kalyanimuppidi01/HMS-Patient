package org.hms.patient.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.*;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple logging aspect to mask PII for controller inputs and outputs.
 * - Masks emails to "x***@domain"
 * - Masks phones to "x*****yz"
 * - Adds a correlationId if available in request attributes (falls back to "-")
 *
 * NOTE: This performs naive string masking. For deep/structured masking prefer
 * JSON serialization + object traversal.
 */
@Aspect
@Component
public class LoggingAspect {
    private static final Logger log = LoggerFactory.getLogger("PII");
    private static final Pattern EMAIL_RE = Pattern.compile("([A-Za-z0-9._%+-]+)@([A-Za-z0-9.-]+\\.[A-Za-z]{2,6})");
    private static final Pattern PHONE_RE = Pattern.compile("\\+?\\d[0-9\\-\\s()]{4,}\\d");

    private String correlationIdOrDash() {
        try {
            RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                Object cid = attrs.getAttribute("correlationId", RequestAttributes.SCOPE_REQUEST);
                if (cid != null) return String.valueOf(cid);
            }
        } catch (Exception ignored) {}
        return "-";
    }

    private String maskEmailMatch(Matcher m) {
        // group(1) local-part, group(2) domain
        String local = m.group(1);
        String domain = m.group(2);
        if (local.length() <= 1) {
            return "***@" + domain;
        }
        // keep first char, mask rest
        return local.charAt(0) + "***@" + domain;
    }

    private String maskPhoneMatch(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() <= 4) return "***" + digits;
        String first = digits.substring(0, 1);
        String last = digits.substring(Math.max(0, digits.length() - 2));
        return first + "*****" + last;
    }

    private String maskString(String input) {
        if (input == null) return null;
        String s = input;

        // Mask emails
        Matcher m = EMAIL_RE.matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, maskEmailMatch(m));
        }
        m.appendTail(sb);
        s = sb.toString();

        // Mask phones (simple)
        Matcher p = PHONE_RE.matcher(s);
        sb = new StringBuffer();
        while (p.find()) {
            String ph = p.group();
            p.appendReplacement(sb, maskPhoneMatch(ph));
        }
        p.appendTail(sb);
        s = sb.toString();

        return s;
    }

    @AfterReturning(pointcut = "within(org.hms.patient.controller..*)", returning = "ret")
    public void logAfter(JoinPoint jp, Object ret) {
        try {
            String signature = jp.getSignature().toShortString();
            String corr = correlationIdOrDash();
            String out = String.valueOf(ret);
            out = maskString(out);

            // include timestamp and correlation id
            String time = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            log.info("[{}] [corr:{}] [RESPONSE] {} -> {}", time, corr, signature, out);
        } catch (Exception e) {
            // do not fail startup on logging errors
            log.warn("LoggingAspect.logAfter failed: {}", e.getMessage());
        }
    }

    @Before("within(org.hms.patient.controller..*) && args(body,..)")
    public void beforeController(JoinPoint jp, Object body) {
        try {
            String signature = jp.getSignature().toShortString();
            String corr = correlationIdOrDash();
            String bodyStr = String.valueOf(body);
            bodyStr = maskString(bodyStr);
            String time = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            log.info("[{}] [corr:{}] [REQUEST] {} -> {}", time, corr, signature, bodyStr);
        } catch (Exception e) {
            log.warn("LoggingAspect.beforeController failed: {}", e.getMessage());
        }
    }
}
