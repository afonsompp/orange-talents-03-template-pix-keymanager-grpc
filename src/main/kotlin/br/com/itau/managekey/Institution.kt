package br.com.itau.managekey

import javax.persistence.Column
import javax.persistence.Embeddable

@Embeddable
class Institution(
	@Column(nullable = false, name = "account_institution_name")
	val name: String,
	@Column(nullable = false, name = "account_institution_ispb")
	val ispb: String
)
