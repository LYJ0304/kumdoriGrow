package com.kumdoriGrow.backend.domain.receipt;

import org.springframework.stereotype.Component;
import java.text.Normalizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ReceiptParser {
    public String extractStoreName(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String[] lines = raw.split("\\R+");
        int limit = Math.min(lines.length, 12);
        for (int i = 0; i < limit; i++) {
            String line = normalize(lines[i]);
            if (line.isBlank()) continue;
            if (containsAny(line, "사업자", "사업자등록", "대표자", "주소", "전화", "TEL", "고객", "영수증")) break;
            if (line.replaceAll("[0-9\\p{Punct}\\s]", "").length() < 2) continue;
            return line.replaceAll("\\s{2,}", " ").trim();
        }
        Matcher m = Pattern.compile("(상호|가맹점명|매장명)\\s*[:\\-]?\\s*(.+)").matcher(raw);
        if (m.find()) return cleanTail(m.group(2));
        return null;
    }

    public Integer extractTotalPrice(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String pattern = "(합계|총액|총\\s*금액|결제\\s*금액|신용카드\\s*결제|카드\\s*결제|현금\\s*결제)\\s*[:\\-]?\\s*₩?\\s*([0-9,]+)";
        Matcher m = Pattern.compile(pattern).matcher(raw);
        int best = -1;
        while (m.find()) {
            int v = parseIntSafe(m.group(2));
            if (v > best) best = v;
        }
        if (best > 0) return best;

        String[] lines = raw.split("\\R+");
        for (int i = lines.length - 1; i >= Math.max(0, lines.length - 12); i--) {
            Matcher m2 = Pattern.compile("([0-9]{1,3}(?:,[0-9]{3})+)").matcher(lines[i]);
            while (m2.find()) {
                int v = parseIntSafe(m2.group(1));
                if (v > best) best = v;
            }
        }
        return best > 0 ? best : null;
    }

    // ===== helpers =====
    private String normalize(String s) {
        String t = Normalizer.normalize(s, Normalizer.Form.NFC);
        return t.replaceAll("[\\u200B-\\u200D\\uFEFF]", "").trim();
    }
    private boolean containsAny(String src, String... tokens) {
        for (String t : tokens) if (src.contains(t)) return true;
        return false;
    }
    private String cleanTail(String s) {
        return s.replaceAll("[\\*\\|]+$", "").replaceAll("\\s{2,}", " ").trim();
    }
    private int parseIntSafe(String n) {
        try {
            return Integer.parseInt(n.replaceAll(",", ""));
        } catch (Exception e) {
            return -1;
        }
    }
}
