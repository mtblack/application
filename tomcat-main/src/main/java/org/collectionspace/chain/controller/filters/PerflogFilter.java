package org.collectionspace.chain.controller.filters;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PerflogFilter implements Filter {
	private static final Logger perflog=LoggerFactory.getLogger("org.collectionspace.perflog");

	public void destroy() {
		// Empty method
	}
	
	public void doFilter(ServletRequest req, ServletResponse resp,
			FilterChain filterChain) throws IOException, ServletException {
		if(!perflog.isDebugEnabled()) {
			// Don't waste any time if logging disabled
			filterChain.doFilter(req, resp);
		} else {
			HttpServletRequest hreq = (HttpServletRequest) req;		
			// TODO might want to add more context e.g. sessionid etc
			String contextString = "HttpServletRequest@" + Integer.toHexString(hreq.hashCode()) + ",";
			String queryString = hreq.getQueryString();
			perflog.debug("ui,app," + contextString
					+ hreq.getMethod() + " " + hreq.getPathInfo()
					+ (queryString!=null ? "?" + queryString : "")
					+ " " + hreq.getContentLength());
			filterChain.doFilter(req,resp);
			perflog.debug("app,ui," + contextString + "FilterChain.doFilter done");
		}
	}

	public void init(FilterConfig config) throws ServletException {
		perflog.debug("Performance Logging Enabled. To disable, see tomcat-main/src/main/resources/log4j.properties\n"
				+"Date,Relative time (ms),Thread,From layer,To layer,Context,Perflog event");
	}
}
