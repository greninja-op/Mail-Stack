mod workspace;

use workspace::LinuxWorkspace;

fn main() {
    let w = LinuxWorkspace::bootstrap_demo();
    println!("Mailstack Linux scaffold");
    println!("inbox: {}", w.inbox().len());
    println!("unread search: {}", w.search_unread());
    println!("needs-attention: {}", w.classify_demo());
    assert!(LinuxWorkspace::oauth_contract_ok());
    assert!(LinuxWorkspace::privacy_ok());
    assert!(LinuxWorkspace::sync_cap_ok());
    println!("ok");
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn privacy_and_demo() {
        let w = LinuxWorkspace::bootstrap_demo();
        assert!(LinuxWorkspace::privacy_ok());
        assert!(!w.inbox().is_empty());
    }
}
