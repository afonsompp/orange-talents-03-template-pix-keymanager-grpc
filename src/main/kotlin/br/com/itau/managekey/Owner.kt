package br.com.itau.managekey

import javax.persistence.Column
import javax.persistence.Embeddable

@Embeddable
class Owner(
	@Column(nullable = false, name = "account_owner_id")
	val id: String,
	@Column(nullable = false, name = "account_owner_name")
	val name: String,
	@Column(nullable = false, name = "account_owner_cpf")
	val cpf: String

)
