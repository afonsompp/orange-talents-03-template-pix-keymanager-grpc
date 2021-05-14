package br.com.itau.managekey

import br.com.zup.manage.pix.AccountType

data class AccountResponse(
	val tipo: String,
	val instituicao: InstitutionResponse,
	val agencia: String,
	val numero: String,
	val titular: OwnerResponse
) {
}

data class InstitutionResponse(
	val nome: String,
	val ispb: String
) {
}

data class OwnerResponse(
	val id: String,
	val nome: String,
	val cpf: String
) {
}
