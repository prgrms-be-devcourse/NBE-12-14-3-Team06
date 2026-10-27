package com.back.nbe12142team06.domain.payment.entity

import com.back.nbe12142team06.domain.post.entity.Post
import com.back.nbe12142team06.domain.settlement.entity.Settlement
import com.back.nbe12142team06.global.entity.BaseTimeEntity
import com.back.nbe12142team06.global.exception.InvalidException
import jakarta.persistence.*
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 결제 엔티티
 * 의뢰인 -> 플랫폼 결제
 */
// 공고 목록의 "공고별 최신 결제" 하위 쿼리(MAX(id) WHERE post_id = ?)가 payment 전체를 훑지 않게 한다
@Entity
@Table(name = "payment", indexes = [Index(name = "idx_payment_post_id_id", columnList = "post_id, id")])
class Payment(
    id: Long? = null,
    amount: BigDecimal,
    hourlyPaySnapshot: Int,
    hours: BigDecimal,
    post: Post,
    method: String? = null,
    paymentStatus: PaymentStatus = PaymentStatus.READY,
    orderId: String? = null,
    paymentKey: String? = null,
    approvedAt: LocalDateTime? = null,
    balanceAmount: BigDecimal = BigDecimal.ZERO,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = id
        protected set

    @Column(nullable = false, precision = 15, scale = 0)
    var amount: BigDecimal = amount
        protected set

    // 시간 당 금액
    @Column(nullable = false)
    var hourlyPaySnapshot: Int = hourlyPaySnapshot
        protected set

    // 동행 시간
    @field:DecimalMin(value = "0.0")
    @field:DecimalMax(value = "24.0")
    @Column(nullable = false, precision = 3, scale = 1)
    var hours: BigDecimal = hours
        protected set

    // 토스 주문 번호
    @Column(unique = true)
    var orderId: String? = orderId
        protected set

    // 토스 결제 키
    @Column(unique = true)
    var paymentKey: String? = paymentKey
        protected set

    // 결제 수단, 현재 서비스는 카드 또는 계좌이체
    var method: String? = method
        protected set

    // 결제 처리 상태, 기본값 준비 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var paymentStatus = paymentStatus
        protected set

    // 승인 날짜
    var approvedAt: LocalDateTime? = approvedAt
        protected set

    // 취소 날짜
    var canceledAt: LocalDateTime? = null
        protected set

    // 취소 사유
    var cancelReason: String? = null
        protected set

    // 결제 취소 할 수 있는 금액, 최소 0원 이상이어야 함
    @Column(
        nullable = false,
        check = [CheckConstraint(name = "chk_balance_amount", constraint = "balance_amount >= 0")],
        precision = 15,
        scale = 0
    )
    var balanceAmount: BigDecimal = balanceAmount
        protected set

    // 결제를 취소/재결제 할 수 있기 때문에 N:1
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "post_id")
    var post: Post = post
        protected set

    // 정산은 결제, 추가 결제 등 여러 개 생길 수 있음
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_id")
    var settlement: Settlement? = null
        protected set

    // id가 nullable로 처리되는데 실제 로직 중 대부분은 id가 null일 수 없습니다.
    // 그래서 id가 null이면 IllegalStateException을 반환합니다.
    // 커스텀 예외가 아닌 이유는 비즈니스 예외가 아닌 개발자의 실수로 발생하는 예외이기 때문입니다.
    val requiredId: Long
        get() = id ?: error("ID가 NULL입니다.")

    fun statusUpdate(status: PaymentStatus) {
        if (!paymentStatus.canTransitionTo(status)) {
            throw InvalidException(
                72, "'${this.paymentStatus.description}' 상태의 결제는 '${status.description}' 상태로 바꿀 수 없습니다."
            )
        }
        this.paymentStatus = status
    }

    // 결제 승인
    fun approvePayment(orderId: String, paymentKey: String, method: String?) {
        this.statusUpdate(PaymentStatus.DONE)
        this.approvedAt = LocalDateTime.now()
        this.orderId = orderId
        this.paymentKey = paymentKey
        this.method = method
        this.balanceAmount = this.amount
    }

    // 결제 취소 -> 새로운 결제 데이터 반환
    fun cancelPayment(cancelReason: String): Payment {
        this.statusUpdate(PaymentStatus.CANCELED)
        this.balanceAmount = BigDecimal.ZERO
        updateCanceledAt()
        this.cancelReason = cancelReason
        return Payment(
            amount = this.amount, hourlyPaySnapshot = this.hourlyPaySnapshot, hours = this.hours, post = this.post
        )
    }

    // 결제 부분 취소
    fun cancelPartialPayment(cancelReason: String, cancelAmount: BigDecimal): Payment {
        this.cancelReason = cancelReason
        // this.amount = this.amount.subtract(cancelAmount);
        this.balanceAmount = this.balanceAmount.subtract(cancelAmount)
        this.statusUpdate(PaymentStatus.PARTIAL_CANCELED)
        return this
    }

    fun updateSettlement(settlement: Settlement) {
        this.settlement = settlement
    }

    fun updateAmount(amount: Long) {
        this.amount = BigDecimal.valueOf(amount)
    }

    fun updateCanceledAt() {
        this.canceledAt = LocalDateTime.now()
    }

    fun update(amount: Long, hours: BigDecimal) {
        updateAmount(amount)
        this.hours = hours
    }

    fun verifyAmount(amount: String): Boolean {
        if (this.amount.compareTo(BigDecimal(amount)) != 0) {
            log.warn("결제 금액 정보 불일치 - 요청 amount: {}, 저장된 amount: {}", amount, this.amount)
            throw InvalidException(42, "결제 금액 정보가 유효하지 않습니다.")
        }
        return true
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(Payment::class.java)
    }
}
