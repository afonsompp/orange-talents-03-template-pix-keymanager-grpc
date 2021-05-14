package br.com.itau.managekey

import br.com.zup.manage.pix.AccountType
import javax.persistence.*

@Embeddable
class Account(
	@Column(nullable = false, name = "account_type")
	@Enumerated(EnumType.STRING)
	val type: AccountType,
	@Column(nullable = false, name = "account_agency")
	val agency: String,
	@Column(nullable = false, name = "account_number")
	val number: String,
	@Embedded
	val owner: Owner,
	@Embedded
	val institution: Institution,
)
