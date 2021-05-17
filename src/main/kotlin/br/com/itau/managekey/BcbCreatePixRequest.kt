package br.com.itau.managekey

import br.com.itau.shered.extension.toBcbAccountType

data class BcbCreatePixRequest(
	val keyType: String,
	val key: String,
	val bankAccount: BcbBankAccountRequest,
	val owner: BcbOwnerRequest
) {
	companion object {
		fun of(key: Key): BcbCreatePixRequest {
			val owner = BcbOwnerRequest(key.account.owner.name, key.account.owner.cpf)
			val account = BcbBankAccountRequest(
				key.account.agency,
				key.account.number,
				key.account.type.toBcbAccountType()
			)
			return BcbCreatePixRequest(key.type.name, key.key, account, owner)
		}
	}
}

data class BcbBankAccountRequest(
	val branch: String,
	val accountNumber: String,
	val accountType: BcbAccountType,
	val participant: String = "60701190",
)

data class BcbOwnerRequest(
	val name: String,
	val taxIdNumber: String,
	val type: String = "NATURAL_PERSON",
)
