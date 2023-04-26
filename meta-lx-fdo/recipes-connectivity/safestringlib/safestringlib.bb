DESCRIPTION = "Recipe for safestringlib library"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "git://github.com/intel/safestringlib.git;branch=master"
SRCREV = "2ccc19fb68653f20ebd616b85b94912c0beeda72"

S = "${WORKDIR}/git"

inherit cmake

do_compile() {
    if [ ! -d "safestringlib" ] ; then
        cd ${S}
        rm -rf makefile
        sed -i '/mmitigate-rop/d' ./CMakeLists.txt
        cmake .
        make
        cp libsafestring_static.a libsafestring.a
    fi
}

do_install() {
    install -d ${D}${includedir}
    install -m 0644 ${S}/include/*.h ${D}${includedir}/
    install -d ${D}${libdir}
    install -m 0644 ${S}/libsafestring.a ${D}${libdir}/
}
