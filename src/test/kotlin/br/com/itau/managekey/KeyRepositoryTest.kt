package br.com.itau.managekey

import br.com.zup.manage.pix.AccountType.CONTA_CORRENTE
import br.com.zup.manage.pix.KeyType
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import javax.inject.Inject

@MicronautTest(transactional = false)
class KeyRepositoryTest {
	@Inject
	lateinit var repository: KeyRepository

	@ParameterizedTest
	@CsvSource("key,true", "kkey,false")
	fun `Should verify if a key already exists in database`(value: String, result: Boolean) {
		val institution = Institution("a", "a")
		val owner = Owner("a", "a", "a")
		val account = Account(CONTA_CORRENTE, "", "", owner, institution)
		val key = Key(value, KeyType.EMAIL, account)

		repository.save(key)

		assertEquals(result, repository.existsByKey("key"))

		repository.deleteAll()
	}
}
