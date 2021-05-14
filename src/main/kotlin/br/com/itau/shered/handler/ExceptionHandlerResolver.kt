package br.com.itau.shered.handler

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExceptionHandlerResolver(@Inject private val handlers: Collection<ExceptionHandler<RuntimeException>>) {
	fun resolve(e: RuntimeException): ExceptionHandler<RuntimeException>? {
		val result = handlers.filter { it.supports(e) }
		if (result.size > 1) throw IllegalStateException(
			"exists more than one handler to exception ${e.javaClass.name}: $result"
		)
		return result.firstOrNull()
	}
}
