package com.back.nbe12142team06.domain.settlement.dto

import com.back.nbe12142team06.domain.post.dto.PostDto
import com.back.nbe12142team06.domain.settlement.entity.Settlement
import com.back.nbe12142team06.domain.settlement.entity.SettlementStatus
import java.time.LocalDate

data class SettlementResponse(
    val id: Long,
    val payoutAmount: Long,
    val platformFee: Long,
    val penaltyAmount: Long,
    val status: SettlementStatus,
    val settledAt: LocalDate,
    val post: PostDto
) {
    constructor(settlement: Settlement) : this(
        id = settlement.requiredId,
        payoutAmount = settlement.payoutAmount.longValueExact(),
        platformFee = settlement.platformFee.longValueExact(),
        penaltyAmount = settlement.penaltyAmount.longValueExact(),
        status = settlement.settlementStatus,
        settledAt = settlement.settledDate,
        post = PostDto(settlement.application.post)
    )
}
