package org.orderofthebee.tools.webhook.hub;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ShutdownHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;

/**
 * @author Axel Faust
 */
public class Runner
{

    private static final Logger LOGGER = LoggerFactory.getLogger(Runner.class);

    private static final Set<String> PRIMARY_COMMANDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("start", "stop")));

    public abstract static class BaseOptions
    {

        @Argument(required = false, description = "The port on which to run the server or contact the server for shutdown")
        private Integer port;

        public void setPort(final Integer port)
        {
            this.port = port;
        }

        public Integer getPort()
        {
            return this.port;
        }

        public int getEffectivePort()
        {
            final int port = this.port != null ? this.port.intValue() : 8080;
            if (port <= 0)
            {
                throw new IllegalArgumentException("'port' must be a positive integer");
            }
            return port;
        }
    }

    public static class StartOptions extends BaseOptions
    {

        @Argument(required = true, description = "The YAML file defining the mappings / transformations to apply to incoming webhook requests")
        private String configFile;

        /**
         * @return the configFile
         */
        public String getConfigFile()
        {
            return this.configFile;
        }

        /**
         * @param configFile
         *            the configFile to set
         */
        public void setConfigFile(final String configFile)
        {
            this.configFile = configFile;
        }

    }

    public static class StopOptions extends BaseOptions
    {
        // No additions yet
    }

    public static void main(final String[] args)
    {
        if (args.length == 0 || !PRIMARY_COMMANDS.contains(args[0].toLowerCase(Locale.ENGLISH)))
        {
            System.err.println("Missing primary command");
            System.exit(1);
        }

        final String primaryCommand = args[0].toLowerCase(Locale.ENGLISH);
        final String[] remainingArgs = new String[args.length - 1];
        System.arraycopy(args, 1, remainingArgs, 0, remainingArgs.length);

        int exitCode = 0;
        switch (primaryCommand)
        {
            case "start":
                final StartOptions startOptions = new StartOptions();
                Args.parseOrExit(startOptions, remainingArgs);
                exitCode = runServer(startOptions);
                break;
            case "stop":
                final StopOptions stopOptions = new StopOptions();
                Args.parseOrExit(stopOptions, remainingArgs);
                exitCode = stopServer(stopOptions);
                break;
        }

        System.exit(exitCode);
    }

    private static int runServer(final StartOptions options)
    {
        int exitCode;
        try
        {
            LOGGER.info("Starting Webhook Hub");
            final String shutdownToken = UUID.randomUUID().toString();
            final Server server = setupServer(shutdownToken, options);
            storeShutdownToken(shutdownToken);
            server.start();
            LOGGER.info("Webhook Hub started");

            server.join();

            LOGGER.info("Webhook Hub stopped");

            exitCode = 0;
        }
        catch (final Exception ex)
        {
            LOGGER.error("Error running Webhook Hub", ex);
            exitCode = 1;
        }
        finally
        {
            deleteShutdownToken();
        }
        return exitCode;
    }

    private static Server setupServer(final String shutdownToken, final StartOptions options) throws IOException
    {
        ServerConfig serverConfig;

        final Path configFilePath = Paths.get(options.getConfigFile());
        LOGGER.info("Loading server configuration file {}", configFilePath.toAbsolutePath());
        try (Reader cr = new InputStreamReader(Files.newInputStream(configFilePath, StandardOpenOption.READ), StandardCharsets.UTF_8))
        {
            final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            serverConfig = mapper.readValue(cr, ServerConfig.class);
        }

        final Server server = new Server(options.getEffectivePort());

        final List<Handler> handlers = new ArrayList<>();
        handlers.add(new ShutdownHandler(shutdownToken));

        serverConfig.getWebhooks().forEach(webhookConfig -> handlers.add(new WebhookHandler(webhookConfig)));

        final HandlerList handlerList = new HandlerList(handlers.toArray(new Handler[0]));
        server.setHandler(handlerList);
        return server;
    }

    private static void storeShutdownToken(final String shutdownToken)
    {
        final Path path = Paths.get("shutdown_token");
        LOGGER.debug("Storing shutdown token in {}", path.toAbsolutePath());
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
                Files.newOutputStream(path, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE),
                StandardCharsets.UTF_8)))
        {
            pw.println(shutdownToken);
        }
        catch (final IOException ioEx)
        {
            LOGGER.error("Failed to store shutdown token", ioEx);
        }
    }

    private static void deleteShutdownToken()
    {
        final Path path = Paths.get("shutdown_token");
        final File file = path.toFile();
        if (file.exists())
        {
            LOGGER.debug("Deleting shutdown token in {}", path.toAbsolutePath());
            if (!file.delete())
            {
                file.deleteOnExit();
            }
        }
    }

    private static int stopServer(final StopOptions options)
    {
        int exitCode;
        try
        {
            LOGGER.info("Sending request to stop Webhook Hub");
            final String shutdownToken = loadShutdownToken();

            final StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append("http://localhost:");
            urlBuilder.append(options.getEffectivePort());
            urlBuilder.append("/shutdown?token=");
            urlBuilder.append(shutdownToken);

            final URL url = new URL(urlBuilder.toString());
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");

            final int responseCode = connection.getResponseCode();
            if (responseCode == 200)
            {
                LOGGER.info("Request to stop Webhook Hub completed");
                exitCode = 0;
            }
            else
            {
                LOGGER.warn("Request to stop Webhook Hub did not complete as expected with {}", connection.getResponseMessage());
                exitCode = 2;
            }
        }
        catch (final Exception ex)
        {
            LOGGER.error("Error stopping Webhook Hub", ex);
            exitCode = 1;
        }
        return exitCode;
    }

    private static String loadShutdownToken() throws IOException
    {
        final Path path = Paths.get("shutdown_token");
        LOGGER.info("Loading shutdown token from {}", path.toAbsolutePath());
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(Files.newInputStream(path, StandardOpenOption.READ), StandardCharsets.UTF_8)))
        {
            final String shutdownToken = br.readLine();
            return shutdownToken;
        }
        catch (final IOException ioEx)
        {
            LOGGER.error("Failed to load shutdown token", ioEx);
            throw ioEx;
        }
    }
}
