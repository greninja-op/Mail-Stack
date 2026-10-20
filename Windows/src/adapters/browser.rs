//! Phase 09 — system browser opener for OAuth / safe external links.
//!
//! Sign-in must open in the trusted system browser — never an in-app
//! credential form. Only http(s) URLs are accepted.

use desktop_core::error::{CoreError, CoreResult};
use desktop_core::platform::BrowserOpen;

#[derive(Default, Debug, Clone, Copy)]
pub struct WindowsBrowser;

impl BrowserOpen for WindowsBrowser {
    fn open_url(&self, url: &str) -> CoreResult<()> {
        // Phase 23: reject javascript:/file:/content:/data: via shared policy.
        match desktop_core::privacy::validate_external_url(url) {
            desktop_core::privacy::UrlSafety::Safe
                if url.to_ascii_lowercase().starts_with("http://")
                    || url.to_ascii_lowercase().starts_with("https://") =>
            {
                open_url_impl(url)
            }
            desktop_core::privacy::UrlSafety::Safe => Err(CoreError::InvalidArgument(
                "only http(s) URLs may be opened via ShellExecute".into(),
            )),
            desktop_core::privacy::UrlSafety::Rejected(reason) => Err(CoreError::InvalidArgument(
                format!("blocked unsafe url: {reason}"),
            )),
        }
    }
}

/// Convenience for OAuth auth URL launch.
pub fn open_oauth_url(url: &str) -> CoreResult<()> {
    WindowsBrowser.open_url(url)
}

/// Safe external link launcher (blocks javascript:/file: etc.).
pub fn open_safe_link(url: &str) -> CoreResult<()> {
    desktop_core::privacy::launch_safe_link(&WindowsBrowser, url)
}

#[cfg(windows)]
fn open_url_impl(url: &str) -> CoreResult<()> {
    use std::ffi::OsStr;
    use std::os::windows::ffi::OsStrExt;
    use windows::core::PCWSTR;
    use windows::Win32::UI::Shell::ShellExecuteW;
    use windows::Win32::UI::WindowsAndMessaging::SW_SHOWNORMAL;

    let wide: Vec<u16> = OsStr::new(url).encode_wide().chain(Some(0)).collect();
    let op: Vec<u16> = OsStr::new("open").encode_wide().chain(Some(0)).collect();
    let result = unsafe {
        ShellExecuteW(
            None,
            PCWSTR(op.as_ptr()),
            PCWSTR(wide.as_ptr()),
            PCWSTR::null(),
            PCWSTR::null(),
            SW_SHOWNORMAL,
        )
    };
    // ShellExecute returns > 32 on success (HINSTANCE cast).
    if result.0 as usize > 32 {
        Ok(())
    } else {
        Err(CoreError::Internal(format!(
            "ShellExecuteW failed ({})",
            result.0 as usize
        )))
    }
}

#[cfg(not(windows))]
fn open_url_impl(_url: &str) -> CoreResult<()> {
    Err(CoreError::Internal(
        "browser open is only implemented on Windows".into(),
    ))
}

#[cfg(test)]
mod tests {
    use super::*;
    use desktop_core::platform::BrowserOpen;

    #[test]
    fn rejects_non_http() {
        let b = WindowsBrowser;
        assert!(b.open_url("file:///c:/secrets").is_err());
        assert!(b.open_url("javascript:alert(1)").is_err());
        assert!(b.open_url("data:text/html,hi").is_err());
    }
}
