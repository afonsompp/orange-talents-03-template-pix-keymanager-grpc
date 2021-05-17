package br.com.itau.managekey

import br.com.zup.manage.pix.KeyType
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
}
