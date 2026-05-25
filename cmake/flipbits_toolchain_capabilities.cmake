include_guard(GLOBAL)

set(FLIPBITS_HAS_STD_MODULE_PROVIDER 0)
if(TARGET __CMAKE::CXX23)
    set(FLIPBITS_HAS_STD_MODULE_PROVIDER 1)
elseif(DEFINED CMAKE_CXX_COMPILER_IMPORT_STD AND 23 IN_LIST CMAKE_CXX_COMPILER_IMPORT_STD)
    set(FLIPBITS_HAS_STD_MODULE_PROVIDER 1)
endif()

function(flipbits_apply_toolchain_capabilities target_name)
    target_compile_definitions(
        ${target_name}
        PUBLIC
            FLIPBITS_HAS_STD_MODULE_PROVIDER=$<IF:$<BOOL:${FLIPBITS_HAS_STD_MODULE_PROVIDER}>,1,0>
    )

    if(FLIPBITS_HAS_STD_MODULE_PROVIDER AND TARGET __CMAKE::CXX23)
        set_property(TARGET ${target_name} PROPERTY CXX_MODULE_STD ON)
    endif()
endfunction()
