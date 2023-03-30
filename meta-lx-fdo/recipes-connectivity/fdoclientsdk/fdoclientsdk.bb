#fdo yocto project

DESCRIPTION = "Recipe for fdo (client-sdk) on linux"
LICENSE = "Apache-2.0"
BB_STRICT_CHECKSUM = "0"

LIC_FILES_CHKSUM = "file://LICENSE;md5=fa818a259cbed7ce8bc2a22d35a464fc"

SRCREV = "11867d01fc8bf6169dc14d99c8ed91f50be465b7"
SRC_URI = "git://github.com/Vishwasrao1/client-sdk-fidoiot.git"
#SRC_URI[sha256sum] = "c821a9afa9f987ac829fb3a8dd72122c3c612b0c25c9c0fe03201f7e1081f183"

SRC_URI += "\
    file://ecdsa384privkey.dat \
    file://ecdsa384privkey.pem \
    file://manufacturer_addr.bin \
    file://manufacturer_sn.bin \
"

S = "${WORKDIR}/git"

TOOLCHAIN = "POKY-GLIBC"
APP_NAME = "c_code_sdk"
DEPENDS += "openssl gcc curl"
PREFERRED_VERSION_curl = "7.86"
#PREFERRED_VERSION_curl = "7.6"

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
CUR_DIR=$(pwd)
cd "${WORKDIR}/git"

cd ${CUR_DIR}/../

if [ ! -d "safestringlib" ] ; then
	git clone https://github.com/intel/safestringlib.git
    export SAFESTRING_ROOT=${CUR_DIR}/../safestringlib
    cd ${SAFESTRING_ROOT}
    rm -rf makefile
    sed -i '/mmitigate-rop/d' ./CMakeLists.txt
    cmake .
    make
    cp libsafestring_static.a libsafestring.a
fi
export SAFESTRING_ROOT=${CUR_DIR}/../safestringlib

CUR_DIR=$(pwd)
cd "${WORKDIR}/git"
cd ${CUR_DIR}/../
if [ ! -d "tinycbor" ] ; then
	git clone https://github.com/intel/tinycbor.git --branch v0.5.3
fi
export TINYCBOR_ROOT=${CUR_DIR}/../tinycbor
cd ${TINYCBOR_ROOT}
make

echo " >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< "
echo "${S}":"${DA}":"${BUILD}":"${AES_MODE}"
echo " >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>><<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< "
#Remove -DRESUSE=true at the time of production
cd ${S}
cmake -DREUSE=true -DHTTPPROXY=${HTTPPROXY} -DBUILD=${BUILD} -DDA=${DA} -DAES_MODE=${AES_MODE} -DOPTIMIZE=1 .
make -j$(nproc)
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