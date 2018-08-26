FROM ${docker.from.id}-graalvm:${docker.from.version} as graalvm

COPY ${project.build.finalName}-shaded.jar /tmp/

RUN mkdir -p /var/lib/webhook-hub \
   && mv /tmp/${project.build.finalName}-shaded.jar /var/lib/webhook-hub/webhook-hub.jar \
   && cd /var/lib/webhook-hub \
   && /usr/lib/jvm/graalvm-ce-${dep.graalvm.version}/bin/native-image --language:js -jar ./webhook-hub.jar -H:Name=webhook-hub -H:+JNI -H:EnableURLProtocols=http

FROM ${docker.from.id}:${docker.from.version}

COPY --from=graalvm /var/lib/webhook-hub/webhook-hub /var/lib/webhook-hub/webhook-hub
COPY initHub.sh startHub.sh hooks.yml /tmp/

RUN mkdir -p /srv/webhook-hub \
   && mv /tmp/hooks.yml /srv/webhook-hub/ \
   && mkdir -p /etc/my_init.d \
   && mv /tmp/initHub.sh /etc/my_init.d/20_initWebhook-Hub.sh \
   && chmod +x /etc/my_init.d/20_initWebhook-Hub.sh \
   && mkdir /etc/service/webhook-hub \
   && mv /tmp/startHub.sh /etc/service/webhook-hub/run \
   && chmod +x /etc/service/webhook-hub/run

EXPOSE 8080

LABEL vendor="${docker.labels.vendor}" \
   ${docker.labels.namespace}.version="${project.version}" \
   ${docker.labels.namespace}.is-beta="" \
   ${docker.labels.namespace}.is-production="" \
   ${docker.labels.namespace}.release-date="${docker.labels.release-date}" \
   ${docker.labels.namespace}.maintainer="${docker.labels.maintainer}"