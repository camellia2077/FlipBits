#if FLIPBITS_ENABLE_ASYNC_ENCODE_JOB
bag_error_code EnsureEncodeJobFollowResult(bag_encode_job* job) {
  if (job == nullptr || !job->operation) {
    return BAG_INVALID_ARGUMENT;
  }
  bag::EncodedPcmFollowResult result{};
  const bag::ErrorCode code = job->operation->TakeResult(&result);
  return ToApiCode(code);
}
#else
bag_error_code EnsureEncodeJobFollowResult(bag_encode_job* job) {
  return job == nullptr ? BAG_INVALID_ARGUMENT : BAG_NOT_IMPLEMENTED;
}
#endif
