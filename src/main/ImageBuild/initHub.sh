#!/bin/bash

set -e

if [ ! -f '/var/lib/webhook-hub/.initDone' ]
then
   # otherwise for will also cut on whitespace
   IFS=$'\n'
   for i in `env`
   do
      if [[ $i == WEBHOOK_* ]]
      then
         echo "Processing environment variable $i" > /proc/1/fd/1
         key=`echo "$i" | cut -d '=' -f 1 | cut -d '_' -f 2-`
         value=`echo "$i" | cut -d '=' -f 2-`
         # encode any / in $value to avoid interference with sed (note: sh collapses 2 \'s into 1)
         value=`echo "$value" | sed -r 's/\\//\\\\\//g'`

         sed -i "s/%${key}%/$value/" /srv/webhook-hub/hooks.yml
      fi
   done

   touch /var/lib/webhook-hub/.initDone
fi