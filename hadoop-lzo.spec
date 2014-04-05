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

%build
 cd hadoop-lzo-%{version} 
 ant -Dversion=%{version} clean tar -Dhadoop.verison=${hadoop_version} -Drepo.maven.org=${nexus_proxy_url}
 cp $RPM_BUILD_DIR/%{name}-%{version}/build/hadoop-lzo-%{version}.tar.gz $RPM_SOURCE_DIR/.

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
