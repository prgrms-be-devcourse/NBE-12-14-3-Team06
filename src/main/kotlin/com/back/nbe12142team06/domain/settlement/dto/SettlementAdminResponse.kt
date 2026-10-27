package com.back.nbe12142team06.domain.settlement.dto

import com.back.nbe12142team06.domain.settlement.entity.Settlement
import com.back.nbe12142team06.domain.settlement.entity.SettlementStatus
import java.time.LocalDate

data class SettlementAdminResponse(
    val id: Long,
    val payoutAmount: Long,
    val platformFee: Long,
    val penaltyAmount: Long,
    val status: SettlementStatus,
    val settledAt: LocalDate,
    val applicationId: Long,
    val userId: Long,
    val name: String,
) {
    constructor(settlement: Settlement) : this(
        id = settlement.id!!,
        payoutAmount = settlement.payoutAmount.longValueExact(),
        platformFee = settlement.platformFee.longValueExact(),
        penaltyAmount = settlement.penaltyAmount.longValueExact(),
        status = settlement.settlementStatus,
        settledAt = settlement.settledDate,
        applicationId = settlement.application.id!!,
        userId = settlement.escort.id,
        name = settlement.escort.name
    )
}
