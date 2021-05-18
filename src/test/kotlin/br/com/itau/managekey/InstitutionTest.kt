package br.com.itau.managekey

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class InstitutionTest {

	@Test
	fun `Should return bank by participant`() {
		assertEquals("ITAÚ UNIBANCO S.A.", Institution.getNameFromParticipant("60701190"))
	}

	@Test
	fun `Should throw IllegalArgumentException if participant don't exists`() {
		val error = assertThrows<IllegalArgumentException> {
			Institution.getNameFromParticipant("60701191")
		}

		assertEquals("participant not found", error.message)
	}
}
