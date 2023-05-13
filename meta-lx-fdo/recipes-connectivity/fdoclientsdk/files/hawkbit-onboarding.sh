#!/bin/bash
if [[ -f /opt/fdo/hawkbit.config ]]; then
    url=$(sed -n 's/URL:\(.*\)/\1/p' /opt/fdo/hawkbit.config | tr -d ' ')
    controllerid=$(awk -F: '/ControllerId:/ {print $2}' /opt/fdo/hawkbit.config | awk '{$1=$1};1')
    securitytoken=$(awk -F: '/SecurityToken:/ {print $2}' /opt/fdo/hawkbit.config | awk '{$1=$1};1')
    /usr/bin/swupdate -v -u "-t DEFAULT -x -u $url -i $controllerid -k $securitytoken" >> /opt/fdo/hawkbit.log 2>&1
fi