#!/bin/bash

if [ ! -d "/mnt/idcard/usf" ]; then
    echo "ID Card server mountpoint missing! Remounting..."
    /usr/bin/mount /mnt/idcard
fi
