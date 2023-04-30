FILESEXTRAPATHS:append := "${THISDIR}/${PN}:"

PACKAGECONFIG_CONFARGS = ""

SRC_URI += " \
    file://swupdate-progress.c \
    file://defconfig \
    "

#do_install:append() {
#    rm ${D}${systemd_system_unitdir}/swupdate.socket
#}

do_compile:prepend() {
    cp ${WORKDIR}/swupdate-progress.c ${S}/tools/
}

SYSTEMD_AUTO_ENABLE_swupdate.socket = "disable"
SYSTEMD_AUTO_ENABLE_swupdate-progress = "disable"
SYSTEMD_SERVICE_${PN} = "swupdate.service"
SYSTEMD_AUTO_ENABLE_swupdate = "disable"

