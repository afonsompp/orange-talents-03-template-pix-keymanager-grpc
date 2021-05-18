package br.com.itau.managekey

import br.com.zup.manage.pix.AccountType.CONTA_CORRENTE
import br.com.zup.manage.pix.KeyType
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import javax.inject.Inject

@MicronautTest(transactional = false)
class KeyRepositoryTest {
	@Inject
	lateinit var repository: KeyRepository

	val account = Account(
		CONTA_CORRENTE,
		"",
		"",
		Owner("123", "a", "a"),
		Institution("a", "a")
	)

	@BeforeEach
	fun setUp() {
	}

	@AfterEach
	fun after() = repository.deleteAll()

	@ParameterizedTest
	@CsvSource("key,true", "kkey,false")
	fun `Should verify if a key already exists in database`(value: String, result: Boolean) {

		val key = Key(value, KeyType.EMAIL, account)

		repository.save(key)
		assertEquals(result, repository.existsByKey("key"))
	}

	@ParameterizedTest
	@CsvSource(",123,false", "1,122,true", "10,123,true")
	fun `Should return key based on id and customer id or null`(
		keyId: Long?,
		customerId: String,
		result: Boolean
	) {
		val key = Key("key", KeyType.EMAIL, account)
		val id = keyId ?: repository.save(key).id!!

		val possibleKey = repository.findByIdAndCustomerId(id, customerId)
		assertEquals(result, possibleKey == null)

		if (possibleKey != null) {
			assertEquals(id, possibleKey.id)
			assertEquals(customerId, possibleKey.account.owner.id)
		}
	}

	@ParameterizedTest
	@CsvSource("123,false,2", "124,true,0")
	fun `Should return list of key of a customer`(customerId: String, isEmpty: Boolean, size: Int) {
		val institution = Institution("a", "a")
		val owner1 = Owner("123", "a", "a")
		val owner2 = Owner("321", "a", "a")
		val account1 = Account(CONTA_CORRENTE, "", "", owner1, institution)
		val account2 = Account(CONTA_CORRENTE, "", "", owner2, institution)
		repository.saveAll(
			listOf(
				Key("key", KeyType.EMAIL, account1),
				Key("key2", KeyType.EMAIL, account1),
				Key("key3", KeyType.EMAIL, account2)
			)
		)
		val keys = repository.findByCustomerId(customerId)

		assertEquals(isEmpty, keys.isNullOrEmpty())
		assertEquals(size, keys.size)
	}

	@ParameterizedTest
	@CsvSource("key,false", "keyy,true")
	fun `Should return key by key value`(key: String, isNull: Boolean) {
		val key1 = Key("key", KeyType.EMAIL, account)
		val key2 = Key("key2", KeyType.EMAIL, account)
		repository.saveAll(listOf(key1, key2))

		val result = repository.findByKey(key)

		assertEquals(isNull, result?.key.isNullOrBlank())
		if (result != null) {
			assertEquals(result.key, result.key)
		}
	}
}
