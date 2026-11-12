package com.fabrodiego.moneyguard.importer;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fabrodiego.moneyguard.common.BadRequestException;
import com.fabrodiego.moneyguard.common.CurrentUser;
import com.fabrodiego.moneyguard.importer.ImportDtos.ApproveRequest;
import com.fabrodiego.moneyguard.importer.ImportDtos.PreviewResponse;
import com.fabrodiego.moneyguard.transaction.TransactionDtos.TransactionResponse;

import jakarta.validation.Valid;

/** Extrato (CSV) e fatura (PDF) do Inter. O arquivo so e lido em memoria; nunca e guardado. */
@RestController
@RequestMapping("/api/imports")
public class ImportController {

    private final ImportService service;

    public ImportController(ImportService service) {
        this.service = service;
    }

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PreviewResponse preview(@AuthenticationPrincipal Jwt jwt, @RequestParam("file") MultipartFile file) {
        try {
            return service.preview(CurrentUser.id(jwt), file.getBytes());
        } catch (IOException e) {
            throw new BadRequestException("Nao foi possivel ler o arquivo enviado");
        }
    }

    @PostMapping("/approve")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse approve(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ApproveRequest req) {
        return service.approve(CurrentUser.id(jwt), req);
    }
}
