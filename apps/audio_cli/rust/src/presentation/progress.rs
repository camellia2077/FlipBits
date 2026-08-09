use crate::bag_api::{EncodeOperationPhase, EncodeOperationProgress};
use crate::TransportMode;
use std::io::{self, IsTerminal, Write};

pub struct EncodeProgressBar {
    enabled: bool,
    last_render_width: usize,
}

impl EncodeProgressBar {
    pub fn new() -> Self {
        Self {
            enabled: io::stderr().is_terminal(),
            last_render_width: 0,
        }
    }

    pub fn update(&mut self, mode: TransportMode, progress: EncodeOperationProgress) {
        if !self.enabled {
            return;
        }

        let percent = (progress.overall_progress_0_to_1 * 100.0)
            .round()
            .clamp(0.0, 100.0) as usize;
        let filled = ((percent * 24) / 100).min(24);
        let bar = format!(
            "{}{}",
            "#".repeat(filled),
            "-".repeat(24usize.saturating_sub(filled))
        );
        let line = format!(
            "[{bar}] {percent:>3}% {} ({mode})",
            phase_label(progress.phase)
        );

        let padding = self.last_render_width.saturating_sub(line.len());
        let mut stderr = io::stderr().lock();
        let _ = write!(stderr, "\r{line}{}", " ".repeat(padding));
        let _ = stderr.flush();
        self.last_render_width = line.len();
    }

    pub fn finish(&mut self) {
        if !self.enabled {
            return;
        }

        let mut stderr = io::stderr().lock();
        let _ = write!(stderr, "\r{}\r", " ".repeat(self.last_render_width));
        let _ = stderr.flush();
        self.last_render_width = 0;
    }
}

fn phase_label(phase: EncodeOperationPhase) -> &'static str {
    match phase {
        EncodeOperationPhase::PreparingInput => "Preparing input",
        EncodeOperationPhase::RenderingPcm => "Rendering PCM",
        EncodeOperationPhase::Postprocessing => "Post-processing",
        EncodeOperationPhase::Finalizing => "Finalizing",
    }
}

#[cfg(test)]
mod tests {
    use super::phase_label;
    use crate::bag_api::EncodeOperationPhase;

    #[test]
    fn phase_labels_are_stable() {
        assert_eq!(
            phase_label(EncodeOperationPhase::PreparingInput),
            "Preparing input"
        );
        assert_eq!(
            phase_label(EncodeOperationPhase::RenderingPcm),
            "Rendering PCM"
        );
        assert_eq!(
            phase_label(EncodeOperationPhase::Postprocessing),
            "Post-processing"
        );
        assert_eq!(phase_label(EncodeOperationPhase::Finalizing), "Finalizing");
    }
}
