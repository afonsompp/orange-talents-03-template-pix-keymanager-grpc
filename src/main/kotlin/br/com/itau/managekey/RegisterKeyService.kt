package br.com.itau.managekey

import br.com.itau.shered.exception.CustomerNotFoundException
import br.com.itau.shered.exception.KeyAlreadyExistsException
import br.com.itau.shered.exception.KeyNotFoundException
import br.com.itau.shered.validation.ValidUUID
import io.micronaut.validation.Validated
import javax.inject.Inject
import javax.inject.Singleton
import javax.validation.Valid
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Positive

@Singleton
@Validated
class RegisterKeyService(
	@Inject val repository: KeyRepository,
	@Inject val erpClient: SystemErpHttpClient
) {

	fun saveKey(@Valid request: KeyRequest): Key {

		if (repository.existsByKey(request.key))
			throw KeyAlreadyExistsException("Pix key already exists")

		val response = erpClient.getAccount(request.customerId, request.accountType!!.name)

		val account = response.body() ?: throw CustomerNotFoundException("Client not found")

		val key = request.toKey(account)

		return repository.save(key)
	}

	fun deleteKey(
		@NotNull @Positive keyId: Long,
		@NotBlank @ValidUUID customerId: String
	) {
		val key = repository.findByIdAndCustomerId(keyId, customerId)
			?: throw KeyNotFoundException("Key not found")

		repository.delete(key)
	}
}
