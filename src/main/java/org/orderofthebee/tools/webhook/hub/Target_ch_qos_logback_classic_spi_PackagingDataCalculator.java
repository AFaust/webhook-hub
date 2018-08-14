package org.orderofthebee.tools.webhook.hub;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * @author Axel Faust
 */
@TargetClass(ch.qos.logback.classic.spi.PackagingDataCalculator.class)
final class Target_ch_qos_logback_classic_spi_PackagingDataCalculator
{

    @Substitute
    String getCodeLocation(final Class<?> type)
    {
        return "na";
    }
}
