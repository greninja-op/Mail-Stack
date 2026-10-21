//! App logo / mark assets.
//!
//! Logo: `Windows/resources/icons/app_logo.png` (same art as repo `logo/logo.png`).
//! Design A: compact sidebar mark + small wordmark only (no large top-chrome logo).
//! Background flowing-shape art: **not present** — see `redesign/04-theme-done.md`.
//! Do not invent fake photo / glass backgrounds; screens use solid tonal fills
//! from `ThemeTokens` (`bg`, `surface`, `tonal_panel()`).

use egui::{ColorImage, IconData, TextureHandle, TextureOptions, Vec2};
use std::path::PathBuf;

/// Compact sidebar brand mark size (logical px).
pub const SIDEBAR_MARK_SIZE: f32 = 28.0;

fn logo_path() -> PathBuf {
    PathBuf::from(env!("CARGO_MANIFEST_DIR")).join("resources/icons/app_logo.png")
}

fn load_rgba() -> Option<(u32, u32, Vec<u8>)> {
    let path = logo_path();
    let img = image::open(&path).ok()?.into_rgba8();
    let (w, h) = img.dimensions();
    Some((w, h, img.into_raw()))
}

/// Window / taskbar icon (downscaled for IconData).
pub fn window_icon() -> Option<IconData> {
    let (w, h, rgba) = load_rgba()?;
    let dynimg = image::RgbaImage::from_raw(w, h, rgba)?;
    let resized =
        image::imageops::resize(&dynimg, 256, 256, image::imageops::FilterType::Triangle);
    let (rw, rh) = resized.dimensions();
    Some(IconData {
        rgba: resized.into_raw(),
        width: rw,
        height: rh,
    })
}

/// Upload logo into egui texture atlas (once). Used for compact sidebar mark.
pub fn upload_logo_texture(ctx: &egui::Context) -> Option<TextureHandle> {
    let (w, h, rgba) = load_rgba()?;
    let color = ColorImage::from_rgba_unmultiplied([w as usize, h as usize], &rgba);
    Some(ctx.load_texture("mailstack_logo", color, TextureOptions::LINEAR))
}

/// egui size for the compact sidebar mark.
pub fn sidebar_mark_size() -> Vec2 {
    Vec2::splat(SIDEBAR_MARK_SIZE)
}

/// True when dedicated light/dark flowing-shape background PNGs exist under resources.
/// Currently always false — assets were not supplied; UI uses solid canvas tones.
pub fn has_flowing_backgrounds() -> bool {
    let base = PathBuf::from(env!("CARGO_MANIFEST_DIR")).join("resources");
    let light = base.join("backgrounds/flowing_light.png");
    let dark = base.join("backgrounds/flowing_dark.png");
    light.is_file() && dark.is_file()
}
