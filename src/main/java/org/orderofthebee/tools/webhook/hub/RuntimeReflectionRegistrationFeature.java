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
    }
}
