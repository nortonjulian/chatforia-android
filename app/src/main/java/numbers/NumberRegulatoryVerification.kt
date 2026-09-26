package com.chatforia.android.numbers

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chatforia.android.ChatforiaGradientButton
import com.chatforia.android.auth.UserDto
import com.chatforia.android.network.ApiClient
import com.chatforia.android.network.ApiException
import com.chatforia.android.ui.theme.ChatforiaColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

private enum class RegulatoryScreenStage {
    LOADING,
    IDENTITY,
    DOCUMENTS,
    PENDING,
    REJECTED,
    ERROR
}

@Composable
fun NumberRegulatoryVerification(
    apiClient: ApiClient,
    user: UserDto,
    verification: NumberRegulatoryVerificationState,
    onBack: () -> Unit,
    onApproved: () -> Unit,
    onVerificationStateChanged: (NumberRegulatoryVerificationState) -> Unit
) {
    val repository =
        remember(apiClient) {
            NumberRegulatoryRepository(
                api = apiClient,
                documentTransport = apiClient
            )
        }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var stage by remember(verification.e164) {
        mutableStateOf(RegulatoryScreenStage.LOADING)
    }

    var requirements by remember(verification.e164) {
        mutableStateOf<RegulatoryRequirementsDto?>(null)
    }

    var error by remember(verification.e164) {
        mutableStateOf<String?>(null)
    }

    var isWorking by remember(verification.e164) {
        mutableStateOf(false)
    }

    val identityValues =
        remember(verification.e164) {
            mutableStateMapOf<String, String>()
        }

    var activeDocumentRequirement by remember(verification.e164) {
        mutableStateOf<RegulatoryDocumentRequirement?>(null)
    }

    var selectedDocumentType by remember(verification.e164) {
        mutableStateOf<RegulatoryAcceptedDocumentDto?>(null)
    }

    var selectedDocumentUri by remember(verification.e164) {
        mutableStateOf<Uri?>(null)
    }

    var documentRequiredFields by remember(verification.e164) {
        mutableStateOf<List<String>>(emptyList())
    }

    val documentFieldValues =
        remember(verification.e164) {
            mutableStateMapOf<String, String>()
        }

    val completedDocumentRequirements =
        remember(verification.e164) {
            mutableStateMapOf<String, Boolean>()
        }

    var bundleEmail by remember(verification.e164) {
        mutableStateOf(user.email.orEmpty())
    }

    val identityFields =
        requirements
            ?.endUser
            .orEmpty()
            .flatMap { it.fields }
            .distinct()

    val documentRequirements =
        repository.supportingDocumentRequirements(
            requirements
        )

    val allDocumentsComplete =
        documentRequirements.all { requirement ->
            completedDocumentRequirements[
                requirement.requirementName
            ] == true
        }

    val documentPicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri ->
            selectedDocumentUri = uri
        }

    fun resetDocumentEditor() {
        activeDocumentRequirement = null
        selectedDocumentType = null
        selectedDocumentUri = null
        documentRequiredFields = emptyList()
        documentFieldValues.clear()
        error = null
    }

    fun applyStatus(
        result: RegulatoryStatusResponseDto
    ) {
        val decision =
            result.decision
                ?: "BLOCKED_UNKNOWN_STATUS"

        onVerificationStateChanged(
            verification.copy(
                decision = decision,
                requiresVerification =
                    result.requiresVerification
            )
        )

        when {
            result.allowed &&
                decision == "APPROVED" -> {
                onApproved()
            }

            decision == "VERIFICATION_PENDING" ||
                decision == "PENDING_REVIEW" ||
                decision == "IN_REVIEW" -> {
                stage = RegulatoryScreenStage.PENDING
            }

            decision == "VERIFICATION_REJECTED" ||
                decision == "REJECTED" -> {
                stage = RegulatoryScreenStage.REJECTED
            }

            result.requiresVerification ||
                decision == "VERIFICATION_REQUIRED" -> {
                stage = RegulatoryScreenStage.LOADING
            }

            else -> {
                error =
                    result.error
                        ?: "Regulatory verification is currently unavailable."
                stage = RegulatoryScreenStage.ERROR
            }
        }
    }

    suspend fun initializeVerification() {
        isWorking = true
        error = null

        try {
            val result =
                repository.initialize(
                    e164 = verification.e164
                )

            requirements = result.requirements

            val fields =
                result.requirements
                    ?.endUser
                    .orEmpty()
                    .flatMap { it.fields }
                    .distinct()

            stage =
                if (fields.isEmpty()) {
                    RegulatoryScreenStage.DOCUMENTS
                } else {
                    RegulatoryScreenStage.IDENTITY
                }
        } catch (e: Exception) {
            error =
                e.message
                    ?: "Could not initialize regulatory verification."
            stage = RegulatoryScreenStage.ERROR
        } finally {
            isWorking = false
        }
    }

    suspend fun checkStatus() {
        isWorking = true
        error = null

        try {
            val result =
                repository.status(
                    e164 = verification.e164
                )

            val requiresInitialization =
                result.requiresVerification ||
                    result.decision == "VERIFICATION_REQUIRED" ||
                    result.decision == "VERIFICATION_REJECTED" ||
                    result.decision == "REJECTED"

            applyStatus(result)

            if (requiresInitialization) {
                initializeVerification()
            }
        } catch (e: Exception) {
            error =
                e.message
                    ?: "Could not check regulatory verification status."
            stage = RegulatoryScreenStage.ERROR
        } finally {
            isWorking = false
        }
    }

    LaunchedEffect(
        verification.e164,
        verification.decision
    ) {
        /*
         * Pending review is deliberately status-only.
         *
         * Do NOT call initialize here for VERIFICATION_PENDING.
         * The server extends the number reservation for review after
         * submission, while initialize uses the shorter verification TTL.
         */
        if (
            verification.decision == "VERIFICATION_PENDING" ||
            verification.decision == "PENDING_REVIEW" ||
            verification.decision == "IN_REVIEW"
        ) {
            checkStatus()
        } else {
            initializeVerification()
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ChatforiaColors.screenBackground
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Verify your number",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = ChatforiaColors.primaryText,
                    modifier = Modifier.weight(1f)
                )

                TextButton(
                    onClick = onBack,
                    enabled = !isWorking
                ) {
                    Text("Back")
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = verification.e164,
                color = ChatforiaColors.secondaryText,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(16.dp))

            HorizontalDivider(
                color = ChatforiaColors.border
            )

            Spacer(Modifier.height(20.dp))

            when (stage) {
                RegulatoryScreenStage.LOADING -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = "Loading verification requirements…",
                            color = ChatforiaColors.secondaryText
                        )
                    }
                }

                RegulatoryScreenStage.IDENTITY -> {
                    Text(
                        text = "Identity information",
                        color = ChatforiaColors.primaryText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text =
                            "This information is required by the phone-number provider for this number.",
                        color = ChatforiaColors.secondaryText,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(Modifier.height(16.dp))

                    Column(
                        verticalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {
                        identityFields.forEach { field ->
                            OutlinedTextField(
                                value =
                                    identityValues[field].orEmpty(),
                                onValueChange = {
                                    identityValues[field] = it
                                },
                                label = {
                                    Text(
                                        regulatoryFieldLabel(field)
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    error?.let {
                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    ChatforiaGradientButton(
                        text =
                            if (isWorking) {
                                "Saving…"
                            } else {
                                "Continue"
                            },
                        enabled =
                            !isWorking &&
                                identityFields.all {
                                    identityValues[it]
                                        ?.isNotBlank() == true
                                },
                        onClick = {
                            scope.launch {
                                isWorking = true
                                error = null

                                try {
                                    val attributes =
                                        identityFields.associateWith {
                                            identityValues[it]
                                                .orEmpty()
                                                .trim()
                                        }

                                    val result =
                                        repository.initialize(
                                            e164 =
                                                verification.e164,
                                            endUserAttributes =
                                                attributes
                                        )

                                    requirements =
                                        result.requirements
                                            ?: requirements

                                    if (result.initialized) {
                                        stage =
                                            RegulatoryScreenStage.DOCUMENTS
                                    } else {
                                        error =
                                            result.reason
                                                ?: "Identity verification could not be completed."
                                    }
                                } catch (e: Exception) {
                                    error =
                                        e.message
                                            ?: "Could not save identity information."
                                } finally {
                                    isWorking = false
                                }
                            }
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                    )
                }

                RegulatoryScreenStage.DOCUMENTS -> {
                    Text(
                        text = "Supporting documents",
                        color = ChatforiaColors.primaryText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text =
                            if (documentRequirements.isEmpty()) {
                                "No supporting documents are required for this number."
                            } else {
                                "Upload the documents required for this number."
                            },
                        color = ChatforiaColors.secondaryText
                    )

                    Spacer(Modifier.height(16.dp))

                    if (activeDocumentRequirement == null) {
                        Column(
                            verticalArrangement =
                                Arrangement.spacedBy(12.dp)
                        ) {
                            documentRequirements.forEach { requirement ->
                                val complete =
                                    completedDocumentRequirements[
                                        requirement.requirementName
                                    ] == true

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = ChatforiaColors.cardBackground,
                                    tonalElevation = 2.dp
                                ) {
                                    Column(
                                        modifier =
                                            Modifier.padding(14.dp)
                                    ) {
                                        Text(
                                            text =
                                                regulatoryFieldLabel(
                                                    requirement.requirementName
                                                ),
                                            color =
                                                ChatforiaColors.primaryText,
                                            fontWeight =
                                                FontWeight.SemiBold
                                        )

                                        Spacer(
                                            Modifier.height(6.dp)
                                        )

                                        Text(
                                            text =
                                                if (complete) {
                                                    "Uploaded"
                                                } else {
                                                    "Required"
                                                },
                                            color =
                                                ChatforiaColors.secondaryText
                                        )

                                        if (!complete) {
                                            Spacer(
                                                Modifier.height(10.dp)
                                            )

                                            TextButton(
                                                onClick = {
                                                    activeDocumentRequirement =
                                                        requirement
                                                    selectedDocumentType =
                                                        null
                                                    selectedDocumentUri =
                                                        null
                                                    documentRequiredFields =
                                                        emptyList()
                                                    documentFieldValues.clear()
                                                    error = null
                                                }
                                            ) {
                                                Text("Add document")
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        error?.let {
                            Spacer(Modifier.height(12.dp))

                            Text(
                                text = it,
                                color =
                                    MaterialTheme.colorScheme.error
                            )
                        }

                        Spacer(Modifier.height(20.dp))

                        OutlinedTextField(
                            value = bundleEmail,
                            onValueChange = {
                                bundleEmail = it
                            },
                            label = {
                                Text("Email")
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(16.dp))

                        ChatforiaGradientButton(
                            text =
                                if (isWorking) {
                                    "Submitting…"
                                } else {
                                    "Submit verification"
                                },
                            enabled =
                                !isWorking &&
                                    allDocumentsComplete &&
                                    bundleEmail.isNotBlank(),
                            onClick = {
                                scope.launch {
                                    isWorking = true
                                    error = null

                                    try {
                                        val assembled =
                                            repository.assemble(
                                                e164 =
                                                    verification.e164,
                                                email =
                                                    bundleEmail.trim()
                                            )

                                        if (!assembled.assembled) {
                                            error =
                                                assembled.reason
                                                    ?: "Could not prepare verification."
                                            return@launch
                                        }

                                        val submitted =
                                            repository.submit(
                                                e164 =
                                                    verification.e164
                                            )

                                        if (!submitted.submitted) {
                                            error =
                                                submitted.reason
                                                    ?: "Could not submit verification."
                                            return@launch
                                        }

                                        onVerificationStateChanged(
                                            verification.copy(
                                                decision =
                                                    "VERIFICATION_PENDING",
                                                requiresVerification =
                                                    false
                                            )
                                        )

                                        stage =
                                            RegulatoryScreenStage.PENDING
                                    } catch (e: ApiException) {
                                        if (
                                            e.statusCode == 409 &&
                                            e.responseBody.contains(
                                                "REGULATORY_RESERVATION_EXPIRED"
                                            )
                                        ) {
                                            onVerificationStateChanged(
                                                verification.copy(
                                                    decision =
                                                        "VERIFICATION_REQUIRED",
                                                    requiresVerification =
                                                        true
                                                )
                                            )

                                            error =
                                                "Your number reservation expired. Please restart verification."

                                            stage =
                                                RegulatoryScreenStage.ERROR
                                        } else {
                                            error =
                                                "Could not submit verification."
                                        }
                                    } catch (e: Exception) {
                                        error =
                                            e.message
                                                ?: "Could not submit verification."
                                    } finally {
                                        isWorking = false
                                    }
                                }
                            },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                        )
                    } else {
                        val requirement =
                            activeDocumentRequirement!!

                        Text(
                            text =
                                regulatoryFieldLabel(
                                    requirement.requirementName
                                ),
                            color = ChatforiaColors.primaryText,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = "Document type",
                            color = ChatforiaColors.secondaryText
                        )

                        Spacer(Modifier.height(8.dp))

                        Column(
                            verticalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {
                            requirement.acceptedDocuments.forEach {
                                document ->
                                val documentType =
                                    document.type
                                        ?.takeIf {
                                            it.isNotBlank()
                                        }

                                if (documentType != null) {
                                    TextButton(
                                        onClick = {
                                            scope.launch {
                                                isWorking = true
                                                error = null

                                                try {
                                                    val result =
                                                        repository
                                                            .documentRequirements(
                                                                e164 =
                                                                    verification.e164,
                                                                requirementName =
                                                                    requirement.requirementName,
                                                                documentType =
                                                                    documentType
                                                            )

                                                    if (!result.resolved) {
                                                        error =
                                                            result.reason
                                                                ?: "Could not load document requirements."
                                                        return@launch
                                                    }

                                                    selectedDocumentType =
                                                        document

                                                    documentRequiredFields =
                                                        result.requiredFields
                                                            .distinct()

                                                    documentFieldValues.clear()
                                                    selectedDocumentUri =
                                                        null
                                                } catch (e: Exception) {
                                                    error =
                                                        e.message
                                                            ?: "Could not load document requirements."
                                                } finally {
                                                    isWorking = false
                                                }
                                            }
                                        }
                                    ) {
                                        Text(
                                            document.name
                                                ?.takeIf {
                                                    it.isNotBlank()
                                                }
                                                ?: regulatoryFieldLabel(
                                                    documentType
                                                )
                                        )
                                    }
                                }
                            }
                        }

                        selectedDocumentType?.let { document ->
                            Spacer(Modifier.height(12.dp))

                            documentRequiredFields.forEach { field ->
                                OutlinedTextField(
                                    value =
                                        documentFieldValues[field]
                                            .orEmpty(),
                                    onValueChange = {
                                        documentFieldValues[field] =
                                            it
                                    },
                                    label = {
                                        Text(
                                            regulatoryFieldLabel(
                                                field
                                            )
                                        )
                                    },
                                    singleLine = true,
                                    modifier =
                                        Modifier.fillMaxWidth()
                                )

                                Spacer(
                                    Modifier.height(10.dp)
                                )
                            }

                            TextButton(
                                onClick = {
                                    documentPicker.launch(
                                        arrayOf(
                                            "image/jpeg",
                                            "image/png",
                                            "application/pdf"
                                        )
                                    )
                                },
                                enabled = !isWorking
                            ) {
                                Text(
                                    if (selectedDocumentUri == null) {
                                        "Choose JPEG, PNG, or PDF"
                                    } else {
                                        "Document selected"
                                    }
                                )
                            }

                            error?.let {
                                Spacer(
                                    Modifier.height(8.dp)
                                )

                                Text(
                                    text = it,
                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .error
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            ChatforiaGradientButton(
                                text =
                                    if (isWorking) {
                                        "Uploading…"
                                    } else {
                                        "Upload document"
                                    },
                                enabled =
                                    !isWorking &&
                                        selectedDocumentUri != null &&
                                        documentRequiredFields.all {
                                            documentFieldValues[it]
                                                ?.isNotBlank() == true
                                        },
                                onClick = {
                                    val uri =
                                        selectedDocumentUri
                                            ?: return@ChatforiaGradientButton

                                    val type =
                                        document.type
                                            ?.takeIf {
                                                it.isNotBlank()
                                            }
                                            ?: return@ChatforiaGradientButton

                                    scope.launch {
                                        isWorking = true
                                        error = null

                                        try {
                                            val mimeType =
                                                context
                                                    .contentResolver
                                                    .getType(uri)
                                                    ?: throw Exception(
                                                        "Could not determine the selected document type."
                                                    )

                                            val extension =
                                                when (mimeType) {
                                                    "image/jpeg" ->
                                                        "jpg"

                                                    "image/png" ->
                                                        "png"

                                                    "application/pdf" ->
                                                        "pdf"

                                                    else ->
                                                        throw Exception(
                                                            "Please choose a JPEG, PNG, or PDF document."
                                                        )
                                                }

                                            val bytes =
                                                withContext(
                                                    Dispatchers.IO
                                                ) {
                                                    context
                                                        .contentResolver
                                                        .openInputStream(
                                                            uri
                                                        )
                                                        ?.use {
                                                            input ->
                                                            input.readBytes()
                                                        }
                                                        ?: throw Exception(
                                                            "Could not read the selected document."
                                                        )
                                                }

                                            if (
                                                bytes.size >
                                                    5 * 1024 * 1024
                                            ) {
                                                throw Exception(
                                                    "The selected document must be 5 MB or smaller."
                                                )
                                            }

                                            val attributes =
                                                documentRequiredFields
                                                    .associateWith {
                                                        field ->
                                                        documentFieldValues[
                                                            field
                                                        ]
                                                            .orEmpty()
                                                            .trim()
                                                    }

                                            val result =
                                                repository.uploadDocument(
                                                    e164 =
                                                        verification.e164,
                                                    requirementName =
                                                        requirement
                                                            .requirementName,
                                                    documentType =
                                                        type,
                                                    attributes =
                                                        attributes,
                                                    filename =
                                                        "regulatory-${UUID.randomUUID()}.$extension",
                                                    mimeType =
                                                        mimeType,
                                                    bytes =
                                                        bytes
                                                )

                                            if (!result.provisioned) {
                                                error =
                                                    result.reason
                                                        ?: "Could not upload document."

                                                if (
                                                    result.requiredFields
                                                        .isNotEmpty()
                                                ) {
                                                    documentRequiredFields =
                                                        result
                                                            .requiredFields
                                                            .distinct()
                                                }

                                                return@launch
                                            }

                                            completedDocumentRequirements[
                                                requirement.requirementName
                                            ] = true

                                            resetDocumentEditor()
                                        } catch (e: Exception) {
                                            error =
                                                e.message
                                                    ?: "Could not upload document."
                                        } finally {
                                            isWorking = false
                                        }
                                    }
                                },
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        TextButton(
                            onClick = {
                                resetDocumentEditor()
                            },
                            enabled = !isWorking
                        ) {
                            Text("Cancel")
                        }
                    }
                }

                RegulatoryScreenStage.PENDING -> {
                    Text(
                        text = "Verification under review",
                        color = ChatforiaColors.primaryText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text =
                            "Your regulatory information has been submitted and is still being reviewed.",
                        color = ChatforiaColors.secondaryText
                    )

                    Spacer(Modifier.height(20.dp))

                    ChatforiaGradientButton(
                        text =
                            if (isWorking) {
                                "Checking…"
                            } else {
                                "Check status"
                            },
                        enabled = !isWorking,
                        onClick = {
                            scope.launch {
                                checkStatus()
                            }
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                    )
                }

                RegulatoryScreenStage.REJECTED -> {
                    Text(
                        text = "Verification needs attention",
                        color = ChatforiaColors.primaryText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text =
                            "The previous verification was not approved. You can review the requirements and submit updated information.",
                        color = ChatforiaColors.secondaryText
                    )

                    Spacer(Modifier.height(20.dp))

                    ChatforiaGradientButton(
                        text =
                            if (isWorking) {
                                "Loading…"
                            } else {
                                "Review requirements"
                            },
                        enabled = !isWorking,
                        onClick = {
                            scope.launch {
                                initializeVerification()
                            }
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                    )
                }

                RegulatoryScreenStage.ERROR -> {
                    Text(
                        text = "Verification unavailable",
                        color = ChatforiaColors.primaryText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text =
                            error
                                ?: "Could not load regulatory verification.",
                        color = MaterialTheme.colorScheme.error
                    )

                    Spacer(Modifier.height(20.dp))

                    ChatforiaGradientButton(
                        text = "Try again",
                        enabled = !isWorking,
                        onClick = {
                            scope.launch {
                                if (
                                    verification.decision ==
                                        "VERIFICATION_PENDING" ||
                                    verification.decision ==
                                        "PENDING_REVIEW" ||
                                    verification.decision ==
                                        "IN_REVIEW"
                                ) {
                                    checkStatus()
                                } else {
                                    initializeVerification()
                                }
                            }
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                    )
                }
            }
        }
    }
}

private fun regulatoryFieldLabel(
    field: String
): String =
    field
        .split("_")
        .filter { it.isNotBlank() }
        .joinToString(" ") { word ->
            word.replaceFirstChar { character ->
                character.uppercase()
            }
        }
