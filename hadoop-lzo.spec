Name: hadoop-lzo
Summary: GPL Compression Libraries for Hadoop (jar)
Version: 0.6.0
Release: 1
License: GPL
Source0: %{name}-%{version}.tar.gz
Group: Development/Libraries
Buildroot: %{_tmppath}/%{name}-%{version}
BuildRequires: ant, ant-nodeps, gcc-c++, lzo-devel
Requires: lzo, hadoop
%define _use_internal_dependency_generator 0

%define hadoop_home /usr/lib/hadoop

%description
GPLed Compression Libraries for Hadoop, built at $DATE on $HOST

%package native
Summary: GPL Compression Libraries for Hadoop (native)
Group: Development/Libraries
%description native
GPLed Compression Libraries for Hadoop, built at $DATE on $HOST

%prep
%setup -n %{name}-%{version}

# Requires: exclude libjvm.so since it generally isn't installed
# on the system library path, and we don't want to have to install
# with --nodeps
# RHEL doesn't have nice macros. Oh well. Do it old school.
%define our_req_script %{name}-find-req.sh
cat <<__EOF__ > %{our_req_script}
#!/bin/sh
%{__find_requires} | grep -v libjvm
__EOF__
%define __find_requires %{_builddir}/%{name}-%{version}/%{our_req_script}
chmod +x %{__find_requires}

%build

ant -Dname=%{name} -Dversion=%{version} \
    clean package -Dhadoop.verison=${hadoop_version} \
    -Drepo.maven.org=${nexus_proxy_url}

%install
mkdir -p $RPM_BUILD_ROOT/%{hadoop_home}/lib
install -m644 $RPM_BUILD_DIR/%{name}-%{version}/build/%{name}-%{version}.jar $RPM_BUILD_ROOT/%{hadoop_home}/lib/
%__rm -rf $RPM_BUILD_DIR/%{name}-%{version}/build/%{name}-%{version}/lib/native/%{name}-%{version}.jar
rsync -av --no-t $RPM_BUILD_DIR/%{name}-%{version}/build/%{name}-%{version}/lib/native/ $RPM_BUILD_ROOT/%{hadoop_home}/lib/native/
%__rm -rf $RPM_BUILD_ROOT/%{hadoop_home}/lib/native/lib

%files native
%defattr(-,root,root,-)
%{hadoop_home}/lib/native/

%files
%defattr(-,root,root,-)
%{hadoop_home}/lib/%{name}-%{version}.jar

%clean
%__rm -rf $RPM_BUILD_ROOT
