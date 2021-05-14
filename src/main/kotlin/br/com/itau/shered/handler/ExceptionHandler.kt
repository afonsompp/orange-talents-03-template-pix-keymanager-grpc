package br.com.itau.shered.handler

import io.grpc.Status

interface ExceptionHandler<E : RuntimeException> {

	fun handle(e: E): Status

	fun supports(e: RuntimeException): Boolean
}
