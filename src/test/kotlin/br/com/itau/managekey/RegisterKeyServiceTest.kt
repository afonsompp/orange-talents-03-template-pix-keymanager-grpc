package br.com.itau.managekey

import br.com.itau.shered.exception.CustomerNotFoundException
import br.com.itau.shered.exception.KeyAlreadyExistsException
import br.com.itau.shered.exception.KeyNotFoundException
import br.com.zup.manage.pix.AccountType
import br.com.zup.manage.pix.AccountType.CONTA_CORRENTE
import br.com.zup.manage.pix.KeyType
import br.com.zup.manage.pix.KeyType.EMAIL
import io.micronaut.http.HttpResponse
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject
import org.mockito.Mockito.`when` as inCase

@MicronautTest
internal class RegisterKeyServiceTest {
	@Inject
	lateinit var repository: KeyRepository

	@Inject
	lateinit var service: ManageKeyService

	@Inject
	lateinit var erp: SystemErpHttpClient

	@Inject
	lateinit var bcb: BcbHttpClient

	private val uuid = UUID.randomUUID().toString()
	private val account = AccountResponse(
		"CONTA_CORRENTE",
		InstitutionResponse("a", "a"),
		"1",
		"1",
		OwnerResponse(uuid, "a", "a")
	)
	val request = KeyRequest("abc@def.com", uuid, EMAIL, CONTA_CORRENTE)

	@AfterEach
	fun after() = repository.deleteAll()

	@Test
	fun `Should return key when erp found client and key don't exists in database`() {
		val bcbOwner = BcbOwnerResponse("NATURAL_PERSON", "Afonso", "123")
		val bcbBankAcc = BcbBankAccountResponse("1", "1", "1", "1")
		val keyHttpResponse = HttpResponse.ok(
			BcbPixResponse(
				"EMAIL", "abc@def.com", bcbBankAcc, bcbOwner, LocalDateTime.now()
			)
		)
		inCase(erp.getAccount(anyString(), anyString())).thenReturn(HttpResponse.ok(account))
		inCase(
			bcb.registerKey(
				BcbCreatePixRequest.of(
					Key(
						"abc@def.com",
						EMAIL,
						account.toAccount()
					)
				)
			)
		).thenReturn(keyHttpResponse)

		val key = service.saveKey(request)

		assertEquals(key.key, "abc@def.com")
		assertEquals(key.type, request.keyType)
		assertEquals(key.account.type, request.accountType)
		assertEquals(key.account.type, AccountType.valueOf(account.tipo))
		assertEquals(key.account.agency, account.agencia)
		assertEquals(key.account.number, account.numero)
		assertEquals(key.account.institution.ispb, account.instituicao.ispb)
		assertEquals(key.account.institution.name, account.instituicao.nome)
		assertEquals(key.account.owner.id, request.customerId)
		assertEquals(key.account.owner.id, account.titular.id)
		assertEquals(key.account.owner.name, account.titular.nome)
		assertEquals(key.account.owner.cpf, account.titular.cpf)
	}

	@Test
	fun `Should throw CustomerNotFoundException when erp return request body null`() {
		inCase(erp.getAccount(anyString(), anyString())).thenReturn(HttpResponse.ok(null))

		val error = assertThrows<CustomerNotFoundException> { service.saveKey(request) }
		assertEquals("Client not found", error.message)
	}

	@Test
	fun `Should throw a KeyAlreadyExistsException when key exist in database`() {

		val key = Key("abc@def.com", EMAIL, account.toAccount())

		inCase(erp.getAccount(anyString(), anyString())).thenReturn(HttpResponse.ok(account))
		repository.save(key)

		val error = assertThrows<KeyAlreadyExistsException> { service.saveKey(request) }
		assertEquals("Pix key already exists", error.message)
	}

	@Test
	fun `Should delete key if key id and customer id exists`() {

		val key = Key("abc@def.com", EMAIL, account.toAccount())
		val savedKey = repository.save(key)

		service.deleteKey(savedKey.id!!, key.account.owner.id)

		val optional = repository.findById(savedKey.id!!)

		assertTrue(optional.isEmpty)
	}

