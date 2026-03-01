find_package(PkgConfig REQUIRED)
pkg_check_modules(SNDFILE REQUIRED IMPORTED_TARGET sndfile)
pkg_check_modules(KISSFFT REQUIRED IMPORTED_TARGET kissfft)

set(third_party_libs
    PkgConfig::SNDFILE
    PkgConfig::KISSFFT
)
