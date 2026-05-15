use std::ffi::CStr;
use std::os::raw::c_char;

pub fn c_str_to_string(raw: *const c_char) -> String {
    if raw.is_null() {
        "unknown".to_string()
    } else {
        c_str(raw).to_string_lossy().into_owned()
    }
}

fn c_str<'a>(raw: *const c_char) -> &'a CStr {
    unsafe {
        // SAFETY: Callers only pass pointers obtained from the native FFI layer,
        // which guarantees a readable, NUL-terminated C string for the duration
        // of the conversion.
        CStr::from_ptr(raw)
    }
}
