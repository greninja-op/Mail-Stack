pub struct Privacy;

impl Privacy {
    pub fn should_fetch_remote_images() -> bool {
        false
    }

    pub fn sanitize_html(html: &str) -> String {
        let mut out = html.to_string();
        for pattern in ["<script", "</script>", "<img"] {
            if let Some(i) = out.to_lowercase().find(&pattern.to_lowercase()) {
                // coarse strip for scaffold — full sanitizer later
                out = out[..i].to_string();
            }
        }
        out
    }

    pub fn decode_entities(text: &str) -> String {
        text.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&nbsp;", " ")
            .replace("&bull;", "•")
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn never_fetches_images() {
        assert!(!Privacy::should_fetch_remote_images());
    }

    #[test]
    fn sanitize_strips_script_and_img() {
        let dirty = r#"<p>Hi</p><script>alert(1)</script><img src="https://evil.test/x.png">"#;
        let clean = Privacy::sanitize_html(dirty);
        assert!(!clean.to_lowercase().contains("<script"));
        assert!(!clean.to_lowercase().contains("<img"));
    }

    #[test]
    fn decode_entities_known_set() {
        assert_eq!(Privacy::decode_entities("&bull;"), "•");
        assert_eq!(Privacy::decode_entities("&amp;"), "&");
        // After decoding &amp;bull; in one pass we get • (amp then bull replacements).
        assert_eq!(Privacy::decode_entities("&amp;bull;"), "•");
        // Already-decoded text must not invent new glyphs.
        assert_eq!(Privacy::decode_entities("plain"), "plain");
    }
}
