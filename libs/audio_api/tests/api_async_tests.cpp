#include "api_test_support.h"

namespace api_tests {

void RegisterApiAsyncTests(test::Runner& runner) {
  RegisterApiAsyncEncodeTests(runner);
  RegisterApiAsyncDecodeTests(runner);
}

}  // namespace api_tests
