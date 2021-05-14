package br.com.itau.managekey

import br.com.zup.manage.pix.AccountType
import br.com.zup.manage.pix.KeyType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class KeyRequestTest {

	@ParameterizedTest
	@MethodSource("provideValues")
	fun `Should return a random UUID if key type is random`(
		value: String, type: KeyType, result: Boolean, length: Int
	) {

		val institution = InstitutionResponse("", "")
		val owner = OwnerResponse("", "", "")
		val account = AccountResponse("CONTA_CORRENTE", institution, "", "", owner)
		val key = KeyRequest(value, "", type, AccountType.CONTA_CORRENTE).toKey(account)


		assertEquals(result, value == (key.key))
		assertEquals(length, key.key.length)
	}

	private fun provideValues(): Stream<Arguments> = Stream.of(
		Arguments.of("", KeyType.RANDOM, false, 36),
		Arguments.of("value", KeyType.CPF, true, "value".length)
	)
}
