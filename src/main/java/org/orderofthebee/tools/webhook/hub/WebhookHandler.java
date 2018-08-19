package org.orderofthebee.tools.webhook.hub;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.orderofthebee.tools.webhook.hub.SecretConfig.Mode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.kevinsawicki.http.HttpRequest;

/**
 * @author Axel Faust
 */
public class WebhookHandler extends AbstractHandler
{

    private static final Logger LOGGER = LoggerFactory.getLogger(WebhookHandler.class);

    private static final List<String> JS_TOP_LEVEL_MEMBERS_TO_REMOVE = Collections
            .unmodifiableList(Arrays.asList("load", "loadWithNewGlobal", "readLine", "readFully", "read", "readbuffer", "readline", "print",
                    "printErr", "console", "exit", "quit", "Polyglot", "Interop", "Java", "Graal"));

    private static final List<String> BODY_LESS_REQUEST_METHODS = Collections.unmodifiableList(Arrays.asList("get", "delete"));

    private static final List<String> BODY_FULL_REQUEST_METHODS = Collections.unmodifiableList(Arrays.asList("put", "post"));

    private final WebhookConfig webhookConfig;

    private final Pattern urlMatchPattern;

    private final List<String> matchVariableNames = new ArrayList<>();

    private final Engine engine = Engine.newBuilder().useSystemProperties(false).build();

