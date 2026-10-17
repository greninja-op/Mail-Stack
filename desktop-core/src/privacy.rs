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
}
