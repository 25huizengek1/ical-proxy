{
  description = "Ical proxy (made with messy Google Calendars in mind)";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-parts.url = "github:hercules-ci/flake-parts";

    treefmt-nix = {
      url = "github:numtide/treefmt-nix";
      inputs.nixpkgs.follows = "nixpkgs";
    };

    build-gradle-application = {
      url = "github:raphiz/buildGradleApplication";
      inputs.nixpkgs.follows = "nixpkgs";
    };
  };

  outputs =
    inputs@{ self, flake-parts, ... }:

    flake-parts.lib.mkFlake { inherit inputs; } {
      systems = [
        "x86_64-linux"
        "aarch64-linux"
        "aarch64-darwin"
        "x86_64-darwin"
      ];

      imports = [
        inputs.treefmt-nix.flakeModule
      ];

      perSystem =
        { system, ... }:

        let
          pkgs = import inputs.nixpkgs {
            inherit system;
            allowUnfree = true;
            overlays = [
              inputs.build-gradle-application.overlays.default
              (
                final: _prev:
                let
                  j = final.jdk23;
                in
                {
                  jdk = j;
                  java = j;
                  gradle = _prev.gradle.override { java = j; };
                }
              )
            ];
          };
          version = self.shortRev or "dirty";
        in
        {
          _module.args.pkgs = pkgs;

          treefmt = {
            programs.nixfmt.enable = true;
            programs.shellcheck.enable = true;
            programs.ruff.enable = true;
          };

          devShells.default = pkgs.mkShellNoCC {
            packages = with pkgs; [
              gradle
              jdk
              python313
            ];

            env = {
              GRADLE_JAVA_HOME = "${pkgs.jdk}";
            };

            shellHook = '''';
          };

          packages.default =
            (pkgs.callPackage ./package.nix {
              inherit version;
              inherit (pkgs) jdk;
            }).overrideAttrs
              {
                env.JAVA_HOME = "${pkgs.jdk}";
                env.GRADLE_JAVA_HOME = "${pkgs.jdk}";
              };
        };

      flake = {
      };
    };
}
