package br.com.itau.managekey

import br.com.zup.manage.pix.*
import br.com.zup.manage.pix.AccountType.CONTA_CORRENTE
import br.com.zup.manage.pix.AccountType.UNKNOWN_ACCOUNT
import br.com.zup.manage.pix.KeyType.*
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.Status.*
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.http.HttpResponse
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import java.time.LocalDateTime
import java.util.*
import java.util.stream.Stream
import javax.inject.Inject
import org.mockito.Mockito.`when` as inCase

@MicronautTest(transactional = false)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PixManagerEndpointTest {
	@Inject
	lateinit var client: SystemErpHttpClient

	@Inject
	lateinit var bcb: BcbHttpClient

	@Inject
	lateinit var repository: KeyRepository

	@Inject
	lateinit var grpc: ManagePixServiceGrpc.ManagePixServiceBlockingStub

	private val request = RegisterKeyRequest.newBuilder()
		.setValue("02654220273")
		.setCustomerId(UUID.randomUUID().toString())
		.setType(CPF)
		.setAccountType(CONTA_CORRENTE)

	private val account = AccountResponse(
		"CONTA_CORRENTE",
		InstitutionResponse("a", "a"),
		"1",
		"1",
		OwnerResponse(UUID.randomUUID().toString(), "a", "a")
	)

	@AfterEach
	fun after() = repository.deleteAll()

	@Test
	fun `Should save key in database and return a valid response`() {
		val keyHttpResponse = HttpResponse.ok(
			BcbPixResponse(
				"EMAIL",
				"abc@def.com",
				BcbBankAccountResponse("1", "1", "1", "1"),
				BcbOwnerResponse("NATURAL_PERSON", "Afonso", "123"),
				LocalDateTime.now()
			)
		)

		inCase(client.getAccount(anyString(), anyString())).thenReturn(HttpResponse.ok(account))
		inCase(
			bcb.registerKey(
				BcbCreatePixRequest.of(
					Key(
						"02654220273",
						CPF,
						account.toAccount()
					)
				)
			)
		).thenReturn(keyHttpResponse)

		grpc.registerKey(request.build())

		val error = assertThrows<StatusRuntimeException> { grpc.registerKey(request.build()) }

		assertEquals(ALREADY_EXISTS.code, error.status.code)
		assertEquals("Pix key already exists", error.status.description)
	}

	@Test
	fun `Should throw error when key already exists in database`() {
		val keyHttpResponse = HttpResponse.ok(
			BcbPixResponse(
				"EMAIL",
				"abc@def.com",
				BcbBankAccountResponse("1", "1", "1", "1"),
				BcbOwnerResponse("NATURAL_PERSON", "Afonso", "123"),
				LocalDateTime.now()
			)
		)
		inCase(client.getAccount(anyString(), anyString()))
			.thenReturn(HttpResponse.ok(account))
		inCase(
			bcb.registerKey(
				BcbCreatePixRequest.of(
					Key(
						"02654220273",
						CPF,
						account.toAccount()
					)
				)
			)
		).thenReturn(keyHttpResponse)

		val response = grpc.registerKey(request.build())

		assertEquals("02654220273", response.key)
	}

	@ParameterizedTest
	@MethodSource("provideValues")
	fun `Should return throw error when are invalid values`(
		keyRequest: RegisterKeyRequest,
		acc: AccountResponse?,
		status: Status,
		message: String
	) {
		inCase(client.getAccount(anyString(), anyString())).thenReturn(HttpResponse.ok(acc))

		val error = assertThrows<StatusRuntimeException> { grpc.registerKey(keyRequest) }

		assertEquals(status.code, error.status.code)
		assertEquals(message, error.status.description)
	}

	@Test
	fun `Should delete key if client have this key`() {
		val key = repository.save(Key("abc@def.com", EMAIL, account.toAccount()))

		val requestRemove = RemoveKeyRequest.newBuilder()
			.setKeyId(key.id!!)
			.setCustomerId(key.account.owner.id)
			.build()
		val response = grpc.removeKey(requestRemove)
		assertEquals("Success", response.message)
	}

	@Test
	fun `Should return key if client have this key`() {
		val key = repository.save(Key("abc@def.com", EMAIL, account.toAccount()))

		val request = KeyDetailsRequest.newBuilder()
			.setPixId(
				KeyDetailsRequest.PixKey.newBuilder()
					.setKeyId(key.id!!)
					.setCustomerId(key.account.owner.id)
			)
			.build()
		val response = grpc.findKey(request)

		assertEquals(key.id!!, response.keyId)
		assertEquals(key.account.owner.id, response.customerId)
	}

	@Test
	fun `Should throw KeyNotFoundException when find by PixId if key don't exists in database`() {

		val request = KeyDetailsRequest.newBuilder()
			.setPixId(
				KeyDetailsRequest.PixKey.newBuilder()
					.setKeyId(1L)
					.setCustomerId(UUID.randomUUID().toString())
			)
			.build()
		val error = assertThrows<StatusRuntimeException> { grpc.findKey(request) }


		assertEquals(NOT_FOUND.code, error.status.code)
		assertEquals("Key not found", error.status.description)
	}

	@Test
	fun `Should throw KeyNotFoundException if key don't exists in database and bcb`() {
		inCase(bcb.findKey(anyString())).thenReturn(HttpResponse.notFound())
		val request = KeyDetailsRequest.newBuilder().setKey("124").build()
		val error = assertThrows<StatusRuntimeException> { grpc.findKey(request) }

		assertEquals(NOT_FOUND.code, error.status.code)
		assertEquals("Key not found", error.status.description)
	}

	@ParameterizedTest
	@ValueSource(strings = ["key", "123"])
	fun `Should return key if exists in system or bcb database when find by key`(key: String) {
		val acc = Account(
			CONTA_CORRENTE,
			"",
			"",
			Owner("123", "a", "a"),
			Institution("a", "60701190")
		)
		repository.save(Key("key", EMAIL, acc))

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
		val request = KeyDetailsRequest.newBuilder().setKey(key).build()
		val result = grpc.findKey(request)

		assertEquals(key, result.key)
	}

	@Test
	fun `Should return key if exists in database when find by key and customer id`() {
		val acc = Account(
			CONTA_CORRENTE,
			"",
			"",
			Owner(UUID.randomUUID().toString(), "a", "a"),
			Institution("a", "60701190")
		)
		val key = repository.save(Key("key", EMAIL, acc))

		val request =
			KeyDetailsRequest.newBuilder()
				.setPixId(
					KeyDetailsRequest.PixKey.newBuilder()
						.setKeyId(key.id!!)
						.setCustomerId(key.account.owner.id)
				).build()
		val result = grpc.findKey(request)

		assertEquals(key.key, result.key)
	}

	@Test
	fun `Should throws CustomerNotFoundException if customer don't exists in database`() {
		val error = assertThrows<StatusRuntimeException> {
			grpc.listKeysOfCustomer(
				ListOfKeysRequest.newBuilder()
					.setCustomerId(UUID.randomUUID().toString())
					.build()
			)
		}
		assertEquals(NOT_FOUND.code, error.status.code)
		assertEquals("Customer not found", error.status.description)
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
				Key("key", EMAIL, account1),
				Key("key2", EMAIL, account1),
				Key("key3", EMAIL, account2)
			)
		)
		val response =
			grpc.listKeysOfCustomer(ListOfKeysRequest.newBuilder().setCustomerId(uuid).build())

		assertEquals(2, response.keyCount)
		assertEquals(list[0].key, response.keyList[0].key)
		assertEquals(list[1].key, response.keyList[1].key)
	}

	fun provideValues(): Stream<Arguments> {

		return Stream.of(
			//customer not found case
			Arguments.of(
				request.build(),
				null,
				NOT_FOUND,
				"Client not found"
			),
			// Invalid Key type case
			Arguments.of(
				request.clone().setType(UNKNOWN_TYPE).build(),
				account,
				INVALID_ARGUMENT,
				"Key type cannot be UNKNOWN_TYPE"
			),
			// Invalid account type case
			Arguments.of(
				request.clone().setAccountType(UNKNOWN_ACCOUNT).build(),
				account,
				INVALID_ARGUMENT,
				"The account type cannot be UNKNOWN_TYPE"
			),
			// Invalid CPF case
			Arguments.of(
				request.clone().setType(CPF).setValue("02654220173").build(),
				account,
				INVALID_ARGUMENT,
				"The CPF is invalid"
			),
			// Invalid Phone number case
			Arguments.of(
				request.clone().setType(PHONE).setValue("5569993551645").build(),
				account,
				INVALID_ARGUMENT,
				"The Phone number is invalid"
			),
			//Invalid email case
			Arguments.of(
				request.clone().setType(EMAIL).setValue("abc@s.").build(),
				account,
				INVALID_ARGUMENT,
				"The Email is invalid"
			),
			// Random key case
			Arguments.of(
				request.clone().setType(RANDOM).setValue(".").build(),
				account,
				INVALID_ARGUMENT,
				"Value to random key must be null or blank"
			),
			// Invalid UUID case
			Arguments.of(
				request.clone().setCustomerId(UUID.randomUUID().toString() + "1").build(),
				account,
				INVALID_ARGUMENT,
				"The UUID is invalid"
			),

			)
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

@Factory
internal class Clients {
	@Bean
	fun blockingStub(
		@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel?
	): ManagePixServiceGrpc.ManagePixServiceBlockingStub {
		return ManagePixServiceGrpc.newBlockingStub(channel)
	}
}
