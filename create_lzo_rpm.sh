!#/bin/bash
CUR_DIR=`pwd`
PKG_NAME="hadoop-lzo"
BASEDIR="$( cd "$( dirname "$0" )" && pwd )"

[[ -z "${VERSION}" ]] && VERSION="0.5.0"
[[ -z "${RELEASE}" ]] && RELEASE="1"

BUILD_DIR=${BUILD_DIR:-$BASEDIR/build-$PKG_NAME}

rm -rf ${BUILD_DIR} && mkdir -p ${BUILD_DIR}

OUTPUT_DIR=${OUTPUT_DIR:-$CUR_DIR/output-$PKG_NAME}
RPM_BUILD_DIR=${BUILD_DIR}/rpm

##mkdir all the rpm required dirs
mkdir -p $RPM_BUILD_DIR/{BUILD,SPECS,SOURCES,RPMS,SRPMS,INSTALL}

$ANT_HOME/bin/ant -Dversion=$VERSION clean tar
#ant -Dversion=$VERSION clean tar

cp ${BASEDIR}/build/${PKG_NAME}-${VERSION}.tar.gz $RPM_BUILD_DIR/SOURCES/
cp ${PKG_NAME}.spec $RPM_BUILD_DIR/SPECS

rpmbuild -ba --target i386 --define "_topdir ${RPM_BUILD_DIR} " ${RPM_BUILD_DIR}/SPECS/hadoop-lzo.spec
