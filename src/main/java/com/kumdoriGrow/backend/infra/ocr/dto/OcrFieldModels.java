package com.kumdoriGrow.backend.infra.ocr.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class OcrFieldModels {

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OcrImage {
        private List<OcrField> fields;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OcrField {
        private String inferText;
        private Double inferConfidence;
    }
}
