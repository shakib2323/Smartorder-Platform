package com.shakib.smartorder.order.invoice;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


final class PdfDocument {

    static final double PAGE_WIDTH = 595;
    static final double PAGE_HEIGHT = 842;

    // Advance widths (1/1000 em) of ASCII 32..126 for Helvetica and Helvetica-Bold.
    private static final int[] REGULAR_WIDTHS = {
            278, 278, 355, 556, 556, 889, 667, 191, 333, 333, 389, 584, 278, 333, 278, 278, 556, 556, 556,
            556, 556, 556, 556, 556, 556, 556, 278, 278, 584, 584, 584, 556, 1015, 667, 667, 722, 722, 667,
            611, 778, 722, 278, 500, 667, 556, 833, 722, 778, 667, 778, 722, 667, 611, 722, 667, 944, 667,
            667, 611, 278, 278, 278, 469, 556, 333, 556, 556, 500, 556, 556, 278, 556, 556, 222, 222, 500,
            222, 833, 556, 556, 556, 556, 333, 500, 278, 556, 500, 722, 500, 500, 500, 334, 260, 334, 584
    };
    private static final int[] BOLD_WIDTHS = {
            278, 333, 474, 556, 556, 889, 722, 238, 333, 333, 389, 584, 278, 333, 278, 278, 556, 556, 556,
            556, 556, 556, 556, 556, 556, 556, 333, 333, 584, 584, 584, 611, 975, 722, 722, 722, 722, 667,
            611, 778, 722, 278, 556, 722, 611, 833, 722, 778, 667, 778, 722, 667, 611, 722, 667, 944, 667,
            667, 611, 333, 278, 333, 584, 556, 333, 556, 611, 556, 611, 556, 333, 611, 611, 278, 278, 556,
            278, 889, 611, 611, 611, 611, 389, 556, 333, 611, 556, 778, 556, 556, 500, 389, 280, 389, 584
    };

    private final List<Page> pages = new ArrayList<>();
    private String title = "";

    void setTitle(String title) {
        this.title = sanitize(title);
    }

    Page newPage() {
        Page page = new Page();
        pages.add(page);
        return page;
    }

    int pageCount() {
        return pages.size();
    }

    Page pageAt(int index) {
        return pages.get(index);
    }

    // Text width in points.
    static double width(String text, boolean bold, double size) {
        int[] widths = bold ? BOLD_WIDTHS : REGULAR_WIDTHS;
        double total = 0;
        for (char c : sanitize(text).toCharArray()) {
            total += widths[c - 32];
        }
        return total * size / 1000.0;
    }

    // Cuts text with "..." so it fits in maxWidth points.
    static String fit(String text, boolean bold, double size, double maxWidth) {
        String clean = sanitize(text);
        if (width(clean, bold, size) <= maxWidth) {
            return clean;
        }
        String ellipsis = "...";
        int end = clean.length();
        while (end > 0 && width(clean.substring(0, end) + ellipsis, bold, size) > maxWidth) {
            end--;
        }
        return clean.substring(0, end).stripTrailing() + ellipsis;
    }

