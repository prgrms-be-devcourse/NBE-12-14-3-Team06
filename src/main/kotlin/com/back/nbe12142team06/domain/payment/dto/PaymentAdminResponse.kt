package com.back.nbe12142team06.domain.payment.dto

import com.back.nbe12142team06.domain.payment.entity.Payment
import com.back.nbe12142team06.domain.payment.entity.PaymentStatus
import java.math.BigDecimal
import java.time.LocalDateTime

data class PaymentAdminResponse(
    val id: Long?,
    val amount: Long,
    val balanceAmount: Long,
    val hourlyPaySnapshot: Int,
    val hours: BigDecimal,
    val orderId: String?,
    val paymentStatus: PaymentStatus,
    val approvedAt: LocalDateTime?,
    val canceledAt: LocalDateTime?,
    val cancelReason: String?,
    val postId: Long?,
    val postTitle: String,
    val userId: Long,
    val name: String
) {
    constructor(payment: Payment): this(
        payment.id,
        payment.amount.longValueExact(),
        payment.balanceAmount.longValueExact(),
        payment.hourlyPaySnapshot,
        payment.hours,
        payment.orderId,
        payment.paymentStatus,
        payment.approvedAt,
        payment.canceledAt,
        payment.cancelReason,
        payment.post.id,
        payment.post.title,
        payment.post.client.id,
        payment.post.client.name
    )
}
