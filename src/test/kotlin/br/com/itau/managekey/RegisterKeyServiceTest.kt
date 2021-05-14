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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import java.util.*
import javax.inject.Inject
import org.mockito.Mockito.`when` as inCase

@MicronautTest
internal class RegisterKeyServiceTest {
	@Inject
	lateinit var repository: KeyRepository

	@Inject
	lateinit var service: RegisterKeyService

	@Inject
	lateinit var client: SystemErpHttpClient

	@AfterEach
	fun after() = repository.deleteAll()

	@Test
	fun `Should return key when erp found client and key don't exists in database`() {
		val uuid = UUID.randomUUID().toString()
		val institution = InstitutionResponse("a", "a")
		val owner = OwnerResponse(uuid, "a", "a")
		val account = AccountResponse("CONTA_CORRENTE", institution, "1", "1", owner)
		val request = KeyRequest("abc@def.com", uuid, EMAIL, CONTA_CORRENTE)

		inCase(client.getAccount(anyString(), anyString())).thenReturn(HttpResponse.ok(account))

		val key = service.saveKey(request)

		assertEquals(key.key, "abc@def.com")
		assertEquals(key.type, request.keyType)
		assertEquals(key.account.type, request.accountType)
		assertEquals(key.account.type, AccountType.valueOf(account.tipo))
		assertEquals(key.account.agency, account.agencia)
		assertEquals(key.account.number, account.numero)
		assertEquals(key.account.institution.ispb, institution.ispb)
		assertEquals(key.account.institution.name, institution.nome)
		assertEquals(key.account.owner.id, request.customerId)
		assertEquals(key.account.owner.id, owner.id)
		assertEquals(key.account.owner.name, owner.nome)
		assertEquals(key.account.owner.cpf, owner.cpf)
	}

	@Test
	fun `Should throw CustomerNotFoundException when erp return request body null`() {
		val request = KeyRequest("abc@def.com", UUID.randomUUID().toString(), EMAIL, CONTA_CORRENTE)

		inCase(client.getAccount(anyString(), anyString())).thenReturn(HttpResponse.ok(null))

		val error = assertThrows<CustomerNotFoundException> { service.saveKey(request) }
		assertEquals("Client not found", error.message)
	}

	@Test
	fun `Should throw a KeyAlreadyExistsException when key exist in database`() {
		val institution = InstitutionResponse("a", "a")
		val owner = OwnerResponse("a", "a", "a")
		val account = AccountResponse("CONTA_CORRENTE", institution, "1", "1", owner)
		val request = KeyRequest("abc@def.com", UUID.randomUUID().toString(), EMAIL, CONTA_CORRENTE)
		val key = Key("abc@def.com", EMAIL, account.toAccount())

		inCase(client.getAccount(anyString(), anyString())).thenReturn(HttpResponse.ok(account))
		repository.save(key)

		val error = assertThrows<KeyAlreadyExistsException> { service.saveKey(request) }
		assertEquals("Pix key already exists", error.message)
	}

	@Test
	fun `Should delete key if key id and customer id exists`() {
		val institution = InstitutionResponse("a", "a")
		val owner = OwnerResponse(UUID.randomUUID().toString(), "a", "a")
		val account = AccountResponse("CONTA_CORRENTE", institution, "1", "1", owner)

		val key = Key("abc@def.com", EMAIL, account.toAccount())
		val savedKey = repository.save(key)

		service.deleteKey(savedKey.id!!, key.account.owner.id)

		val optional = repository.findById(savedKey.id!!)

		assertTrue(optional.isEmpty)
	}

	@Test
	fun `Should throws KeyNotFoundException if key don't exists in database`() {
		val institution = InstitutionResponse("a", "a")
		val owner = OwnerResponse(UUID.randomUUID().toString(), "a", "a")
		val account = AccountResponse("CONTA_CORRENTE", institution, "1", "1", owner)

		val key = Key("abc@def.com", EMAIL, account.toAccount())
		val savedKey = repository.save(key)

		val error = assertThrows<KeyNotFoundException> {
			service.deleteKey(savedKey.id!!, UUID.randomUUID().toString())

		}

		assertEquals("Key not found", error.message)
	}

	@MockBean(SystemErpHttpClient::class)
	fun mockClient(): SystemErpHttpClient {
		return mock(SystemErpHttpClient::class.java)
	}
}
