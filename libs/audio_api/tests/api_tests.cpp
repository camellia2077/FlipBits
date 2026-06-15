#include "api_test_support.h"
#include "bag_api.h"

int main() {
  test::Runner runner;
  api_tests::RegisterApiSyncTests(runner);
  api_tests::RegisterApiAsyncTests(runner);
  api_tests::RegisterApiFlashTests(runner);
  api_tests::RegisterApiVoiceFxTests(runner);
  return runner.Run();
}
