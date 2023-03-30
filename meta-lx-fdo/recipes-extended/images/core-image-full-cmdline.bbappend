DESCRIPTION = "A console-only image with more full-featured Linux system \
functionality installed."

CORE_IMAGE_EXTRA_INSTALL += " swupdate \
                         swupdate-www \
                         swupdate-client \
                         swupdate-lua \
                         swupdate-progress \
                         swupdate-tools \
                         swupdate-tools-hawkbit \
                         swupdate-tools-ipc \
                         swupdate-usb \
                         u-boot-fw-utils \
                         fdoclientsdk \
                         bash \
                         curl \
                         jq"

