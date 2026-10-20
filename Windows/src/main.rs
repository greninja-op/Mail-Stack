mod workspace;

use workspace::WindowsWorkspace;

fn main() {
    let w = WindowsWorkspace::bootstrap_demo();
    println!("Mailstack Windows scaffold");
    println!("inbox: {}", w.inbox().len());
    println!("unread search: {}", w.search_unread());
    println!("needs-attention: {}", w.classify_demo());
    assert!(WindowsWorkspace::oauth_contract_ok());
    assert!(WindowsWorkspace::privacy_ok());
    assert!(WindowsWorkspace::sync_cap_ok());
    println!("ok");
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn demo_smoke() {
        let w = WindowsWorkspace::bootstrap_demo();
        assert!(!w.inbox().is_empty());
    }
}
