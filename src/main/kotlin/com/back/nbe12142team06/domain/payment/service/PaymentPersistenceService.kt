package com.back.nbe12142team06.domain.payment.service

import com.back.nbe12142team06.domain.application.repository.ApplicationRepository
import com.back.nbe12142team06.domain.payment.dto.TossConfirmResponse
import com.back.nbe12142team06.domain.payment.entity.Payment
import com.back.nbe12142team06.domain.payment.entity.PaymentStatus
import com.back.nbe12142team06.domain.payment.repository.PaymentRepository
import com.back.nbe12142team06.domain.payment.repository.dto.PaymentDailyAnalyticsDto
import com.back.nbe12142team06.domain.post.entity.Post
import com.back.nbe12142team06.global.exception.ForbiddenException
import com.back.nbe12142team06.global.exception.InvalidException
import com.back.nbe12142team06.global.exception.NotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class PaymentPersistenceService(
    private val paymentRepository: PaymentRepository,
    private val applicationRepository: ApplicationRepository
) {

    @Transactional
    fun confirmUpdate(userId: Long, paymentId: Long, amount: String): Payment {
        val payment = findById(userId, paymentId)

        if (payment.paymentStatus == PaymentStatus.DONE) {
            throw InvalidException(41, "이미 결제를 완료하셨습니다.")
        }

        if (payment.paymentStatus == PaymentStatus.CANCELED) {
            throw InvalidException(43, "취소된 결제입니다.")
        }

        // 1. 검증 로직
        payment.verifyAmount(amount)

        if (confirmUpdateStatus(payment.requiredId) <= 0) {
            throw InvalidException(44, "결제 대기 중인 데이터가 없습니다.")
        }
        payment.statusUpdate(PaymentStatus.IN_PROGRESS)

        return payment
    }

    @Transactional
    fun paymentSaveDb(
        response: TossConfirmResponse,
        paymentId: Long,
        tossPaymentKey: String,
        tossOrderId: String
    ): Payment {
        val payment: Payment = paymentRepository.findByIdFetchJoin(paymentId)
            ?: throw NotFoundException(41, "결제 정보를 찾을 수 없습니다.")
        // 승인 시 상태 변경, 더티 체킹으로 자동 변경
        payment.approvePayment(tossOrderId, tossPaymentKey, response.method)
        return payment
    }

    @Transactional
    fun paymentCancelDb(paymentId: Long, cancelReason: String, createNewPayment: Boolean): Payment {
        val payment: Payment = paymentRepository.findByIdOrNull(paymentId)
            ?: throw NotFoundException(41, "결제 정보를 찾을 수 없습니다.")
        val newPayment = payment.cancelPayment(cancelReason)
        if (createNewPayment) {
            return paymentRepository.save(newPayment)
        }
        return payment
    }

    @Transactional
    fun paymentPartialCancelDb(paymentId: Long, cancelReason: String, amount: BigDecimal): Payment {
        val payment: Payment = paymentRepository.findByIdOrNull(paymentId)
            ?: throw NotFoundException(41, "결제 정보를 찾을 수 없습니다.")
        return payment.cancelPartialPayment(cancelReason, amount)
    }

    @Transactional
    fun cancelPreempt(paymentId: Long): Int = paymentRepository.paymentCanceling(paymentId, LocalDateTime.now())

    @Transactional
    fun createPayment(payment: Payment): Payment {
        return paymentRepository.save(payment)
    }

    fun findByPostId(postId: Long): List<Payment> {
        return paymentRepository.findSuccessPayByPostId(postId)
    }

    fun findById(userId: Long, paymentId: Long): Payment {
        val payment: Payment = (paymentRepository.findByIdFetchJoin(paymentId)
            ?: throw NotFoundException(41, "결제 정보를 찾을 수 없습니다."))

        if (payment.post.client.id != userId) {
            throw ForbiddenException(41, "사용자의 결제 정보가 아닙니다.")
        }

        return payment
    }

    @Transactional
    fun updateAmount(paymentId: Long, amount: String): Payment {
        val payment: Payment = paymentRepository.findByIdOrNull(paymentId)
            ?: throw NotFoundException(41, "결제 정보를 찾을 수 없습니다.")
        payment.updateAmount(amount.toInt())
        return payment
    }

    // 공고 삭제 트랜잭션이랑 다른 트랜잭션으로 진행
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun updateDeleteStatus(postId: Long): Payment {
        val payment: Payment = paymentRepository.findByPostId(postId)
            ?: throw NotFoundException(41, "결제 정보를 찾을 수 없습니다.")
        payment.statusUpdate(PaymentStatus.DELETED)
        payment.updateCanceledAt()
        return payment
    }

    fun findDeletedAll(): List<Payment> {
        return paymentRepository.findDeletedAll()
    }

    fun getNotPaid(userId: Long): List<Payment> {
        return paymentRepository.findNotPaidByUserId(userId)
    }

    @Transactional
    fun confirmUpdateStatus(paymentId: Long): Int {
        return paymentRepository.paymentInProgress(paymentId)
    }

    @Transactional
    fun failedConfirm(paymentId: Long) {
        paymentRepository.deleteById(paymentId)
    }

    @Transactional
    fun updateAmountForModifyPost(postId: Long, post: Post) {
        val payment: Payment = paymentRepository.findByPostIdAndPaymentStatus(postId, PaymentStatus.READY).first()
        payment.update(post.totalPay.toInt(), post.escortHours)
    }

    fun getDailyAnalytics(startDate: LocalDateTime, endDate: LocalDateTime): List<PaymentDailyAnalyticsDto> {
        return paymentRepository.findDailyAnalytics(startDate, endDate)
    }

    fun getAnalyticsByPeriod(startDateTime: LocalDateTime, endDateTime: LocalDateTime): List<Payment> {
        return paymentRepository.findAnalyticsByPeriod(startDateTime, endDateTime)
    }

    fun getPaymentsForAdmin(
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime,
        pageable: Pageable
    ): Page<Payment> {
        return paymentRepository.findPaymentsForAdmin(startDateTime, endDateTime, pageable)
    }

    @Transactional
    fun updateStatusForAdmin(paymentId: Long, status: PaymentStatus) {
        val payment: Payment = paymentRepository.findByIdOrNull(paymentId)
            ?: throw NotFoundException(41, "결제 정보를 찾을 수 없습니다.")

        payment.statusUpdate(status)
    }

    @Transactional
    fun rejectApplication(post: Post) {
        applicationRepository.rejectAllByPost(post, LocalDateTime.now())
    }
}
