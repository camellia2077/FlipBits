#include "audio_runtime.h"

#include "test_framework.h"

namespace {

void TestRuntimeLoadPlayPauseResumeComplete() {
    auto state = audio_runtime_load(200, 100);
    test::AssertEq(state.phase, AUDIO_RUNTIME_PLAYBACK_IDLE, "Load should initialize runtime in idle state.");
    test::AssertEq(state.total_samples, 200, "Load should preserve total samples.");
    test::AssertEq(state.sample_rate_hz, 100, "Load should preserve sample rate.");

    state = audio_runtime_play_started(state);
    test::AssertEq(state.phase, AUDIO_RUNTIME_PLAYBACK_PLAYING, "Play should move runtime into playing state.");

    state = audio_runtime_progress(state, 75);
    test::AssertEq(state.played_samples, 75, "Progress should advance played samples.");
    test::AssertTrue(
        audio_runtime_progress_fraction(&state) > 0.37f && audio_runtime_progress_fraction(&state) < 0.38f,
        "Progress fraction should reflect the played sample ratio.");

    state = audio_runtime_paused(state);
    test::AssertEq(state.phase, AUDIO_RUNTIME_PLAYBACK_PAUSED, "Pause should move runtime into paused state.");

    state = audio_runtime_resumed(state);
    test::AssertEq(state.phase, AUDIO_RUNTIME_PLAYBACK_PLAYING, "Resume should return runtime to playing state.");

    state = audio_runtime_completed(state);
    test::AssertEq(state.phase, AUDIO_RUNTIME_PLAYBACK_COMPLETED, "Complete should move runtime into completed state.");
    test::AssertEq(state.played_samples, 200, "Complete should advance playback to the end.");
}

void TestRuntimePausedScrubCommit() {
    auto state = audio_runtime_load(120, 60);
    state = audio_runtime_play_started(state);
    state = audio_runtime_progress(state, 30);
    state = audio_runtime_paused(state);

    state = audio_runtime_scrub_started(state);
    state = audio_runtime_scrub_changed(state, 45);
    test::AssertEq(state.is_scrubbing, 1, "Scrub change should keep runtime in scrubbing mode.");
    test::AssertEq(state.scrub_target_samples, 45, "Scrub change should update preview target.");
    test::AssertEq(state.played_samples, 30, "Scrub preview should not overwrite the committed playback position.");

    state = audio_runtime_scrub_committed(state);
    test::AssertEq(state.phase, AUDIO_RUNTIME_PLAYBACK_PAUSED, "Commit from paused should stay paused.");
    test::AssertEq(state.is_scrubbing, 0, "Scrub commit should clear scrubbing mode.");
    test::AssertEq(state.played_samples, 45, "Scrub commit should move the committed playback position.");
}

void TestRuntimePlayingScrubCommitResume() {
    auto state = audio_runtime_load(100, 50);
    state = audio_runtime_play_started(state);
    state = audio_runtime_progress(state, 20);

    state = audio_runtime_scrub_started(state);
    test::AssertEq(state.phase, AUDIO_RUNTIME_PLAYBACK_PAUSED, "Scrub start while playing should pause the runtime state.");
    test::AssertEq(state.resume_after_scrub, 1, "Scrub start while playing should remember auto-resume intent.");

    const auto progress_while_scrubbing = audio_runtime_progress(state, 70);
    test::AssertEq(
        progress_while_scrubbing.played_samples,
        state.played_samples,
        "Backend progress should be ignored while the user is scrubbing.");

    state = audio_runtime_scrub_changed(state, 80);
    state = audio_runtime_scrub_committed(state);
    test::AssertEq(state.resume_after_scrub, 0, "Scrub commit should clear the auto-resume intent latch.");
    test::AssertEq(state.played_samples, 80, "Scrub commit should keep the selected target.");

    state = audio_runtime_resumed(state);
    test::AssertEq(state.phase, AUDIO_RUNTIME_PLAYBACK_PLAYING, "Resume after scrub should return to playing state.");
    test::AssertEq(state.played_samples, 80, "Resume after scrub should continue from the committed target.");
}

void TestRuntimeClampAndFractionHelpers() {
    test::AssertEq(audio_runtime_clamp_samples(100, -5), 0, "Clamp should floor negative sample positions.");
    test::AssertEq(audio_runtime_clamp_samples(100, 120), 100, "Clamp should cap sample positions at the end.");
    test::AssertEq(audio_runtime_fraction_to_samples(200, -1.0f), 0, "Fraction helper should floor negative fractions.");
    test::AssertEq(audio_runtime_fraction_to_samples(200, 1.0f), 200, "Fraction helper should map 1.0 to the full sample count.");
}

void TestRuntimeCompletedScrubBackBecomesPaused() {
    auto state = audio_runtime_load(90, 45);
    state = audio_runtime_completed(state);
    state = audio_runtime_scrub_started(state);
    state = audio_runtime_scrub_changed(state, 12);
    state = audio_runtime_scrub_committed(state);

    test::AssertEq(state.phase, AUDIO_RUNTIME_PLAYBACK_PAUSED, "Dragging back from completed should land in paused state.");
    test::AssertEq(state.played_samples, 12, "Dragging back from completed should update the playback position.");
}

void TestRuntimeZeroTotalsStaySafe() {
    auto state = audio_runtime_load(0, 0);
    test::AssertEq(state.phase, AUDIO_RUNTIME_PLAYBACK_IDLE, "Zero-length loads should stay idle.");
    test::AssertEq(audio_runtime_progress_fraction(&state), 0.0f, "Zero-length loads should not report progress.");
    test::AssertEq(audio_runtime_elapsed_ms(&state), 0LL, "Zero sample rate should yield zero elapsed time.");
    test::AssertEq(audio_runtime_total_ms(&state), 0LL, "Zero sample rate should yield zero total time.");

    state = audio_runtime_scrub_started(state);
    test::AssertEq(state.is_scrubbing, 0, "Zero-length sessions should not enter scrubbing mode.");
}

}  // namespace

int main() {
    test::Runner runner;
    runner.Add("Runtime.LoadPlayPauseResumeComplete", TestRuntimeLoadPlayPauseResumeComplete);
    runner.Add("Runtime.PausedScrubCommit", TestRuntimePausedScrubCommit);
    runner.Add("Runtime.PlayingScrubCommitResume", TestRuntimePlayingScrubCommitResume);
    runner.Add("Runtime.ClampAndFractionHelpers", TestRuntimeClampAndFractionHelpers);
    runner.Add("Runtime.CompletedScrubBackBecomesPaused", TestRuntimeCompletedScrubBackBecomesPaused);
    runner.Add("Runtime.ZeroTotalsStaySafe", TestRuntimeZeroTotalsStaySafe);
    return runner.Run();
}
