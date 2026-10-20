use crate::mailbox::ThreadSummary;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum CategoryGroup {
    NeedsAttention,
    Promotions,
    Updates,
    WorkAndLearning,
    EverythingElse,
}

impl CategoryGroup {
    pub fn title(self) -> &'static str {
        match self {
            Self::NeedsAttention => "Needs attention",
            Self::Promotions => "Promotions",
            Self::Updates => "Updates",
            Self::WorkAndLearning => "Work & learning",
            Self::EverythingElse => "Everything else",
        }
    }
}

pub fn classify(thread: &ThreadSummary) -> CategoryGroup {
    let s = format!("{} {}", thread.subject, thread.snippet).to_lowercase();
    if s.contains("security") || s.contains("invoice") || s.contains("alert") {
        CategoryGroup::NeedsAttention
    } else if s.contains("sale") || s.contains("newsletter") || s.contains("receipt") {
        CategoryGroup::Promotions
    } else if s.contains("standup") || s.contains("project") || s.contains("kickoff") || s.contains("review")
    {
        CategoryGroup::WorkAndLearning
    } else if thread.sender_domain.is_some() {
        CategoryGroup::Updates
    } else {
        CategoryGroup::EverythingElse
    }
}

pub fn why(thread: &ThreadSummary) -> String {
    format!("Matched heuristics for {}", classify(thread).title())
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::mailbox::demo_threads;

    #[test]
    fn classifies_security_as_needs_attention() {
        let t = demo_threads()
            .into_iter()
            .find(|t| t.subject.to_lowercase().contains("security"))
            .unwrap();
        assert_eq!(classify(&t), CategoryGroup::NeedsAttention);
    }
}
