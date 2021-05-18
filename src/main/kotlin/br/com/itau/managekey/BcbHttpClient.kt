package br.com.itau.managekey

import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType.APPLICATION_XML
import io.micronaut.http.annotation.*
import io.micronaut.http.client.annotation.Client

@Client("http://localhost:8082/api/v1")
interface BcbHttpClient {

	@Post(value = "/pix/keys", produces = [APPLICATION_XML], consumes = [APPLICATION_XML])
	fun registerKey(@Body request: BcbCreatePixRequest): HttpResponse<BcbPixResponse>

	@Delete(value = "/pix/keys/{key}", produces = [APPLICATION_XML], consumes = [APPLICATION_XML])
	fun deleteKey(@PathVariable key: String, @Body request: BcbDeleteKeyRequest):
			HttpResponse<BcbDeleteKeyResponse>

	@Get(value = "/pix/keys/{key}", produces = [APPLICATION_XML], consumes = [APPLICATION_XML])
	fun findKey(@PathVariable key: String): HttpResponse<BcbPixResponse>
}
