package com.back.nbe12142team06.domain.payment.dto

import com.back.nbe12142team06.domain.payment.entity.Payment
import com.back.nbe12142team06.domain.payment.entity.PaymentStatus
import java.math.BigDecimal
import java.time.LocalDateTime

data class PaymentResponse(
    val id: Long?,
    val amount: Long,
    val hourlyPaySnapshot: Int,
    val hours: BigDecimal,
    val orderId: String?,
    val paymentStatus: PaymentStatus,
    val approvedAt: LocalDateTime?,
    val canceledAt: LocalDateTime?,
    val cancelReason: String?,
    val postId: Long?
) {
    constructor(payment: Payment) : this(
        payment.id,
        payment.amount.longValueExact(),
        payment.hourlyPaySnapshot,
        payment.hours,
        payment.orderId,
        payment.paymentStatus,
        payment.approvedAt,
        payment.canceledAt,
        payment.cancelReason,
        payment.post.id
    )
}
