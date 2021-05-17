package br.com.itau.managekey

import br.com.zup.manage.pix.*
import io.grpc.ManagedChannel
import io.grpc.Status
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
		.setType(KeyType.CPF)
		.setAccountType(AccountType.CONTA_CORRENTE)

	private val account = AccountResponse(
		"CONTA_CORRENTE",
		InstitutionResponse("a", "a"),
		"1",
		"1",
		OwnerResponse("a", "a", "a")
	)

	@AfterEach
	fun after() = repository.deleteAll()

	@Test
	fun `Should save key in database and return a valid response`() {
		val bcbOwner = BcbOwnerResponse("NATURAL_PERSON", "Afonso", "123")
		val bcbBankAcc = BcbBankAccountResponse("1", "1", "1", "1")
		val keyHttpResponse = HttpResponse.ok(
			BcbCreatePixResponse(
				"EMAIL", "abc@def.com", bcbBankAcc, bcbOwner, LocalDateTime.now()
			)
		)

		inCase(client.getAccount(anyString(), anyString())).thenReturn(HttpResponse.ok(account))
		inCase(
			bcb.registerKey(
				BcbCreatePixRequest.of(
					Key(
						"02654220273",
						KeyType.CPF,
						account.toAccount()
					)
				)
			)
		).thenReturn(keyHttpResponse)

		grpc.registerKey(request.build())

		val error = assertThrows<StatusRuntimeException> { grpc.registerKey(request.build()) }

		assertEquals(Status.ALREADY_EXISTS.code, error.status.code)
		assertEquals("Pix key already exists", error.status.description)
	}

	@Test
	fun `Should throw error when key already exists in database`() {
		val bcbOwner = BcbOwnerResponse("NATURAL_PERSON", "Afonso", "123")
		val bcbBankAcc = BcbBankAccountResponse("1", "1", "1", "1")
		val keyHttpResponse = HttpResponse.ok(
			BcbCreatePixResponse(
				"EMAIL", "abc@def.com", bcbBankAcc, bcbOwner, LocalDateTime.now()
			)
		)
		inCase(client.getAccount(anyString(), anyString()))
			.thenReturn(HttpResponse.ok(account))
		inCase(
			bcb.registerKey(
				BcbCreatePixRequest.of(
					Key(
						"02654220273",
						KeyType.CPF,
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
		val institution = InstitutionResponse("a", "a")
		val owner = OwnerResponse(UUID.randomUUID().toString(), "a", "a")
		val account = AccountResponse("CONTA_CORRENTE", institution, "1", "1", owner)

		val key = Key("abc@def.com", KeyType.EMAIL, account.toAccount())
		repository.save(key)

		val requestRemove = RemoveKeyRequest.newBuilder()
			.setKeyId(key.id!!)
			.setCustomerId(owner.id)
			.build()
		val response = grpc.removeKey(requestRemove)
		assertEquals("Success", response.message)
	}

	fun provideValues(): Stream<Arguments> {

		return Stream.of(
			//customer not found case
			Arguments.of(
				request.build(),
				null,
				Status.NOT_FOUND,
				"Client not found"
			),
			// Invalid Key type case
			Arguments.of(
				request.clone().setType(KeyType.UNKNOWN_TYPE).build(),
				account,
				Status.INVALID_ARGUMENT,
				"Key type cannot be UNKNOWN_TYPE"
			),
			// Invalid account type case
			Arguments.of(
				request.clone().setAccountType(AccountType.UNKNOWN_ACCOUNT).build(),
				account,
				Status.INVALID_ARGUMENT,
				"The account type cannot be UNKNOWN_TYPE"
			),
			// Invalid CPF case
			Arguments.of(
				request.clone().setType(KeyType.CPF).setValue("02654220173").build(),
				account,
				Status.INVALID_ARGUMENT,
				"The CPF is invalid"
			),
			// Invalid Phone number case
			Arguments.of(
				request.clone().setType(KeyType.PHONE).setValue("5569993551645").build(),
				account,
				Status.INVALID_ARGUMENT,
				"The Phone number is invalid"
			),
			//Invalid email case
			Arguments.of(
				request.clone().setType(KeyType.EMAIL).setValue("abc@s.").build(),
				account,
				Status.INVALID_ARGUMENT,
				"The Email is invalid"
			),
			// Random key case
			Arguments.of(
				request.clone().setType(KeyType.RANDOM).setValue(".").build(),
				account,
				Status.INVALID_ARGUMENT,
				"Value to random key must be null or blank"
			),
			//
			Arguments.of(
				request.clone().setCustomerId(UUID.randomUUID().toString() + "1").build(),
				account,
				Status.INVALID_ARGUMENT,
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
