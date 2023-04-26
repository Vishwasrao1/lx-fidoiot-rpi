#fdo yocto project

DESCRIPTION = "Recipe for fdo (client-sdk) on linux"
LICENSE = "Apache-2.0"
BB_STRICT_CHECKSUM = "0"

LIC_FILES_CHKSUM = "file://LICENSE;md5=fa818a259cbed7ce8bc2a22d35a464fc"

SRCREV = "0e6913c0309232a5556dffad64e5a3740c903c62"
SRC_URI = "git://github.com/Vishwasrao1/client-sdk-fidoiot.git"

#adding just for testing & development purpose will be removed in release
SRC_URI += "\
    file://ecdsa384privkey.dat \
    file://ecdsa384privkey.pem \
    file://manufacturer_addr.bin \
    file://manufacturer_sn.bin \
"

S = "${WORKDIR}/git"

TOOLCHAIN = "POKY-GLIBC"
APP_NAME = "c_code_sdk"
DEPENDS += "openssl gcc curl safestringlib tinycbor metee"
PREFERRED_VERSION_curl = "7.86"

inherit pkgconfig cmake

FILES:${PN} += "/opt \
                /opt/fdo \
                /opt/fdo/linux-client"


# make command parameters
BUILD = "debug"
HTTPPROXY = "false"
AES_MODE = "gcm"
DA = "ecdsa384"

do_configure(){
}

do_compile(){
    export SAFESTRING_ROOT=${STAGING_DIR_TARGET}${prefix}
    export TINYCBOR_ROOT=${STAGING_DIR_TARGET}${prefix}
    export METEE_ROOT=${STAGING_DIR_TARGET}${prefix}
    #Remove -DRESUSE=true at the time of production
    cd ${S}
    cmake -DREUSE=true -DHTTPPROXY=${HTTPPROXY} -DBUILD=${BUILD} -DDA=${DA} -DAES_MODE=${AES_MODE} -DOPTIMIZE=1 .
    make -j8
}

do_install() {
    install -d "${D}/opt/fdo"
    install "${WORKDIR}/git/build/linux-client" "${D}/opt/fdo"
    install -d "${D}/opt/fdo/data"
    cp -r "${WORKDIR}/git/data/" "${D}/opt/fdo/"
    cp -r "${WORKDIR}/ecdsa384privkey.dat" "${D}/opt/fdo/data/"
    cp -r "${WORKDIR}/ecdsa384privkey.pem" "${D}/opt/fdo/data/"
    cp -r "${WORKDIR}/manufacturer_addr.bin" "${D}/opt/fdo/data/"
    cp -r "${WORKDIR}/manufacturer_sn.bin" "${D}/opt/fdo/data/"
    install -d "${D}/opt/fdo/data_bkp"
    cp -r "${WORKDIR}/git/data/" "${D}/opt/fdo/data_bkp"
}

do_package_qa[noexec] = "1"

INITSCRIPT_PACKAGES = "${PN}"