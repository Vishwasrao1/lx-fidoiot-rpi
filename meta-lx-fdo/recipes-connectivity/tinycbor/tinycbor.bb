DESCRIPTION = "Recipe for tinycbor library"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "git://github.com/intel/tinycbor.git"
SRCREV = "755f9ef932f9830a63a712fd2ac971d838b131f1"

S = "${WORKDIR}/git"

do_compile() {
    make
}

do_install() {
    install -d ${D}${includedir}
    install -m 0644 ${S}/src/*.h ${D}${includedir}/
    install -d ${D}${libdir}
    install -m 0644 ${S}/lib/libtinycbor.a ${D}${libdir}/
}

