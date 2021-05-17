package br.com.itau.managekey

import java.time.LocalDateTime

data class BcbCreatePixResponse(
	val keyType: String,
	val key: String,
	val bankAccount: BcbBankAccountResponse,
	val owner: BcbOwnerResponse,
	val createdAt: LocalDateTime
)

data class BcbBankAccountResponse(
	val participant: String,
	val branch: String,
	val accountNumber: String,
	val accountType: String,
)

data class BcbOwnerResponse(
	val type: String,
	val name: String,
	val taxIdNumber: String,
)
