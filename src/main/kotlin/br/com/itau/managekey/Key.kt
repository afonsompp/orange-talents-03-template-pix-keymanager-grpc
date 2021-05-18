package br.com.itau.managekey

import br.com.zup.manage.pix.KeyDetailsResponse
import br.com.zup.manage.pix.KeyDetailsResponse.AccountDetailsResponse
import br.com.zup.manage.pix.KeyType
import com.google.protobuf.Timestamp
import java.time.LocalDateTime
import java.time.ZoneId
import javax.persistence.*

@Entity
@Table(name = "tb_key")
class Key(
	key: String,
	@Column(nullable = false, name = "key_type")
	@Enumerated(EnumType.STRING)
	val type: KeyType,
	@Column(nullable = false)
	@Embedded
	val account: Account,
	@Id
	@GeneratedValue
	val id: Long? = null
) {
	val createdAt = LocalDateTime.now().atZone(ZoneId.of("UTC"))

	@Column(nullable = false, length = 77, name = "key_value")
	var key = key
		private set

	fun updateKey(value: String): Boolean {
		if (isRandom()) {
			key = value
			return true
		}
		return false
	}

	private fun isRandom(): Boolean {
		if (type == KeyType.RANDOM) return true
		return false
	}

	fun toKeyDetailsResponse(): KeyDetailsResponse {
		return KeyDetailsResponse.newBuilder()
			.setKeyId(id!!)
			.setKey(key)
			.setCustomerId(account.owner.id)
			.setKeyType(type)
			.setAccount(
				AccountDetailsResponse.newBuilder()
					.setCustomerName(account.owner.name)
					.setCustomerCPF(account.owner.cpf)
					.setInstitution(account.institution.name)
					.setBranch(account.agency)
					.setNumber(account.number)
					.setAccountType(account.type).build()
			)
			.setCreatedAt(
				Timestamp.newBuilder()
					.setNanos(createdAt.toInstant().nano)
					.setSeconds(createdAt.toInstant().epochSecond)
					.build()
			).build()
	}
}
