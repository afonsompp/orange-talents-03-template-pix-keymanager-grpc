package br.com.itau.managekey

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.client.annotation.Client

@Client("http://localhost:9091//api/v1")
interface SystemErpHttpClient {

	@Get("/clientes/{clienteId}/contas")
	fun getAccount(
		@PathVariable clienteId: String,
		@QueryValue tipo: String
	): HttpResponse<AccountResponse>
}
