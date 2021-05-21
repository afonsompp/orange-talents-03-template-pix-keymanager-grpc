package br.com.itau.managekey

import br.com.itau.shered.exception.CustomerNotFoundException
import br.com.itau.shered.exception.KeyAlreadyExistsException
import br.com.itau.shered.exception.KeyNotFoundException
import br.com.zup.manage.pix.AccountType
import br.com.zup.manage.pix.AccountType.CONTA_CORRENTE
import br.com.zup.manage.pix.KeyType.EMAIL
import io.micronaut.http.HttpResponse
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
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
	private val request = KeyRequest("email@email.com", uuid, EMAIL, CONTA_CORRENTE)

	private lateinit var savedKey: Key

	@BeforeEach
	internal fun setUp() {
		savedKey = repository.save(Key("abc@def.com", EMAIL, account.toAccount()))
	}

	@AfterEach
	fun after() = repository.deleteAll()

	@Test
	fun `Should return key when erp found client and key don't exists in database`() {
		val bcbOwner = BcbOwnerResponse("NATURAL_PERSON", "Afonso", "123")
		val bcbBankAcc = BcbBankAccountResponse("1", "1", "1", "1")
		val keyHttpResponse = HttpResponse.ok(
			BcbPixResponse("EMAIL", "email@email.com", bcbBankAcc, bcbOwner, LocalDateTime.now())
		)
		inCase(erp.getAccount(anyString(), anyString())).thenReturn(HttpResponse.ok(account))
		inCase(
			bcb.registerKey(BcbCreatePixRequest.of(Key("email@email.com", EMAIL, account.toAccount())))
		).thenReturn(keyHttpResponse)

		val key = service.saveKey(request)

		assertEquals(request.key, key.key)
		assertEquals(request.keyType, key.type)
		with(key.account) {
			assertEquals(AccountType.valueOf(account.tipo), type)
			assertEquals(request.accountType, type)
			assertEquals(account.agencia, agency)
			assertEquals(account.numero, number)
			assertEquals(account.instituicao.ispb, institution.ispb)
			assertEquals(account.instituicao.nome, institution.name)
			assertEquals(request.customerId, owner.id)
			with(account) {
				assertEquals(titular.id, owner.id)
				assertEquals(titular.nome, owner.name)
				assertEquals(titular.cpf, owner.cpf)
			}
		}
	}

	@Test
	fun `Should throw CustomerNotFoundException when erp return request body null`() {
		inCase(erp.getAccount(anyString(), anyString())).thenReturn(HttpResponse.ok(null))

		val error = assertThrows<CustomerNotFoundException> { service.saveKey(request) }
		assertEquals("Client not found", error.message)
	}

	@Test
	fun `Should throw a KeyAlreadyExistsException when key exist in database`() {
		val key = Key("email@email.com", EMAIL, account.toAccount())
		repository.save(key)

		inCase(erp.getAccount(anyString(), anyString())).thenReturn(HttpResponse.ok(account))

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
		val list = repository.saveAll(
			listOf(
				Key("key", EMAIL, account.toAccount()),
				Key("key2", EMAIL, account.toAccount()),
			)
		)
		val response = service.findKeyByCustomer(uuid)

		assertEquals(3, response.size)
		assertEquals(savedKey.key, response[0].key)
		assertEquals(list[0].key, response[1].key)
		assertEquals(list[1].key, response[2].key)
	}

	@Test
	fun `Should return key if key id and customer id exists`() {
		val keyDetails = service.findKeyByKeyIdAndCustomer(savedKey.id!!, savedKey.account.owner.id)

		with(keyDetails) {
			assertEquals(savedKey.id!!, keyId)
			assertEquals(savedKey.key, key)
			assertEquals(savedKey.account.owner.id, customerId)
		}
	}

	@ParameterizedTest
	@ValueSource(strings = ["abc@def.com", "123"])
	fun `Should return key if exists in system or bcb database`(key: String) {
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
