package com.back.nbe12142team06.domain.settlement.dto

import com.back.nbe12142team06.domain.settlement.entity.Settlement
import com.back.nbe12142team06.domain.settlement.entity.SettlementStatus
import java.time.LocalDate

data class SettlementAdminResponse(
    val id: Long,
    val payoutAmount: Int,
    val platformFee: Int,
    val penaltyAmount: Int,
    val status: SettlementStatus,
    val settledAt: LocalDate,
    val applicationId: Long,
    val userId: Long,
    val name: String,
) {
    constructor(settlement: Settlement) : this(
        id = settlement.id!!,
        payoutAmount = settlement.payoutAmount.toInt(),
        platformFee = settlement.platformFee.toInt(),
        penaltyAmount = settlement.penaltyAmount.toInt(),
        status = settlement.settlementStatus,
        settledAt = settlement.settledDate,
        applicationId = settlement.application.id!!,
        userId = settlement.escort.id,
        name = settlement.escort.name
    )
}
