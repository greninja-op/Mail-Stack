use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum SyncStage {
    Idle,
    Connecting,
    Fetching,
    Saving,
    Processing,
    Finalizing,
    Succeeded,
    Failed,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum SyncProgress {
    Idle,
    Running {
        stage: SyncStage,
        messages_processed: u32,
        total_estimate: Option<u32>,
    },
    Succeeded {
        messages_processed: u32,
    },
    Failed {
        message: String,
    },
}

/// Cap at 0.99 while running; Done = 1.0. Never revise total mid-run.
pub fn display_fraction(progress: &SyncProgress) -> f64 {
    match progress {
        SyncProgress::Idle => 0.0,
        SyncProgress::Running {
            messages_processed,
            total_estimate,
            ..
        } => match total_estimate {
            Some(total) if *total > 0 => {
                ((*messages_processed as f64) / (*total as f64)).min(0.99)
            }
            _ => 0.0,
        },
        SyncProgress::Succeeded { .. } => 1.0,
        SyncProgress::Failed { .. } => 0.0,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn running_caps_at_99() {
        let p = SyncProgress::Running {
            stage: SyncStage::Fetching,
            messages_processed: 100,
            total_estimate: Some(100),
        };
        assert!((display_fraction(&p) - 0.99).abs() < f64::EPSILON);
    }
}
