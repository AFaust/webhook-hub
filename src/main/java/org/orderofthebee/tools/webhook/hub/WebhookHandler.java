package org.orderofthebee.tools.webhook.hub;

import java.io.IOException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Axel Faust
 */
public class WebhookHandler extends AbstractHandler
{

    private static final Logger LOGGER = LoggerFactory.getLogger(WebhookHandler.class);

    private final Engine engine = Engine.newBuilder().useSystemProperties(false).build();

    public WebhookHandler()
    {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handle(final String target, final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response)
            throws IOException, ServletException
    {
        LOGGER.info("Running {} request on {} through handler", request.getMethod(), request.getRequestURI());
        try (final Context context = Context.newBuilder("js").engine(this.engine).build())
        {
            // TODO Load configured handler script, supporting internal + external lookup
            final URL scriptResource = this.getClass().getClassLoader().getResource("testScript.js");
            if (scriptResource == null)
            {
                throw new IllegalStateException("Failed to find script to execute");
            }
            final Source source = Source.newBuilder("js", scriptResource).build();
            context.eval(source);

            if (!baseRequest.isHandled())
            {
                LOGGER.warn("Request {} has not been handled by script");
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Request handler failed to act");
                baseRequest.setHandled(true);
            }
        }
        catch (final PolyglotException pEx)
        {
            LOGGER.error("Failed to run script", pEx);
            if (!baseRequest.isHandled())
            {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, pEx.getMessage());
                baseRequest.setHandled(true);
            }
            else
            {
                LOGGER.warn("Request {} has already been handled - not sending error response", request);
            }
        }
    }
}
