#!/bin/sh

  CUR_DIR=`pwd`
  PKG_NAME="hadoop-lzo"
  BASEDIR="$( cd "$( dirname "$0" )" && pwd )"
  osname="$(lsb_release -si | tr '[:upper:]' '[:lower:]'| cut -d" " -f1)$(lsb_release -sr | cut -d. -f1)"

  [[ -z "${VERSION}" ]] && VERSION="0.5.0"
  [[ -z "${RELEASE}" ]] && RELEASE="1"

  BUILD_DIR=${BUILD_DIR:-$BASEDIR/build-$PKG_NAME}

  rm -rf ${BUILD_DIR} && mkdir -p ${BUILD_DIR}

  OUTPUT_DIR=${OUTPUT_DIR:-$CUR_DIR/output-$PKG_NAME}
  RPM_BUILD_DIR=${BUILD_DIR}/rpm

  ##mkdir all the rpm required dirs
  mkdir -p ${OUTPUT_DIR}
  mkdir -p $RPM_BUILD_DIR/{BUILD,SPECS,SOURCES,RPMS,SRPMS,INSTALL}

  buildHadooplzo() {
    unset JAVA_HOME
    export JAVA_HOME=${JAVA_HOME_32BIT}
    echo "JAVA_HOME for 32bit HADOOP = $JAVA_HOME"
    export CFLAGS=""
    export CXXFLAGS=""
    export CFLAGS=-m32
    export CXXFLAGS=-m32
    $ANT_HOME/bin/ant -Dversion=$VERSION clean tar
  }

  cp ${BASEDIR}/build/${PKG_NAME}-${VERSION}.tar.gz $RPM_BUILD_DIR/SOURCES/
  cp ${PKG_NAME}.spec $RPM_BUILD_DIR/SPECS

  build_hadoop32lzorpm() {
    unset target
    export target='--target i386'
    unset JAVA_HOME
    export JAVA_HOME=${JAVA_HOME_32BIT}
    export PATH=$JAVA_HOME_32BIT:$PATH
    echo "PATH = $PATH"
    echo "JAVA_HOME for 32bit HADOOP = $JAVA_HOME"
    export CFLAGS="-m32"
    export CXXFLAGS="-m32" 
    rpmbuild -ba ${target} --define "_topdir ${RPM_BUILD_DIR} " ${RPM_BUILD_DIR}/SPECS/hadoop-lzo.spec
    cp ${RPM_BUILD_DIR}/RPMS/i386/hadoop-lzo*.i386.rpm ${OUTPUT_DIR}/
    if [ $osname = "suse11" ];then
      BUILD_ROOT="${RPM_BUILD_DIR}/RPMS"
      echo hadoop-lzo > ${BUILD_ROOT}/baselibs.conf
      rm -rf /usr/src/packages/RPMS/ia64/*
      rm -rf /usr/src/packages/RPMS/x86_64/*
      /usr/lib/build/mkbaselibs -c /usr/lib/build/baselibs_global.conf -c ${BUILD_ROOT}/baselibs.conf ${BUILD_ROOT}/i386/hadoop-lzo*.i386.rpm
      cp -p /usr/src/packages/RPMS/x86_64/hadoop-lzo*.rpm ${OUTPUT_DIR}/
    fi
  }

  build_hadoop64lzorpm() {
    unset target
    export target=''
    unset JAVA_HOME
    export JAVA_HOME=${JAVA_HOME_64BIT}
    echo "JAVA_HOME for 64bit HADOOP = $JAVA_HOME"
    export CFLAGS=""
    export CXXFLAGS=""
    export CFLAGS=-m64
    export CXXFLAGS=-m64
    rpmbuild -ba  --define "_topdir ${RPM_BUILD_DIR} " ${RPM_BUILD_DIR}/SPECS/hadoop-lzo.spec
    cp ${RPM_BUILD_DIR}/RPMS/x86_64/hadoop-lzo*.rpm ${OUTPUT_DIR}/
  }

  buildHadooplzo
  build_hadoop32lzorpm
  build_hadoop64lzorpm
