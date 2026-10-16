package com.chatforia.android.numbers

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class RegulatoryRequirementsDto(
    @SerialName("end_user")
    val endUser: List<RegulatoryEndUserRequirementDto> = emptyList(),

    @SerialName("supporting_document")
    val supportingDocument: List<JsonElement> = emptyList()
)

@Serializable
data class RegulatoryEndUserRequirementDto(
    val fields: List<String> = emptyList()
)

@Serializable
data class RegulatorySupportingDocumentRequirementDto(
    @SerialName("requirement_name")
    val requirementName: String? = null,

    val type: String? = null,

    @SerialName("accepted_documents")
    val acceptedDocuments: List<RegulatoryAcceptedDocumentDto> = emptyList()
)

@Serializable
data class RegulatoryAcceptedDocumentDto(
    val name: String? = null,
    val type: String? = null
)

@Serializable
data class RegulatoryProfileDto(
    val endUserSid: String? = null,
    val rejectionReason: String? = null
)

@Serializable
data class RegulatoryValidationDto(
    val missingFields: List<String> = emptyList()
)

@Serializable
data class RegulatoryInitializeResponseDto(
    val initialized: Boolean = false,
    val reused: Boolean = false,
    val reason: String? = null,
    val profile: RegulatoryProfileDto? = null,
    val requirements: RegulatoryRequirementsDto? = null,
    val validation: RegulatoryValidationDto? = null
)

@Serializable
data class RegulatoryStatusResponseDto(
    val allowed: Boolean = false,
    val decision: String? = null,
    val requiresVerification: Boolean = false,
    val profile: RegulatoryProfileDto? = null,
    val error: String? = null
)

@Serializable
data class RegulatoryDocumentRequirementsResponseDto(
    val resolved: Boolean = false,
    val reason: String? = null,
    val requiredFields: List<String> = emptyList(),
    val missingFields: List<String> = emptyList()
)

@Serializable
data class RegulatoryDocumentResponseDto(
    val provisioned: Boolean = false,
    val reused: Boolean = false,
    val reason: String? = null,
    val requiredFields: List<String> = emptyList(),
    val missingFields: List<String> = emptyList()
)

@Serializable
data class RegulatoryAssembleResponseDto(
    val assembled: Boolean = false,
    val reason: String? = null
)

@Serializable
data class RegulatorySubmitResponseDto(
    val submitted: Boolean = false,
    val reason: String? = null
)

@Serializable
data class RegulatoryInitializeRequest(
    val e164: String,
    val endUserAttributes: Map<String, String>? = null
)

@Serializable
data class RegulatoryStatusRequest(
    val e164: String
)

@Serializable
data class RegulatoryDocumentRequirementsRequest(
    val e164: String,
    val requirementName: String,
    val documentType: String
)

@Serializable
data class RegulatoryAssembleRequest(
    val e164: String,
    val email: String
)

@Serializable
data class RegulatorySubmitRequest(
    val e164: String
)

data class RegulatoryDocumentRequirement(
    val requirementName: String,
    val type: String?,
    val acceptedDocuments: List<RegulatoryAcceptedDocumentDto>
)
