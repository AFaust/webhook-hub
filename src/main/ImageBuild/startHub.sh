#!/bin/sh
exec java -jar /var/lib/webhook-hub/webhook-hub.jar start -configFile /srv/webhook-hub/hooks.yml > /proc/1/fd/1 2>&1