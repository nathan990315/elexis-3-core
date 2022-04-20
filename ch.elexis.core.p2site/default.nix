# Helper to add Elexis to a NixOS system, must be installed using the following steps
# mvn -V clean -f verify elexis-master/pom.xml
# nix-build ; nix-env -i result/
with (import <nixpkgs> {});
let pname = "Elexis";
  version = "3.10";
  exec = "Elexis3";
  icon = "icon";
in 
stdenv.mkDerivation {

  name = "${pname}-${version}";
  icon = "${icon}";
  longDescription = ''
        Elexis: Eine umffassende Lösung für eine elektronische Krankenführung in Schweizer Arztpraxen
        Entwickelt durch Gerry Weirich, Marco Descher, Thomas Huster,
        Niklaus Giger und viele Andere.
        Professionnelle Unterstützung wird durch die Medelexis AG angeboten
  '';
  homepage    = "https://elexis.info/";
  changelog   = "https://github.com/elexis/elexis-3-core/blob/master/Changelog";
#  license     = licenses.epl20;
#    maintainers = with maintainers; [ ngiger ];
#    platforms   = platforms.unix;
  inherit gcc coreutils perl glibc busybox makeWrapper stdenv patchelf  ;
  nativeBuildInputs = [ unzip  ];
  src = target/products/Elexis3-linux.gtk.x86_64.zip;

  desktopItem = makeDesktopItem {
    name = "${pname}-${version}";
    exec = "${exec}";
    icon = "${icon}";
    comment = "Elexis: Eine umffassende Lösung für eine elektronische Krankenführung in Schweizer Arztpraxen";
    desktopName = "Elexis3";
    genericName = "Elexis3";
    categories = "Office;";
  };

  buildInputs = [
    cairo
    fontconfig
    freetype
    glib
    gsettings-desktop-schemas
    gtk3
    makeWrapper
    openjdk11
    unzip
    webkit
    openjfx11 # openjfx
    webkitgtk
    xorg.libX11
    xorg.libXrender
    xorg.libXtst
    zlib
  ] ;
  unpackPhase = ''
    mkdir -p "$out/app" 
    unzip -qd "$out/app" $src
  '';
  
  phases = [ "unpackPhase" "buildPhase" ] ;
  ldPath = "${lib.makeLibraryPath ([ glib gtk3 xorg.libXtst openjfx11 webkit webkitgtk ])}";
  setElexisEnv =  writeScriptBin "setElexisEnv" ''
      #!${pkgs.stdenv.shell}
#       # Generate a small helper script for running RCPTT test via mvn rpctt plugin
	  export GDK_BACKEND=x11
      export PATH='${openjdk11}/bin'
	  export LD_LIBRARY_PATH='$ldPath'
      export XDG_DATA_DIRS='${gsettings-desktop-schemas}/share/gsettings-schemas/gsettings-desktop-schemas-40.0'
      export GIO_EXTRA_MODULES='${glib_networking}/lib/gio/modules'
      export JAVA_HOME='${pkgs.openjdk11}'
  '';
  buildPhase = ''
    # Unpack tarball.
    mkdir -p $out/bin
    cp -v $setElexisEnv/bin/setElexisEnv  $out/bin
    # Patch Elexis3 binary AND the embedded java from the JRE
    interpreter=$(echo ${stdenv.glibc.out}/lib/ld-linux*.so.2)
    patchelf \
    --set-interpreter $interpreter $out/app/Elexis3 \
    --set-rpath $ldPath \
    $out/app/Elexis3
    patchelf --set-interpreter $interpreter $out/app/plugins/*/jre/bin/java
    
    # ensure eclipse.ini does not try to use a justj jvm, as those aren't compatible with nix
    sed -i '/-vm/d' $out/app/Elexis3.ini
    sed -i '/org.eclipse.justj/d' $out/app/Elexis3.ini
    # does not seem to work with webbrowser find $out -name "*justj*" -type d | xargs rm -rf
    
    makeWrapper $out/app/Elexis3 $out/bin/${exec} \
	  --prefix GDK_BACKEND : x11 \
      --prefix PATH : ${openjdk11}/bin \
      --prefix LD_LIBRARY_PATH : $ldPath \
      --prefix XDG_DATA_DIRS : "$GSETTINGS_SCHEMAS_PATH" \
      --prefix GIO_EXTRA_MODULES : "${glib_networking}/lib/gio/modules" \
      --add-flags "-nl de_CH -os linux -ws gtk -arch x86_64 -consoleLog"
    ${gnugrep}/bin/egrep 'bash|export' $out/bin/${exec} > $out/bin/get_exports
    chmod +x $out/bin/get_exports
    # Create desktop items
    mkdir -p $out/share/applications
    cp $desktopItem/share/applications/* $out/share/applications
  '';
  system = builtins.currentSystem;
}
