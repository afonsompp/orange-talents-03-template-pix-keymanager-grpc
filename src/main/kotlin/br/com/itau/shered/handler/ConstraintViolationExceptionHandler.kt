package br.com.itau.shered.handler

import io.grpc.Status
import javax.inject.Singleton
import javax.validation.ConstraintViolationException

@Singleton
class ConstraintViolationExceptionHandler : ExceptionHandler<ConstraintViolationException> {

	override fun handle(e: ConstraintViolationException): Status {
		return Status.INVALID_ARGUMENT.withDescription(e.message!!.substringAfter(": "))
	}

	override fun supports(e: RuntimeException): Boolean {
		return e is ConstraintViolationException
	}
}
