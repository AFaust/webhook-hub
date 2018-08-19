package org.orderofthebee.tools.webhook.hub;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.BufferedResponseHandler;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This handler wrapper implementation wraps {@link HttpServletRequest HTTP servlet requests} in order to allow handlers to access the
 * content via {@link HttpServletRequest#getInputStream() the request input stream} multiple times if necessary (i.e. if handlers need to
 * determine whether they should handle the request based on the body contents). Jetty by default does not ship a similar handler wrapper,
 * only one for {@link BufferedResponseHandler buffering the response}.
 *
 * @author Axel Faust
 */
public class RequestBufferingHandler extends HandlerWrapper
{

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestBufferingHandler.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public void handle(final String target, final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response)
            throws IOException, ServletException
    {
        final BufferedttpServletRequestWrapper wrappedRequest = new BufferedttpServletRequestWrapper(request);
        try
        {
            super.handle(target, baseRequest, wrappedRequest, response);
        }
        catch (final Throwable ex)
        {
            LOGGER.error("Caught error from wrapped handler", ex);
            throw ex;
        }
        finally
        {
            wrappedRequest.cleanUp();
        }
    }

}
