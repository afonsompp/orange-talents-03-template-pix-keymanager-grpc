package br.com.itau.shered.handler

import br.com.itau.shered.exception.CustomerNotFoundException
import br.com.itau.shered.exception.KeyAlreadyExistsException
import br.com.itau.shered.exception.PixException
import io.grpc.Status
import javax.inject.Singleton

@Singleton
class PixExceptionHandler : ExceptionHandler<PixException> {
	override fun supports(e: RuntimeException): Boolean {
		return e is PixException
	}

	override fun handle(e: PixException): Status {
		return when (e) {
			is CustomerNotFoundException -> Status.NOT_FOUND.withDescription(e.message)
			is KeyAlreadyExistsException -> Status.ALREADY_EXISTS.withDescription(e.message)
			else -> Status.UNKNOWN.withDescription("UNKNOWN EXCEPTION")
		}
	}
}
