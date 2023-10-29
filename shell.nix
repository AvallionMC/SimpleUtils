{pkgs ? import <nixpkgs> {}}: let
  java = pkgs.openjdk11;
in
  pkgs.mkShell {
    buildInputs = with pkgs; [java gradle maven];
    shellHook = ''
      export JAVA_HOME=${java.home}
    '';
  }
