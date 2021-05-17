package br.com.itau.managekey

import br.com.itau.shered.extension.toModel
import br.com.itau.shered.handler.ExceptionInterceptor
import br.com.zup.manage.pix.*
import io.grpc.stub.StreamObserver
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class PixManagerEndpoint(
	@Inject val service: RegisterKeyService
) : ManagePixServiceGrpc.ManagePixServiceImplBase() {

	@ExceptionInterceptor
	override fun registerKey(
		request: RegisterKeyRequest,
		responseObserver: StreamObserver<RegisterKeyResponse>
	) {
		val key = service.saveKey(request.toModel())

		responseObserver.onNext(
			RegisterKeyResponse.newBuilder()
				.setKeyId(key.id!!)
				.setKey(key.key)
				.build()
		)
		responseObserver.onCompleted()
	}

	@ExceptionInterceptor
	override fun removeKey(
		request: RemoveKeyRequest,
		responseObserver: StreamObserver<RemoveKeyResponse>
	) {
		service.deleteKey(request.keyId, request.customerId)

		responseObserver.onNext(RemoveKeyResponse.newBuilder().setMessage("Success").build())
		responseObserver.onCompleted()
	}

	@ExceptionInterceptor
	override fun findKey(
		request: KeyDetailsRequest,
		responseObserver: StreamObserver<KeyDetailsResponse>
	) {

		if (request.filterCase.number == 1) {
			responseObserver.onNext(
				service.findKeyByKeyIdAndCustomer(request.pixId.keyId, request.pixId.customerId)
			)
			responseObserver.onCompleted()
			return
		}

		responseObserver.onNext(service.findKeyByKey(request.key))
		responseObserver.onCompleted()
	}
}