    public WebhookHandler(final WebhookConfig webhookConfig)
    {
        if (!webhookConfig.isMandatoryConfigComplete())
        {
            LOGGER.error("Incomplete webhook configuration {}", webhookConfig);
            throw new IllegalStateException("Webhook configuration is incomplete");
        }

        this.webhookConfig = webhookConfig;
        this.urlMatchPattern = this.buildUrlMatchPattern(webhookConfig, this.matchVariableNames);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handle(final String target, final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response)
            throws IOException, ServletException
    {
        final Optional<Map<String, Object>> urlVariablesOpt = this.matchRequestUrlAndExtractVariables(request);

        urlVariablesOpt.ifPresent(urlVariables -> {
            try
            {
                if (!this.verifySecret(request, this.webhookConfig.getInbound()))
                {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Secret missing or invalid");
                    baseRequest.setHandled(true);
                }
                else
                {
                    this.executeHandlerScript(baseRequest, request, response, urlVariables);
                }
            }
            catch (final IOException ioEx)
            {
                LOGGER.error("IO error handling request {}", request, ioEx);
            }
        });
        // else not matching URL pattern and no need to be handled
    }

    protected static byte[] generateDigest(final InputStream is, final String digestAlgorithm) throws NoSuchAlgorithmException, IOException
    {
        final MessageDigest digest = MessageDigest.getInstance(digestAlgorithm);

        final byte[] buf = new byte[10240];
        int bytesRead;
        while ((bytesRead = is.read(buf)) != -1)
        {
            digest.update(buf, 0, bytesRead);
        }

        final byte[] calculatedDigest = digest.digest();
        return calculatedDigest;
    }

    protected static byte[] generateSaltedDigest(final InputStream is, final String sharedSecret, final String digestAlgorithm)
            throws NoSuchAlgorithmException, InvalidKeyException, IOException
    {
        final SecretKeySpec signingKey = new SecretKeySpec(sharedSecret.getBytes(StandardCharsets.UTF_8), digestAlgorithm);
        final Mac mac = Mac.getInstance(digestAlgorithm);
        mac.init(signingKey);

        final byte[] buf = new byte[10240];
        int bytesRead;
        while ((bytesRead = is.read(buf)) != -1)
        {
            mac.update(buf, 0, bytesRead);
        }

        final byte[] calculatedDigest = mac.doFinal();
        return calculatedDigest;
    }

    protected static String toHex(final byte[] calculatedDigestBytes)
    {
        final StringBuilder hexBuilder = new StringBuilder(calculatedDigestBytes.length * 2);
        for (final byte digestByte : calculatedDigestBytes)
        {
            hexBuilder.append(Integer.toHexString((digestByte & 0xF0) >> 4));
            hexBuilder.append(Integer.toHexString(digestByte & 0x0F));
        }
        final String calculcatedDigestHex = hexBuilder.toString();
        return calculcatedDigestHex;
    }

    protected void executeHandlerScript(final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response,
            final Map<String, Object> urlVariables) throws IOException
    {
        LOGGER.info("Running {} request on {} through handler", request.getMethod(), request.getRequestURI());

        try (final Context context = Context.newBuilder("js").engine(this.engine).build())
        {
            this.prepareScriptBindings(context, baseRequest, request, response, urlVariables);

            final String handlerScript = this.webhookConfig.getHandlerScript();
            // priority to internalised scripts, fallback to file system scripts
            URL scriptResource = this.getClass().getClassLoader().getResource(handlerScript);
            if (scriptResource == null)
            {
                final Path path = Paths.get(handlerScript);
                if (Files.exists(path) && Files.isRegularFile(path))
                {
                    scriptResource = path.toUri().toURL();
                }
            }

            if (scriptResource == null)
            {
                LOGGER.error("Failed to lookup handler script {}", handlerScript);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server configuration error");
                baseRequest.setHandled(true);
            }
            else
            {
                final Source source = Source.newBuilder("js", scriptResource).build();
                context.eval(source);
            }

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

    protected Pattern buildUrlMatchPattern(final WebhookConfig webhookConfig, final List<String> matchVariableNames)
    {
        final String urlPattern = webhookConfig.getInbound().getUrlPattern();
        final StringBuilder matchPatternBuilder = new StringBuilder(urlPattern.length());

        int previousEndIdx = 0;
        int idxOfOpeningCurly = urlPattern.indexOf('{');
        while (idxOfOpeningCurly != -1)
        {
            final int idxOfNextSlash = urlPattern.indexOf('/', idxOfOpeningCurly);
            final int idxOfNextClosingCurly = urlPattern.indexOf('}', idxOfOpeningCurly);

            if (idxOfNextClosingCurly == -1)
            {
                throw new IllegalStateException("Unclosed variable placeholder in base path pattern " + urlPattern);
            }
            if (idxOfNextSlash != -1 && idxOfNextSlash < idxOfNextClosingCurly)
            {
                throw new IllegalStateException("Name of match group is not allowed to contain a forward slash");
            }

            matchPatternBuilder.append(urlPattern.substring(previousEndIdx, idxOfOpeningCurly));
            matchPatternBuilder.append("([^/]+)");
            previousEndIdx = idxOfNextClosingCurly + 1;

            final String matchVariableName = urlPattern.substring(idxOfOpeningCurly + 1, idxOfNextClosingCurly);
            matchVariableNames.add(matchVariableName);

            idxOfOpeningCurly = urlPattern.indexOf('{', previousEndIdx);
        }
        matchPatternBuilder.append(urlPattern.substring(previousEndIdx));

        if (matchPatternBuilder.charAt(0) != '/')
        {
            matchPatternBuilder.insert(0, '/');
        }
        matchPatternBuilder.insert(0, '^');
        matchPatternBuilder.append('$');

        final Pattern matchPattern = Pattern.compile(matchPatternBuilder.toString());
        return matchPattern;
    }

    protected Optional<Map<String, Object>> matchRequestUrlAndExtractVariables(final HttpServletRequest request)
    {
        final InboundConfig inboundConfig = this.webhookConfig.getInbound();
        LOGGER.debug("Trying to match request {} against {}", request, this.webhookConfig);

        final Optional<Map<String, Object>> urlVariables;

        final String requestURI = request.getRequestURI();
        final String requestMethod = request.getMethod();

        final String configuredRequestMethod = inboundConfig.getRequestMethod();
        if (configuredRequestMethod == null || configuredRequestMethod.equalsIgnoreCase(requestMethod))
        {
            final int idxOfQueryString = requestURI.indexOf('?');
            final String requestPath = idxOfQueryString != -1 ? requestURI.substring(0, idxOfQueryString) : requestURI;
            final Matcher pathMatcher = this.urlMatchPattern.matcher(requestPath);
            if (pathMatcher.matches())
            {
                final Map<String, Object> urlVariablesMap = new HashMap<>();
                final int groupCount = pathMatcher.groupCount();
                for (int group = 0; group < groupCount; group++)
                {
                    final String match = pathMatcher.group(group + 1);
                    final String variableName = this.matchVariableNames.get(group);
                    urlVariablesMap.put(variableName, match);
                }
                LOGGER.debug("Extracted URI variables {} from {} via pattern {}", urlVariablesMap, requestURI,
                        inboundConfig.getUrlPattern());

                urlVariables = Optional.of(urlVariablesMap);
            }
            else
            {
                LOGGER.debug("Configured path pattern {} does not match request path {}", inboundConfig.getUrlPattern(), requestPath);
                urlVariables = Optional.empty();
            }
        }
        else
        {
            LOGGER.debug("Request to {} using method {} does not match configured request method {}", requestURI, requestMethod,
                    configuredRequestMethod);
            urlVariables = Optional.empty();
        }
        return urlVariables;
    }

    protected boolean verifySecret(final HttpServletRequest request, final InboundConfig inboundConfig) throws IOException
    {
        boolean validSecret = true;
        final SecretConfig configuredSecret = inboundConfig.getSecret();

        if (configuredSecret != null)
        {
            validSecret = false;

            final String requestURI = request.getRequestURI();
            final Mode mode = configuredSecret.getMode();
            final String name = configuredSecret.getName();
            final String secretValue = configuredSecret.getSecretValue();
            final String digestAlgorithm = configuredSecret.getDigestAlgorithm();

            if (name != null && mode != null)
            {
                switch (mode)
                {
                    case PLAIN_HEADER:
                        if (secretValue == null)
                        {
                            LOGGER.warn("Inbound secret configuration is incomplete");
                        }
                        else
                        {
                            final String headerSecret = request.getHeader(name);
                            validSecret = secretValue.equals(headerSecret);
                        }
                        break;
                    case PLAIN_PARAMETER:
                        if (secretValue == null)
                        {
                            LOGGER.warn("Inbound secret configuration is incomplete");
                        }
                        else
                        {
                            final String paramSecret = request.getParameter(name);
                            validSecret = secretValue.equals(paramSecret);
                        }
                        break;
                    case BODY_DIGEST_HEADER:
                        final String simpleSignature = request.getHeader(name);
                        validSecret = simpleSignature != null && this.verifyRequestDigest(request, simpleSignature, null, digestAlgorithm);
                        break;
                    case BODY_DIGEST_WITH_SHARED_SECRET_HEADER:
                        if (secretValue == null)
                        {
                            LOGGER.warn("Inbound secret configuration is incomplete");
                        }
                        else
                        {
                            final String saltedSignature = request.getHeader(name);
                            validSecret = saltedSignature != null
                                    && this.verifyRequestDigest(request, saltedSignature, secretValue, digestAlgorithm);
                        }
                        break;
                    default:
                        LOGGER.warn("Unsupported secret mode {} - unable to verify secret on request to {}", mode, request.getRequestURI());
                        validSecret = false;
                }

                if (!validSecret)
                {
                    LOGGER.warn("Secret in request to {} retrieved via {} using mode {} is invalid", requestURI, name, mode);
                }
                else
                {
                    LOGGER.debug("Secret in request to {} retrieved via {} using mode {} is valid", requestURI, name, mode);
                }
            }
            else
            {
                LOGGER.warn("Inbound secret configuration is incomplete");
            }
        }
        else
        {
            LOGGER.debug("No inbound secret has been configured");
        }

        return validSecret;
    }

    protected boolean verifyRequestDigest(final HttpServletRequest request, final String signature, final String sharedSecret,
            final String digestAlgorithm) throws IOException
    {
        final int idxOfEq = signature.indexOf('=');
        final String digestValue = idxOfEq != -1 ? signature.substring(idxOfEq + 1) : signature;

        boolean validDigest = false;

        try
        {
            try (ServletInputStream sis = request.getInputStream())
            {
                if (sharedSecret != null)
                {
                    final byte[] calculatedDigest = generateSaltedDigest(sis, sharedSecret, digestAlgorithm);
                    validDigest = this.digestsMatch(digestValue, calculatedDigest);
                }
                else
                {
                    final byte[] calculatedDigest = generateDigest(sis, digestAlgorithm);
                    validDigest = this.digestsMatch(digestValue, calculatedDigest);
                }
            }
        }
        catch (final NoSuchAlgorithmException nsaEx)
        {
            LOGGER.error("Failed to verify request digest due to missing algorithm", nsaEx);
        }
        catch (final InvalidKeyException ikEx)
        {
            LOGGER.error("Failed to verify request digest due to invalid key", ikEx);
        }
        return validDigest;
    }

    protected boolean digestsMatch(final String requestProvidedDigest, final byte[] calculatedDigestBytes)
    {
        final String calculcatedDigestHex = toHex(calculatedDigestBytes);
        final boolean digestsMatch = requestProvidedDigest.equals(calculcatedDigestHex);
        return digestsMatch;
    }

    protected void prepareScriptBindings(final Context context, final Request baseRequest, final HttpServletRequest request,
            final HttpServletResponse response, final Map<String, Object> urlVariables) throws IOException
    {
        final Value jsBindings = context.getBindings("js");

        // remove top-level members to which we do not want to allow access
        JS_TOP_LEVEL_MEMBERS_TO_REMOVE.forEach(member -> jsBindings.removeMember(member));

        jsBindings.putMember("url", request.getRequestURL().toString());
        jsBindings.putMember("urlVariables", ProxyObject.fromMap(urlVariables));
        jsBindings.putMember("args", ProxyObject.fromMap(new HashMap<>(request.getParameterMap())));

        final Map<String, Object> headers = new HashMap<>();
        final Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements())
        {
            final String headerName = headerNames.nextElement();
            final Enumeration<String> headerValues = request.getHeaders(headerName);
            final List<String> headerValuesL = new ArrayList<>();
            while (headerValues.hasMoreElements())
            {
                headerValuesL.add(headerValues.nextElement());
            }

            headers.put(headerName, headerValuesL.size() == 1 ? headerValuesL.get(0) : headerValuesL.toArray(new String[0]));
        }
        jsBindings.putMember("headers", ProxyObject.fromMap(headers));

        if (BODY_FULL_REQUEST_METHODS.contains(request.getMethod().toLowerCase(Locale.ENGLISH)))
        {
            jsBindings.putMember("contentType", request.getContentType());
            try (BufferedReader r = new BufferedReader(new InputStreamReader(request.getInputStream(), request.getCharacterEncoding())))
            {
                final StringBuilder sb = new StringBuilder(1024 * 100);
                String line;
                while ((line = r.readLine()) != null)
                {
                    sb.append(line);
                }
                jsBindings.putMember("content", sb.toString());
            }
            catch (final IOException ioEx)
            {
                LOGGER.error("Error reading request content", ioEx);
                throw ioEx;
            }
        }

        final OutboundConfig outboundConfig = this.webhookConfig.getOutbound();
        jsBindings.putMember("requestUrl", outboundConfig.getRequestUrl());
        jsBindings.putMember("requestMethod", outboundConfig.getRequestMethod());
        jsBindings.putMember("requestContentType", outboundConfig.getRequestContentType());

        final ProxyExecutable skipFn = arguments -> this.processSkip(baseRequest, request, response);
        final ProxyExecutable sendFn = arguments -> this.processSend(arguments, baseRequest, request, response);

        jsBindings.putMember("skip", skipFn);
        jsBindings.putMember("send", sendFn);
        jsBindings.putMember("logger", LOGGER);
    }

    protected Boolean processSkip(final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response)
    {
        Boolean result;
        if (!baseRequest.isHandled())
        {
            LOGGER.info("Skipped processing request {}", request);
            response.setStatus(HttpServletResponse.SC_OK);
            baseRequest.setHandled(true);
            result = Boolean.TRUE;
        }
        else
        {
            LOGGER.warn("Request {} has already been handled - skip() has no effect", request);
            result = Boolean.FALSE;
        }
        return result;
    }

    protected Boolean processSend(final Value[] arguments, final Request baseRequest, final HttpServletRequest request,
            final HttpServletResponse response)
    {
        if (arguments.length == 0 || !arguments[0].isString())
        {
            throw new IllegalArgumentException("Missing arguments in call to send()");
        }

        Boolean result;
        if (!baseRequest.isHandled())
        {
            final OutboundConfig outboundConfig = this.webhookConfig.getOutbound();
            final SecretConfig secret = outboundConfig.getSecret();
            final String requestMethod = outboundConfig.getRequestMethod().toLowerCase(Locale.ENGLISH);

            String requestUrl = null;
            String requestData = null;
            String requestContentType = null;
            if (BODY_LESS_REQUEST_METHODS.contains(requestMethod))
            {
                requestUrl = arguments[0].asString();
            }
            else if (BODY_FULL_REQUEST_METHODS.contains(requestMethod))
            {
                requestData = arguments[0].asString();
                requestContentType = arguments.length >= 2 && arguments[1].isString() ? arguments[1].asString()
                        : outboundConfig.getRequestContentType();
                requestUrl = arguments.length >= 3 && arguments[2].isString() ? arguments[2].asString() : outboundConfig.getRequestUrl();
            }
            else
            {
                LOGGER.error("Unsupported request method {}", requestMethod);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                baseRequest.setHandled(true);
            }

            if (requestUrl != null)
            {
                final HttpRequest httpRequest = this.doSendRequest(requestMethod, requestUrl, requestData, requestContentType, secret);

                final int code = httpRequest.code();
                if (code >= 200 && code < 300)
                {
                    LOGGER.info("Request to {} succeeded", requestUrl);
                    response.setStatus(HttpServletResponse.SC_OK);
                    baseRequest.setHandled(true);
                    result = Boolean.TRUE;
                }
                else
                {
                    final String message = httpRequest.message();
                    final String body = httpRequest.body();

                    LOGGER.warn("Received non-OK status from request to {}: {} - {}\n{}", requestUrl, code, message, body);

                    baseRequest.setHandled(true);
                    result = Boolean.FALSE;
                }
            }
            else
            {
                result = Boolean.FALSE;
            }
        }
        else
        {
            LOGGER.warn("Request {} has already been handled - send() has no effect", request);
            result = Boolean.FALSE;
        }
        return result;
    }

    protected HttpRequest doSendRequest(final String requestMethod, final String requestUrl, final String requestData,
            final String requestContentType, final SecretConfig secret)
    {
        final Mode secretMode = secret != null ? secret.getMode() : null;
        final String secretName = secret != null ? secret.getName() : null;
        final String secretValue = secret != null ? secret.getSecretValue() : null;
        final String secretDigestAlgorith = secret != null ? secret.getDigestAlgorithm() : null;

        boolean useSecretArgs = false;
        if (secretMode == Mode.PLAIN_PARAMETER)
        {
            if (secretName != null && secretValue != null)
            {
                useSecretArgs = true;
            }
            else
            {
                LOGGER.warn("Incomplete secret configuration - not sending secret with request to {}", requestUrl);
            }
        }

        HttpRequest httpRequest;
        switch (requestMethod)
        {
            case "put":
                httpRequest = useSecretArgs ? HttpRequest.put(requestUrl, true, secretName, secretValue) : HttpRequest.put(requestUrl);
                break;
            case "post":
                httpRequest = useSecretArgs ? HttpRequest.post(requestUrl, true, secretName, secretValue) : HttpRequest.post(requestUrl);
                break;
            case "delete":
                httpRequest = useSecretArgs ? HttpRequest.delete(requestUrl, true, secretName, secretValue)
                        : HttpRequest.delete(requestUrl);
                break;
            case "get":
            default:
                httpRequest = useSecretArgs ? HttpRequest.get(requestUrl, true, secretName, secretValue) : HttpRequest.get(requestUrl);
        }

        httpRequest = httpRequest.userAgent("OOTBee Webhook-Hub (http://orderofthebee.org, 1.0-SNAPSHOT)");
        try
        {
            httpRequest = this.doSendRequestHandleHeaderSecret(httpRequest, requestUrl, requestData, secretMode, secretName, secretValue,
                    secretDigestAlgorith);
        }
        catch (final IOException ioEX)
        {
            LOGGER.error("Failed to generate request digest due to IO error", ioEX);
        }
        catch (final NoSuchAlgorithmException nsaEx)
        {
            LOGGER.error("Failed to generate request digest due to missing algorithm", nsaEx);
        }
        catch (final InvalidKeyException ikEx)
        {
            LOGGER.error("Failed to generate request digest due to invalid key", ikEx);
        }

        if (requestData != null)
        {
            LOGGER.info("Sending request data {} in {} format via {} to {}", requestData, requestContentType, requestMethod, requestUrl);
            httpRequest = httpRequest.contentType(requestContentType, StandardCharsets.UTF_8.name()).send(requestData);
        }
        else
        {
            LOGGER.info("Sending request via {} to {}", requestMethod, requestUrl);
        }
        return httpRequest;
    }

    protected HttpRequest doSendRequestHandleHeaderSecret(final HttpRequest httpRequest, final String requestUrl,
            final String requestMessage, final Mode secretMode, final String secretName, final String secretValue,
            final String secretDigestAlgorith) throws NoSuchAlgorithmException, InvalidKeyException, IOException
    {
        HttpRequest resultRequest = httpRequest;
        if (secretMode != null && secretMode != Mode.PLAIN_PARAMETER)
        {
            String effectiveSecretValue = null;
            switch (secretMode)
            {
                case PLAIN_HEADER:
                    if (secretName != null && secretValue != null)
                    {
                        effectiveSecretValue = secretValue;
                    }
                    else
                    {
                        LOGGER.warn("Incomplete secret configuration - not sending secret with request to {}", requestUrl);
                    }
                    break;
                case BODY_DIGEST_HEADER:
                    if (secretName != null)
                    {
                        if (requestMessage != null)
                        {
                            final byte[] generateDigest = generateDigest(
                                    new ByteArrayInputStream(requestMessage.getBytes(StandardCharsets.UTF_8.name())), secretDigestAlgorith);
                            effectiveSecretValue = toHex(generateDigest);
                        }
                        else
                        {
                            LOGGER.warn("Unable to generate message digest in request to {} since no content will be sent", requestUrl);
                        }
                    }
                    else
                    {
                        LOGGER.warn("Incomplete secret configuration - not sending secret with request to {}", requestUrl);
                    }
                    break;
                case BODY_DIGEST_WITH_SHARED_SECRET_HEADER:
                    if (secretName != null && secretValue != null)
                    {
                        if (requestMessage != null)
                        {
                            final byte[] generateSaltedDigest = generateSaltedDigest(
                                    new ByteArrayInputStream(requestMessage.getBytes(StandardCharsets.UTF_8.name())), secretValue,
                                    secretDigestAlgorith);
                            effectiveSecretValue = toHex(generateSaltedDigest);
                        }
                        else
                        {
                            LOGGER.warn("Unable to generate message digest in request to {} since no content will be sent", requestUrl);
                        }
                    }
                    else
                    {
                        LOGGER.warn("Incomplete secret configuration - not sending secret with request to {}", requestUrl);
                    }
                    break;
                default:
                    LOGGER.warn("Unsupported secret mode {} - not sending secret with request to {}", secretMode, requestUrl);
            }

            if (effectiveSecretValue != null)
            {
                resultRequest = httpRequest.header(secretName, effectiveSecretValue);
            }
        }
        return resultRequest;
    }
}
