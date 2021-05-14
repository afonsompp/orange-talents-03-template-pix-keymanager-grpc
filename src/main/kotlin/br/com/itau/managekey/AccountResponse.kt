package br.com.itau.managekey

import br.com.zup.manage.pix.AccountType

data class AccountResponse(
	val tipo: String,
	val instituicao: InstitutionResponse,
	val agencia: String,
	val numero: String,
	val titular: OwnerResponse
) {
	fun toAccount(): Account = Account(
		AccountType.valueOf(tipo),
		agencia,
		numero,
		titular.toOwner(),
		instituicao.toInstitution()
	)
}

data class InstitutionResponse(
	val nome: String,
	val ispb: String
) {
	fun toInstitution(): Institution = Institution(nome, ispb)
}

data class OwnerResponse(
	val id: String,
	val nome: String,
	val cpf: String
) {
	fun toOwner(): Owner = Owner(id, nome, cpf)
}
