package br.com.itau.managekey

import br.com.zup.manage.pix.KeyType
import javax.persistence.*

@Entity
@Table(name = "tb_key")
class Key(
	@Column(nullable = false, length = 77, name = "key_value")
	val key: String,
	@Column(nullable = false, name = "key_type")
	@Enumerated(EnumType.STRING)
	val type: KeyType,
	@Column(nullable = false)
	@Embedded
	val account: Account,
	@Id
	@GeneratedValue
	val id: Long? = null
)
