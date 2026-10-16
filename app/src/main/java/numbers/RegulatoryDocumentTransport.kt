package com.chatforia.android.numbers

interface RegulatoryDocumentTransport {

    fun uploadRegulatoryDocumentRaw(
        e164: String,
        requirementName: String,
        documentType: String,
        attributesJson: String,
        filename: String,
        mimeType: String,
        bytes: ByteArray
    ): String
}