	@Test
	fun `Should throws KeyNotFoundException if key don't exists in database`() {

		val key = Key("abc@def.com", EMAIL, account.toAccount())
		val savedKey = repository.save(key)

		val error = assertThrows<KeyNotFoundException> {
			service.deleteKey(savedKey.id!!, UUID.randomUUID().toString())

		}

		assertEquals("Key not found", error.message)
	}

	@Test
	fun `Should throws KeyNotFoundException if key id and customer don't exists in database`() {
		val error = assertThrows<KeyNotFoundException> {
			service.findKeyByKeyIdAndCustomer(1L, UUID.randomUUID().toString())
		}
		assertEquals("Key not found", error.message)
	}

	@Test
	fun `Should throws CustomerNotFoundException if customer don't exists in database`() {
		val error = assertThrows<CustomerNotFoundException> {
			service.findKeyByCustomer(UUID.randomUUID().toString())
		}
		assertEquals("Customer not found", error.message)
	}

	@Test
	fun `Should return key list of a customer`() {
		val uuid = UUID.randomUUID().toString()
		val institution = Institution("a", "a")
		val owner1 = Owner(uuid, "a", "a")
		val owner2 = Owner("321", "a", "a")
		val account1 = Account(CONTA_CORRENTE, "", "", owner1, institution)
		val account2 = Account(CONTA_CORRENTE, "", "", owner2, institution)
		val list = repository.saveAll(
			listOf(
				Key("key", KeyType.EMAIL, account1),
				Key("key2", KeyType.EMAIL, account1),
				Key("key3", KeyType.EMAIL, account2)
			)
		)
		val response = service.findKeyByCustomer(uuid)

		assertEquals(2, response.size)
		assertEquals(list[0].key, response[0].key)
		assertEquals(list[1].key, response[1].key)
	}

	@Test
	fun `Should return key if key id and customer id exists`() {
		val key = repository.save(Key("abc@def.com", EMAIL, account.toAccount()))

		val keyDetails = service.findKeyByKeyIdAndCustomer(key.id!!, key.account.owner.id)

		assertEquals(key.id!!, keyDetails.keyId)
		assertEquals(key.key, keyDetails.key)
		assertEquals(key.account.owner.id, keyDetails.customerId)
	}

	@ParameterizedTest
	@ValueSource(strings = ["key", "123"])
	fun `Should return key if exists in system or bcb database`(key: String) {
		val institution = Institution("a", "60701190")
		val owner = Owner("123", "a", "a")
		val acc = Account(CONTA_CORRENTE, "", "", owner, institution)
		repository.save(Key("key", KeyType.EMAIL, acc))

		val keyHttpResponse = HttpResponse.ok(
			BcbPixResponse(
				"CPF",
				"123",
				BcbBankAccountResponse("60701190", "1", "1", "CACC"),
				BcbOwnerResponse("NATURAL_PERSON", "Afonso", "123"),
				LocalDateTime.now()
			)
		)

		inCase(bcb.findKey(anyString())).thenReturn(keyHttpResponse)

		val result = service.findKeyByKey(key)

		assertEquals(key, result.key)
	}

	@Test
	fun `Should throw KeyNotFoundException if key don't exists in system or bcb database`() {
		inCase(bcb.findKey(anyString())).thenReturn(HttpResponse.notFound())
		val error = assertThrows<KeyNotFoundException> {
			service.findKeyByKey("key")
		}

		assertEquals("Key not found", error.message)
	}

	@MockBean(SystemErpHttpClient::class)
	fun mockClientErp(): SystemErpHttpClient {
		return mock(SystemErpHttpClient::class.java)
	}

	@MockBean(BcbHttpClient::class)
	fun mockClientBcb(): BcbHttpClient {
		return mock(BcbHttpClient::class.java)
	}
}