    private static String sanitize(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            out.append(c >= 32 && c <= 126 ? c : '?');
        }
        return out.toString();
    }

    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    private static String color(int rgb) {
        return String.format(Locale.ROOT, "%.3f %.3f %.3f", ((rgb >> 16) & 0xFF) / 255.0, ((rgb >> 8) & 0xFF) / 255.0,
                (rgb & 0xFF) / 255.0);
    }

    private static String num(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    // One page. Drawing calls append PDF operators to its content stream.
    static final class Page {

        private final StringBuilder content = new StringBuilder();

        void text(double x, double baselineY, boolean bold, double size, String text, int rgb) 
        {
            content.append("BT /").append(bold ? "F2" : "F1").append(' ').append(num(size)).append(" Tf ")
                    .append(color(rgb)).append(" rg ").append(num(x)).append(' ').append(num(PAGE_HEIGHT - baselineY))
                    .append(" Td (").append(escape(sanitize(text))).append(") Tj ET\n");
        }

        void textRight(double rightX, double baselineY, boolean bold, double size, String text, int rgb) 
        {
            text(rightX - width(text, bold, size), baselineY, bold, size, text, rgb);
        }

        void textCenter(double centerX, double baselineY, boolean bold, double size, String text, int rgb) 
        {
            text(centerX - width(text, bold, size) / 2, baselineY, bold, size, text, rgb);
        }

        void fillRect(double x, double top, double w, double h, int rgb) {
            content.append(color(rgb)).append(" rg ").append(num(x)).append(' ').append(num(PAGE_HEIGHT - top - h))
                    .append(' ').append(num(w)).append(' ').append(num(h)).append(" re f\n");
        }

        void strokeRect(double x, double top, double w, double h, double lineWidth, int rgb)
        {
            content.append(num(lineWidth)).append(" w ").append(color(rgb)).append(" RG ").append(num(x)).append(' ')
                    .append(num(PAGE_HEIGHT - top - h)).append(' ').append(num(w)).append(' ').append(num(h))
                    .append(" re S\n");
        }

        void line(double x1, double y1, double x2, double y2, double lineWidth, int rgb)
        {
            content.append(num(lineWidth)).append(" w ").append(color(rgb)).append(" RG ").append(num(x1)).append(' ')
                    .append(num(PAGE_HEIGHT - y1)).append(" m ").append(num(x2)).append(' ')
                    .append(num(PAGE_HEIGHT - y2)).append(" l S\n");
        }
    }

    byte[] toBytes() {
        if (pages.isEmpty()) 
        {
            newPage();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();
        int pageCount = pages.size();
        int firstPageObject = 6; // 1 catalog, 2 pages, 3 regular font, 4 bold font, 5 info

        write(out, "%PDF-1.4\n");

        offsets.add(out.size());
        write(out, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < pageCount; i++) {
            kids.append(firstPageObject + 2 * i).append(" 0 R ");
        }
        offsets.add(out.size());
        write(out, "2 0 obj\n<< /Type /Pages /Kids [" + kids + "] /Count " + pageCount + " >>\nendobj\n");

        offsets.add(out.size());
        write(out, "3 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>\nendobj\n");
        offsets.add(out.size());
        write(out, "4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>\nendobj\n");
        offsets.add(out.size());
        write(out, "5 0 obj\n<< /Title (" + escape(title) + ") /Producer (SmartOrder) >>\nendobj\n");

        for (int i = 0; i < pageCount; i++) 
        {
            int pageObject = firstPageObject + 2 * i;
            byte[] stream = pages.get(i).content.toString().getBytes(StandardCharsets.ISO_8859_1);

            offsets.add(out.size());
            write(out, pageObject + " 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
                    + "/Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents " + (pageObject + 1)
                    + " 0 R >>\nendobj\n");

            offsets.add(out.size());
            write(out, (pageObject + 1) + " 0 obj\n<< /Length " + stream.length + " >>\nstream\n");
            out.writeBytes(stream);
            write(out, "\nendstream\nendobj\n");
        }

        int xrefOffset = out.size();
        int objectCount = offsets.size() + 1;
        write(out, "xref\n0 " + objectCount + "\n");
        write(out, "0000000000 65535 f \n");
        for (int offset : offsets) {
            write(out, String.format(Locale.ROOT, "%010d 00000 n \n", offset));
        }
        write(out, "trailer\n<< /Size " + objectCount + " /Root 1 0 R /Info 5 0 R >>\nstartxref\n" + xrefOffset
                + "\n%%EOF\n");
        return out.toByteArray();
    }

    private static void write(ByteArrayOutputStream out, String text)
    {
        out.writeBytes(text.getBytes(StandardCharsets.ISO_8859_1));
    }
}
