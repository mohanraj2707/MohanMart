package com.mohan.mohanmart.filter;

/**
 * Request-ID Filter binding a unique UUID per request to SLF4J MDC ("requestId")
 * and setting the X-Request-Id response header (§13).
 * Delegates to {@link LoggingFilter} which is registered across "/*".
 */
public class RequestIdFilter extends LoggingFilter {
}
