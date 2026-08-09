use super::abi::AudioIoOwnedString;

pub(super) fn owned_string_to_string(raw: &AudioIoOwnedString) -> String {
    if raw.data.is_null() || raw.size == 0 {
        String::new()
    } else {
        let bytes = raw_slice(raw.data.cast_const().cast::<u8>(), raw.size);
        String::from_utf8_lossy(bytes).into_owned()
    }
}

pub(super) fn raw_bytes_to_vec(data: *const u8, size: usize) -> Vec<u8> {
    raw_slice(data, size).to_vec()
}

pub(super) fn raw_slice_to_vec<T: Copy>(data: *const T, size: usize) -> Vec<T> {
    raw_slice(data, size).to_vec()
}

pub(super) fn raw_slice<'a, T>(data: *const T, size: usize) -> &'a [T] {
    if data.is_null() || size == 0 {
        &[]
    } else {
        unsafe {
            // SAFETY: Callers only pass pointers/lengths obtained from the native
            // `audio_io` API, which guarantees readable contiguous storage for the
            // reported element count until the matching free function runs.
            std::slice::from_raw_parts(data, size)
        }
    }
}
