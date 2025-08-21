package com.kumdoriGrow.backend.infra.ocr.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OcrResult {
    private List<OcrFieldModels.OcrImage> images;
}
