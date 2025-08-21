package com.kumdoriGrow.backend.infra.ocr.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OcrResult {
    private List<OcrFieldModels.OcrImage> images;
}
