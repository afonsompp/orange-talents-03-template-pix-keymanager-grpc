package br.com.itau.managekey

import javax.persistence.Column
import javax.persistence.Embeddable

@Embeddable
class Institution(
	@Column(nullable = false, name = "account_institution_name")
	val name: String,
	@Column(nullable = false, name = "account_institution_ispb")
	val ispb: String
) {
	companion object {
		fun getNameFromParticipant(participant: String): String {
			return when (participant) {
				"60701190" -> "ITAÚ UNIBANCO S.A."
				else -> throw IllegalArgumentException("participant not found")
			}
		}
	}
}
