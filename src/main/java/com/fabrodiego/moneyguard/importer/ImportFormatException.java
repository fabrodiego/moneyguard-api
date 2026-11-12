package com.fabrodiego.moneyguard.importer;

import com.fabrodiego.moneyguard.common.BadRequestException;

public class ImportFormatException extends BadRequestException {

    public ImportFormatException(String message) {
        super(message);
    }
}
