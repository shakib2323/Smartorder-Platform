package com.shakib.smartorder.order.controller;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.shakib.smartorder.order.invoice.InvoiceService;

final class PdfResponses {

    private PdfResponses() {
    }

    // Opens in the browser's PDF viewer; the file name is the invoice number when the user saves it. 
    static ResponseEntity<byte[]> inline(InvoiceService.Document document) 
    {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(document.fileName()).build().toString())
                .body(document.bytes());
    }
}
