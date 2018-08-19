package org.orderofthebee.tools.webhook.hub;

import org.graalvm.nativeimage.Feature;
import org.graalvm.nativeimage.RuntimeReflection;
import org.orderofthebee.tools.webhook.hub.Runner.BaseOptions;
import org.orderofthebee.tools.webhook.hub.Runner.StartOptions;
import org.orderofthebee.tools.webhook.hub.Runner.StopOptions;

import com.oracle.svm.core.annotate.AutomaticFeature;

/**
 * @author Axel Faust
 */
@AutomaticFeature
class RuntimeReflectionRegistrationFeature implements Feature
{

    @Override
    public void beforeAnalysis(final BeforeAnalysisAccess access)
    {
        // CLI options
        RuntimeReflection.register(BaseOptions.class);
        RuntimeReflection.register(StartOptions.class);
        RuntimeReflection.register(StopOptions.class);
        RuntimeReflection.register(BaseOptions.class.getDeclaredFields());
        RuntimeReflection.register(BaseOptions.class.getDeclaredMethods());
        RuntimeReflection.register(StartOptions.class.getDeclaredFields());
        RuntimeReflection.register(StartOptions.class.getDeclaredMethods());
        RuntimeReflection.register(StopOptions.class.getDeclaredFields());
        RuntimeReflection.register(StopOptions.class.getDeclaredMethods());

        // YAML config
        RuntimeReflection.register(ServerConfig.class);
        // RuntimeReflection.register(ServerConfig.class.getConstructors());
        RuntimeReflection.register(ServerConfig.class.getDeclaredFields());
        RuntimeReflection.register(ServerConfig.class.getDeclaredMethods());
        RuntimeReflection.register(WebhookConfig.class);
        // RuntimeReflection.register(WebhookConfig.class.getConstructors());
        RuntimeReflection.register(WebhookConfig.class.getDeclaredFields());
        RuntimeReflection.register(WebhookConfig.class.getDeclaredMethods());
        RuntimeReflection.register(InboundConfig.class);
        // RuntimeReflection.register(InboundConfig.class.getConstructors());
        RuntimeReflection.register(InboundConfig.class.getDeclaredFields());
        RuntimeReflection.register(InboundConfig.class.getDeclaredMethods());
        RuntimeReflection.register(OutboundConfig.class);
        // RuntimeReflection.register(OutboundConfig.class.getConstructors());
        RuntimeReflection.register(OutboundConfig.class.getDeclaredFields());
        RuntimeReflection.register(OutboundConfig.class.getDeclaredMethods());
        RuntimeReflection.register(SecretConfig.class);
        // RuntimeReflection.register(SecretConfig.class.getConstructors());
        RuntimeReflection.register(SecretConfig.class.getDeclaredFields());
        RuntimeReflection.register(SecretConfig.class.getDeclaredMethods());
    }
}
