#!/bin/sh

  CUR_DIR=`pwd`
  PKG_NAME="hadoop-lzo"
  BASEDIR="$( cd "$( dirname "$0" )" && pwd )"
  osname="$(lsb_release -si | tr '[:upper:]' '[:lower:]'| cut -d" " -f1)$(lsb_release -sr | cut -d. -f1)"

  [[ -z "${VERSION}" ]] && VERSION="0.6.0"
  [[ -z "${RELEASE}" ]] && RELEASE="1"
  [[ -z "${hadoop_version}" ]] && echo "hadoop_version not set"
  [[ -z "${nexus_proxy_url}" ]] && echo "nexus_proxy_url not set"
  [[ -z "${JAVA_HOME}" ]] && echo "JAVA_HOME not set"

  BUILD_DIR=${BUILD_DIR:-$BASEDIR/build-$PKG_NAME}

  rm -rf ${BUILD_DIR} && mkdir -p ${BUILD_DIR}

  OUTPUT_DIR=${OUTPUT_DIR:-$CUR_DIR/output-$PKG_NAME}
  RPM_BUILD_DIR=${BUILD_DIR}/rpm

  ##mkdir all the rpm required dirs
  mkdir -p ${OUTPUT_DIR}
  mkdir -p $RPM_BUILD_DIR/{BUILD,SPECS,SOURCES,RPMS,SRPMS,INSTALL}

  buildHadooplzo() {
    #64Bit Binary tarball
    $ANT_HOME/bin/ant -Dversion=$VERSION \
        -Dhadoop.verison=${hadoop_version} \
        -Drepo.maven.org=${nexus_proxy_url} clean tar
  }

  copyHadooplzoArtifacts() {
    tar xvzf ../${PKG_NAME}-${VERSION}.tar.gz ../${PKG_NAME}-${VERSION} --exclude=.git*
    cp ${BASEDIR} ${PKG_NAME}-${VERSION}.tar.gz $RPM_BUILD_DIR/SOURCES/
    cp ${BASEDIR}/${PKG_NAME}.spec $RPM_BUILD_DIR/SPECS
  }

  build_hadooplzorpm() {
    rpmbuild -ba  --define "_topdir ${RPM_BUILD_DIR} " ${RPM_BUILD_DIR}/SPECS/hadoop-lzo.spec
    cp ${RPM_BUILD_DIR}/RPMS/x86_64/hadoop-lzo*.rpm ${OUTPUT_DIR}/
  }

  copyHadooplzoArtifacts
  build_hadooplzorpm
