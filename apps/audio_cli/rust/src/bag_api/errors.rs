use crate::util::c_str_to_string;

use super::abi::{
    bag_error_code_message, bag_validation_issue_message, BagErrorCode, BagValidationIssue,
};

pub(super) fn validation_issue_message(issue: BagValidationIssue) -> String {
    let raw = unsafe {
        // SAFETY: The native function returns a process-lifetime message pointer
        // for any validation issue code.
        bag_validation_issue_message(issue)
    };
    c_str_to_string(raw)
}

pub(super) fn error_code_message(code: BagErrorCode) -> String {
    let raw = unsafe {
        // SAFETY: The native function returns a process-lifetime message pointer
        // for any error code value.
        bag_error_code_message(code)
    };
    c_str_to_string(raw)
}
