DESCRIPTION = "Add additional packages to rpi image."

IMAGE_INSTALL += " swupdate \
                   swupdate-www \
                   swupdate-client \
                   swupdate-progress \
                   swupdate-lua \
                   swupdate-tools \
                   swupdate-tools-hawkbit \
                   swupdate-tools-ipc \
                   swupdate-usb \
                   u-boot-fw-utils \
                   fdoclientsdk \
                   bash \
                   curl \
                   jq"
