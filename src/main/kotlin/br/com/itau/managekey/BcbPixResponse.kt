package br.com.itau.managekey

import br.com.zup.manage.pix.KeyDetailsResponse
import br.com.zup.manage.pix.KeyDetailsResponse.AccountDetailsResponse
import br.com.zup.manage.pix.KeyType
import com.google.protobuf.Timestamp
import java.time.LocalDateTime

data class BcbPixResponse(
	val keyType: String,
	val key: String,
	val bankAccount: BcbBankAccountResponse,
	val owner: BcbOwnerResponse,
	val createdAt: LocalDateTime
) {
	fun toKeyDetailsResponse(): KeyDetailsResponse {
		return KeyDetailsResponse.newBuilder()
			.setKey(key)
			.setKeyType(KeyType.valueOf(keyType))
			.setAccount(
				AccountDetailsResponse.newBuilder()
					.setCustomerName(owner.name)
					.setCustomerCPF(owner.taxIdNumber)
					.setInstitution(Institution.getNameFromParticipant(bankAccount.participant))
					.setBranch(bankAccount.branch)
					.setNumber(bankAccount.accountNumber)
					.setAccountType(BcbAccountType.valueOf(bankAccount.accountType).grpcAccountType)
					.build()
			)
			.setCreatedAt(
				Timestamp.newBuilder()
					.setNanos(createdAt.nano)
					.setSeconds(createdAt.second.toLong())
					.build()
			).build()
	}
}

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
