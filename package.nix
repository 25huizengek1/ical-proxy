{
  lib,
  version,
  buildGradleApplication,
  jdk,
}:

buildGradleApplication {
  pname = "ical-proxy";
  version = version;
  src = ./.;

  inherit jdk;

  meta = with lib; {
    description = "ICalendar proxy that caches/filters any valid ICalendar url efficiently using a Redis cache";

    sourceProvenance = with sourceTypes; [
      fromSource
      binaryBytecode
    ];

    platforms = platforms.unix;
  };
}
