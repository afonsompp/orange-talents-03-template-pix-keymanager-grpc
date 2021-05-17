package br.com.itau.managekey

import br.com.zup.manage.pix.AccountType
import br.com.zup.manage.pix.KeyType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class KeyTest {

	@ParameterizedTest
	@CsvSource("key2,EMAIL,false", "UUID,RANDOM,true")
	fun `Should modify key if type is random`(value: String, keyType: KeyType, result: Boolean) {
		val institution = Institution("a", "a")
		val owner = Owner("123", "a", "a")
		val account = Account(AccountType.CONTA_CORRENTE, "", "", owner, institution)
		val key = Key("key", keyType, account)



		assertEquals(result, key.updateKey(value))
		if (result) {
			assertEquals(value, key.key)
		}
	}
}
