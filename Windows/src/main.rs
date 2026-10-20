//! Mailstack Windows entry.
//!
//! GUI subsystem binary — **not** a CLI product.
//! Interim presentation: egui / eframe multi-pane shell (Fluent-inspired tokens).
//! Packaging path: WinUI 3 / Windows App SDK when SDK + signing are available.

#![windows_subsystem = "windows"]

mod adapters;
mod app_state;
mod bridge;
mod capability_profile;
mod identity;
mod interaction;
mod navigation;
mod redacting_log;
mod shell;
mod tokens;
mod ui;
mod workspace;

fn main() {
    let identity = identity::app_identity();
    if let Err(err) = shell::run(identity) {
        #[cfg(windows)]
        {
            use std::ffi::OsStr;
            use std::os::windows::ffi::OsStrExt;
            use std::ptr;
            type HWND = *mut core::ffi::c_void;
            type LPCWSTR = *const u16;
            #[link(name = "user32")]
            extern "system" {
                fn MessageBoxW(h: HWND, t: LPCWSTR, c: LPCWSTR, u: u32) -> i32;
            }
            fn wide(s: &str) -> Vec<u16> {
                OsStr::new(s).encode_wide().chain(Some(0)).collect()
            }
            let title = wide("Mailstack");
            let body = wide(&format!("Mailstack failed to start:\n{err}"));
            unsafe {
                MessageBoxW(ptr::null_mut(), body.as_ptr(), title.as_ptr(), 0x10);
            }
        }
        #[cfg(not(windows))]
        {
            eprintln!("Mailstack failed to start: {err}");
        }
    }
}
