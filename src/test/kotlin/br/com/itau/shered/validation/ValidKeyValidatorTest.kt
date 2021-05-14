package br.com.itau.shered.validation

import br.com.itau.managekey.KeyRequest
import br.com.zup.manage.pix.AccountType
import br.com.zup.manage.pix.KeyType
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.micronaut.validation.validator.Validator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.*
import java.util.stream.Stream
import javax.inject.Inject

@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ValidKeyValidatorTest {
	@Inject
	lateinit var validator: Validator

	@ParameterizedTest
	@MethodSource("provideValues")
	fun `Should validate account, key type and key value, and return message error`(
		request: KeyRequest,
		error: String?
	) {

		val errors = validator.validate(request)
		val errorsMessage = errors.stream().iterator().next().messageTemplate
		assertEquals(true, errorsMessage.equals(error, true))
	}

	@Test
	fun `Don't should return error to valid key values`() {
		val request = KeyRequest(
			"", UUID.randomUUID().toString(), KeyType.RANDOM, AccountType.CONTA_CORRENTE
		)
		assertTrue(validator.validate(request).isEmpty())
	}

	private fun provideValues(): Stream<Arguments> {
		val uuid = UUID.randomUUID().toString()
		return Stream.of(
			Arguments.of(
				KeyRequest("value", uuid, KeyType.RANDOM, AccountType.UNKNOWN_ACCOUNT),
				"The account type cannot be UNKNOWN_TYPE"
			),
			Arguments.of(
				KeyRequest("value", uuid + "1", KeyType.RANDOM, AccountType.CONTA_CORRENTE),
				"The UUID is invalid"
			),
			Arguments.of(
				KeyRequest("02654220274", uuid, KeyType.CPF, AccountType.CONTA_CORRENTE),
				"The CPF is invalid"
			),
			Arguments.of(
				KeyRequest("afonso@dew.", uuid, KeyType.EMAIL, AccountType.CONTA_CORRENTE),
				"The Email is invalid"
			),
			Arguments.of(
				KeyRequest("5569993551645", uuid, KeyType.PHONE, AccountType.CONTA_CORRENTE),
				"The Phone number is invalid"
			),
			Arguments.of(
				KeyRequest(".", uuid, KeyType.RANDOM, AccountType.CONTA_CORRENTE),
				"Value to random key must be null or blank"
			),
			Arguments.of(
				KeyRequest(".", uuid, KeyType.UNKNOWN_TYPE, AccountType.CONTA_CORRENTE),
				"Key type cannot be UNKNOWN_TYPE"
			),
		)
	}
}
