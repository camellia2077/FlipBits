bag_error_code bag_create_encode_operation(const bag_encoder_config* config,
                                           const char* text,
                                           bag_encode_operation** out_operation) {
  if (config == nullptr || text == nullptr || out_operation == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }

  *out_operation = nullptr;
  if (bag_validate_encode_request(config, text) != BAG_VALIDATION_OK) {
    return BAG_INVALID_ARGUMENT;
  }

  try {
    auto* operation = new (std::nothrow) bag_encode_operation{};
    if (operation == nullptr) {
      return BAG_INTERNAL;
    }
    const bag::CoreConfig core_config = ToCoreConfig(
        config->sample_rate_hz, config->frame_samples,
        config->enable_diagnostics, config->mode, config->flash_signal_profile,
        config->flash_voicing_flavor, config->reserved);
    operation->operation = bag::CreateEncodeOperation(core_config, text);
    if (!operation->operation) {
      delete operation;
      return BAG_INTERNAL;
    }
    *out_operation = operation;
    return BAG_OK;
  } catch (...) {
    *out_operation = nullptr;
    return BAG_INTERNAL;
  }
}

bag_error_code bag_run_encode_operation(bag_encode_operation* operation) {
  if (operation == nullptr || !operation->operation) {
    return BAG_INVALID_ARGUMENT;
  }
  try {
    return ToApiCode(operation->operation->Run());
  } catch (...) {
    return BAG_INTERNAL;
  }
}

bag_error_code bag_cancel_encode_operation(bag_encode_operation* operation) {
  if (operation == nullptr || !operation->operation) {
    return BAG_INVALID_ARGUMENT;
  }
  try {
    return ToApiCode(operation->operation->Cancel());
  } catch (...) {
    return BAG_INTERNAL;
  }
}

bag_error_code bag_get_encode_operation_work_plan(
    const bag_encode_operation* operation,
    bag_encode_operation_work_plan* out_work_plan) {
  if (operation == nullptr || !operation->operation || out_work_plan == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }
  try {
    FillEncodeOperationWorkPlan(operation->operation->WorkPlan(), out_work_plan);
    return BAG_OK;
  } catch (...) {
    *out_work_plan = {};
    return BAG_INTERNAL;
  }
}

bag_error_code bag_pump_encode_operation(
    bag_encode_operation* operation,
    bag_encode_operation_pump_budget budget,
    int* out_did_progress) {
  if (operation == nullptr || !operation->operation || out_did_progress == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }
  try {
    bool did_progress = false;
    const bag::EncodePumpBudget core_budget{
        .max_work_units = budget.max_work_units,
        .max_wall_time_ms = budget.max_wall_time_ms,
    };
    const bag::ErrorCode code =
        operation->operation->Pump(core_budget, &did_progress);
    *out_did_progress = did_progress ? 1 : 0;
    return ToApiCode(code);
  } catch (...) {
    *out_did_progress = 0;
    return BAG_INTERNAL;
  }
}

bag_error_code bag_poll_encode_operation(
    const bag_encode_operation* operation,
    bag_encode_operation_progress* out_progress) {
  if (operation == nullptr || !operation->operation || out_progress == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }
  try {
    FillEncodeOperationProgress(operation->operation->Snapshot(), out_progress);
    return BAG_OK;
  } catch (...) {
    *out_progress = {};
    return BAG_INTERNAL;
  }
}

bag_error_code bag_take_encode_operation_result(
    const bag_encode_operation* operation,
    bag_encode_result* out_result) {
  if (operation == nullptr || !operation->operation || out_result == nullptr) {
    return BAG_INVALID_ARGUMENT;
  }

  try {
    bag::EncodedPcmFollowResult result{};
    const bag::ErrorCode take_code = operation->operation->TakeResult(&result);
    if (take_code != bag::ErrorCode::kOk) {
      ClearApiEncodeResult(out_result);
      return ToApiCode(take_code);
    }
    return CopyStructuredEncodeResultToApi(result, out_result);
  } catch (...) {
    ClearApiEncodeResult(out_result);
    return BAG_INTERNAL;
  }
}

void bag_destroy_encode_operation(bag_encode_operation* operation) {
  delete operation;
}
