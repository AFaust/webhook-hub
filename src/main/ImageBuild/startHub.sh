#!/bin/sh
exec /var/lib/webhook-hub/webhook-hub start -configFile /srv/webhook-hub/hooks.yml > /proc/1/fd/1 2>&1