package br.com.itau.managekey

import br.com.itau.shered.extension.toModel
import br.com.zup.manage.pix.ManagePixServiceGrpc
import br.com.zup.manage.pix.RegisterKeyRequest
import br.com.zup.manage.pix.RegisterKeyResponse
import io.grpc.stub.StreamObserver
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class PixManagerEndpoint(
	@Inject val service: RegisterKeyService
) : ManagePixServiceGrpc.ManagePixServiceImplBase() {

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
}
