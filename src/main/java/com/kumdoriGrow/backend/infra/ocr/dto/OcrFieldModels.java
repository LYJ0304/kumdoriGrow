package com.kumdoriGrow.backend.infra.ocr.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class OcrFieldModels {

    @Getter
    @Setter
    public static class OcrImage {
        private List<OcrField> fields;
    }

    @Getter
    @Setter
    public static class OcrField {
        private String inferText;
        private Double inferConfidence;
    }
}
