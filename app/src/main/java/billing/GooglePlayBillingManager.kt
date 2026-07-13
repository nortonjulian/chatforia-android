package com.chatforia.android.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.asStateFlow

data class GooglePlayOffer(
    val pricingProduct: PricingProduct,
    val productDetails: ProductDetails,
    val offerToken: String,
    val formattedPrice: String
)

data class GooglePlayBillingState(
    val isReady: Boolean = false,
    val isLoading: Boolean = true,
    val offers: Map<PricingProduct, GooglePlayOffer> = emptyMap(),
    val errorMessage: String? = null
)

sealed interface GooglePlayBillingEvent {

    data class PurchasesFound(
        val purchases: List<Purchase>,
        val requestedProduct: PricingProduct?,
        val fromRestore: Boolean
    ) : GooglePlayBillingEvent

    data class Message(
        val text: String
    ) : GooglePlayBillingEvent
}

class GooglePlayBillingManager(
    context: Context
) : PurchasesUpdatedListener {

    private val _state =
        MutableStateFlow(GooglePlayBillingState())

    val state: StateFlow<GooglePlayBillingState> =
        _state.asStateFlow()

    private val _events =
        Channel<GooglePlayBillingEvent>(
            capacity = Channel.BUFFERED
        )

    val events: Flow<GooglePlayBillingEvent> =
        _events.receiveAsFlow()

    private var isConnecting = false
    private var isClosed = false

    private var pendingProduct: PricingProduct? = null

    private val billingClient =
        BillingClient.newBuilder(context.applicationContext)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .enableAutoServiceReconnection()
            .build()

    fun start() {
        if (isClosed) {
            return
        }

        if (billingClient.isReady) {
            handleBillingReady()
            return
        }

        if (isConnecting) {
            return
        }

        isConnecting = true

        _state.value = _state.value.copy(
            isLoading = true,
            errorMessage = null
        )

        billingClient.startConnection(
            object : BillingClientStateListener {

                override fun onBillingSetupFinished(
                    billingResult: BillingResult
                ) {
                    isConnecting = false

                    if (
                        billingResult.responseCode ==
                        BillingClient.BillingResponseCode.OK
                    ) {
                        handleBillingReady()
                    } else {
                        setError(
                            billingResult.debugMessage.ifBlank {
                                "Google Play Billing could not start."
                            }
                        )
                    }
                }

                override fun onBillingServiceDisconnected() {
                    isConnecting = false

                    _state.value = _state.value.copy(
                        isReady = false,
                        isLoading = false
                    )
                }
            }
        )
    }

    private fun handleBillingReady() {
        _state.value = _state.value.copy(
            isReady = true,
            isLoading = true,
            errorMessage = null
        )

        queryProductDetails()

        // Process any existing purchases when billing connects.
        queryPurchases(fromRestore = false)
    }

    private fun queryProductDetails() {
        val playProductIds =
            PricingProduct.entries
                .map { it.playProductId }
                .distinct()

        val productList =
            playProductIds.map { productId ->
                QueryProductDetailsParams.Product
                    .newBuilder()
                    .setProductId(productId)
                    .setProductType(
                        BillingClient.ProductType.SUBS
                    )
                    .build()
            }

        val params =
            QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()

        billingClient.queryProductDetailsAsync(params) {
                billingResult,
                productDetailsResult ->

            if (
                billingResult.responseCode !=
                BillingClient.BillingResponseCode.OK
            ) {
                setError(
                    billingResult.debugMessage.ifBlank {
                        "Could not load Google Play subscriptions."
                    }
                )

                return@queryProductDetailsAsync
            }

            val detailsByProductId =
                productDetailsResult.productDetailsList
                    .associateBy { it.productId }

            val resolvedOffers =
                buildMap<PricingProduct, GooglePlayOffer> {

                    PricingProduct.entries.forEach { pricingProduct ->

                        val productDetails =
                            detailsByProductId[
                                pricingProduct.playProductId
                            ] ?: return@forEach

                        val matchingOffers =
                            productDetails
                                .subscriptionOfferDetails
                                ?.filter { offer ->
                                    offer.basePlanId ==
                                            pricingProduct.basePlanId
                                }
                                .orEmpty()

                        // Prefer the normal base plan rather than a
                        // discounted offer that might be added later.
                        val selectedOffer =
                            matchingOffers.firstOrNull { offer ->
                                offer.offerId == null
                            } ?: matchingOffers.firstOrNull()
                            ?: return@forEach

                        val formattedPrice =
                            selectedOffer
                                .pricingPhases
                                .pricingPhaseList
                                .lastOrNull()
                                ?.formattedPrice
                                ?: return@forEach

                        put(
                            pricingProduct,
                            GooglePlayOffer(
                                pricingProduct = pricingProduct,
                                productDetails = productDetails,
                                offerToken = selectedOffer.offerToken,
                                formattedPrice = formattedPrice
                            )
                        )
                    }
                }

            val missingProducts =
                PricingProduct.entries.filter {
                    it !in resolvedOffers
                }

            _state.value = _state.value.copy(
                isReady = true,
                isLoading = false,
                offers = resolvedOffers,
                errorMessage =
                    if (missingProducts.isEmpty()) {
                        null
                    } else {
                        "Some Google Play subscriptions are unavailable."
                    }
            )
        }
    }

    fun launchPurchase(
        activity: Activity,
        product: PricingProduct
    ) {
        if (!billingClient.isReady) {
            setError("Google Play Billing is still connecting.")
            start()
            return
        }

        val offer = _state.value.offers[product]

        if (offer == null) {
            setError(
                "This subscription is not available through Google Play."
            )
            return
        }

        pendingProduct = product

        val productDetailsParams =
            BillingFlowParams.ProductDetailsParams
                .newBuilder()
                .setProductDetails(offer.productDetails)
                .setOfferToken(offer.offerToken)
                .build()

        val billingFlowParams =
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(productDetailsParams)
                )
                .build()

        val result =
            billingClient.launchBillingFlow(
                activity,
                billingFlowParams
            )

        if (
            result.responseCode !=
            BillingClient.BillingResponseCode.OK
        ) {
            pendingProduct = null

            setError(
                result.debugMessage.ifBlank {
                    "Google Play could not open the purchase screen."
                }
            )
        }
    }

    fun restorePurchases() {
        if (!billingClient.isReady) {
            setError("Google Play Billing is still connecting.")
            start()
            return
        }

        queryPurchases(fromRestore = true)
    }

    private fun queryPurchases(
        fromRestore: Boolean
    ) {
        if (!billingClient.isReady) {
            return
        }

        val params =
            QueryPurchasesParams.newBuilder()
                .setProductType(
                    BillingClient.ProductType.SUBS
                )
                .build()

        billingClient.queryPurchasesAsync(params) {
                billingResult,
                purchases ->

            if (
                billingResult.responseCode ==
                BillingClient.BillingResponseCode.OK
            ) {
                _events.trySend(
                    GooglePlayBillingEvent.PurchasesFound(
                        purchases = purchases,
                        requestedProduct = null,
                        fromRestore = fromRestore
                    )
                )
            } else {
                _events.trySend(
                    GooglePlayBillingEvent.Message(
                        billingResult.debugMessage.ifBlank {
                            "Could not restore Google Play purchases."
                        }
                    )
                )
            }
        }
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        when (billingResult.responseCode) {

            BillingClient.BillingResponseCode.OK -> {
                val completedPurchases =
                    purchases.orEmpty()

                _events.trySend(
                    GooglePlayBillingEvent.PurchasesFound(
                        purchases = completedPurchases,
                        requestedProduct = pendingProduct,
                        fromRestore = false
                    )
                )

                pendingProduct = null
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                pendingProduct = null

                _events.trySend(
                    GooglePlayBillingEvent.Message(
                        "Purchase canceled."
                    )
                )
            }

            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                pendingProduct = null

                _events.trySend(
                    GooglePlayBillingEvent.Message(
                        "This subscription is already owned. Restoring purchases."
                    )
                )

                restorePurchases()
            }

            else -> {
                pendingProduct = null

                _events.trySend(
                    GooglePlayBillingEvent.Message(
                        billingResult.debugMessage.ifBlank {
                            "Google Play could not complete the purchase."
                        }
                    )
                )
            }
        }
    }

    private fun setError(
        message: String
    ) {
        _state.value = _state.value.copy(
            isLoading = false,
            errorMessage = message
        )

        _events.trySend(
            GooglePlayBillingEvent.Message(message)
        )
    }

    fun clearError() {
        _state.value = _state.value.copy(
            errorMessage = null
        )
    }

    fun close() {
        if (isClosed) {
            return
        }

        isClosed = true
        billingClient.endConnection()
    }
}