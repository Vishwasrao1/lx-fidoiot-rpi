DESCRIPTION = "Recipe for metee library"
LICENSE = "Apache-2.0"
#BB_STRICT_CHECKSUM = "0"

LIC_FILES_CHKSUM = "file://COPYING;md5=2ee41112a44fe7014dce33e26468ba93"


SRC_URI = "git://github.com/intel/metee.git;protocol=https"
SRCREV = "8baaedbed9e4b793f2802ce2e6e3cf00da3fc1ec"

DEPENDS = "openssl cmake"

S = "${WORKDIR}/git"

inherit cmake

do_compile() {
    cd ${S}
    cmake .
    make -j4
}

do_install() {
    install -d ${D}${prefix}/include/metee
    install -m 0644 ${S}/include/* ${D}${prefix}/include/metee/
    install -d ${D}${prefix}/lib/
    install -m 0644 ${S}/libmetee.a ${D}${prefix}/lib/
}

PACKAGES_DYNAMIC += "^${PN}-.*"
FILES_${PN} += "${libdir}/*"
