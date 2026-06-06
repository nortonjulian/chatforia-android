package com.chatforia.android.billing

import com.chatforia.android.network.ApiClient
import com.chatforia.android.network.ApiRequest
import com.chatforia.android.network.HttpMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

enum class PricingProduct(val rawValue: String) {
    Plus("chatforia_plus"),
    PremiumMonthly("chatforia_premium_monthly"),
    PremiumAnnual("chatforia_premium_annual")
}

@Serializable
data class PricingQuote(
    val product: String? = null,
    val country: String? = null,
    val regionTier: String? = null,
    val currency: String? = null,
    val unitAmount: Int? = null,
    val stripePriceId: String? = null,
    val appleSku: String? = null,
    val googleSku: String? = null,
    val display: PricingQuoteDisplay? = null
)

@Serializable
data class PricingQuoteDisplay(
    val amount: String? = null,
    val currency: String? = null
)

class PricingQuoteService(
    private val apiClient: ApiClient
) {
    suspend fun getQuote(
        product: PricingProduct,
        country: String? = null,
        currency: String? = null
    ): PricingQuote? {
        val query = buildQuery(
            product = product.rawValue,
            country = country,
            currency = currency
        )

        return try {
            withContext(Dispatchers.IO) {
                apiClient.send(
                    ApiRequest(
                        path = "pricing/quote?$query",
                        method = HttpMethod.GET,
                        requiresAuth = false
                    )
                )
            }
        } catch (_: Exception) {
            fallbackQuote(product, country, currency)
        }
    }

    suspend fun getQuotes(
        products: List<PricingProduct>,
        country: String? = null,
        currency: String? = null
    ): Map<PricingProduct, PricingQuote> {
        val results = mutableMapOf<PricingProduct, PricingQuote>()

        for (product in products) {
            val quote = getQuote(product, country, currency)
            if (quote != null) {
                results[product] = quote
            }
        }

        return results
    }

    fun formattedPrice(
        quote: PricingQuote?,
        fallbackProduct: PricingProduct? = null,
        locale: Locale = Locale.getDefault()
    ): String? {
        quote?.let {
            val currency = it.currency
            val unitAmount = it.unitAmount

            if (!currency.isNullOrBlank() && unitAmount != null) {
                return formatMoney(unitAmount, currency, locale)
            }
        }

        if (fallbackProduct != null) {
            val fallback = fallbackAmounts[fallbackProduct] ?: return null
            return formatMoney(fallback.unitAmount, fallback.currency, locale)
        }

        return null
    }

    private fun buildQuery(
        product: String,
        country: String?,
        currency: String?
    ): String {
        val parts = mutableListOf("product=$product")

        if (!country.isNullOrBlank()) {
            parts.add("country=$country")
        }

        if (!currency.isNullOrBlank()) {
            parts.add("currency=$currency")
        }

        return parts.joinToString("&")
    }

    private fun fallbackQuote(
        product: PricingProduct,
        country: String?,
        currency: String?
    ): PricingQuote? {
        val fallback = fallbackAmounts[product] ?: return null
        val resolvedCurrency = currency ?: fallback.currency

        return PricingQuote(
            product = product.rawValue,
            country = country ?: "US",
            regionTier = "ROW",
            currency = resolvedCurrency,
            unitAmount = fallback.unitAmount,
            display = PricingQuoteDisplay(currency = resolvedCurrency)
        )
    }

    private fun formatMoney(
        unitAmount: Int,
        currencyCode: String,
        locale: Locale
    ): String {
        val amount = unitAmount / 100.0

        return try {
            val formatter = NumberFormat.getCurrencyInstance(locale)
            formatter.currency = Currency.getInstance(currencyCode.uppercase())
            formatter.format(amount)
        } catch (_: Exception) {
            "$amount ${currencyCode.uppercase()}"
        }
    }

    private data class FallbackAmount(
        val currency: String,
        val unitAmount: Int
    )

    private val fallbackAmounts = mapOf(
        PricingProduct.Plus to FallbackAmount("USD", 699),
        PricingProduct.PremiumMonthly to FallbackAmount("USD", 1299),
        PricingProduct.PremiumAnnual to FallbackAmount("USD", 9999)
    )
}