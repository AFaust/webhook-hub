FROM ${images.from.id}:${images.from.version.majorVersion}.${images.from.version.minorVersion}.${images.from.version.incrementalVersion}

COPY initHub.sh startHub.sh hooks.yml ${project.build.finalName}-shaded.jar /tmp/

# if we had installed GraalVM in this build we could also uninstall it after running natve-image to achieve a much slimmer image
RUN install_clean \
      gcc \
      zlib1g-dev \
   && mkdir -p /var/lib/webhook-hub \
   && mv /tmp/${project.build.finalName}-shaded.jar /var/lib/webhook-hub/webhook-hub.jar \
   && cd /var/lib/webhook-hub \
   && /usr/lib/jvm/graalvm-ce-${graalvm.version}/bin/native-image -jar ./webhook-hub.jar -H:Name=webhook-hub -H:+JNI -H:EnableURLProtocols=http \
   && apt-get autoremove -y \
      gcc \
      zlib1g-dev \
   && mkdir -p /srv/webhook-hub \
   && mv /tmp/hooks.yml /srv/webhook-hub/ \
   && mkdir -p /etc/my_init.d \
   && mv /tmp/initHub.sh /etc/my_init.d/20_initWebhook-Hub.sh \
   && chmod +x /etc/my_init.d/20_initWebhook-Hub.sh \
   && mkdir /etc/service/webhook-hub \
   && mv /tmp/startHub.sh /etc/service/webhook-hub/run \
   && chmod +x /etc/service/webhook-hub/run

EXPOSE 8080

LABEL vendor="${images.labels.vendor}" \
   ${images.labels.namespace}.version="${project.version.majorVersion}.${project.version.minorVersion}.${project.version.incrementalVersion}" \
   ${images.labels.namespace}.is-beta="" \
   ${images.labels.namespace}.is-production="" \
   ${images.labels.namespace}.release-date="${images.label.release-date}" \
   ${images.labels.namespace}.maintainer="${images.labels.maintainer}"