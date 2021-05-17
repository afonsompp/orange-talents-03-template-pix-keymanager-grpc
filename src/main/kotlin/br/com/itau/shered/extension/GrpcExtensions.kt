package br.com.itau.shered.extension

import br.com.itau.managekey.BcbAccountType
import br.com.itau.managekey.KeyRequest
import br.com.zup.manage.pix.AccountType
import br.com.zup.manage.pix.AccountType.CONTA_CORRENTE
import br.com.zup.manage.pix.AccountType.CONTA_POUPANCA
import br.com.zup.manage.pix.RegisterKeyRequest

fun RegisterKeyRequest.toModel(): KeyRequest = KeyRequest(value, customerId, type, accountType)

fun AccountType.toBcbAccountType(): BcbAccountType = when (this) {
	CONTA_CORRENTE -> BcbAccountType.CACC
	CONTA_POUPANCA -> BcbAccountType.SVGS
	else -> throw IllegalArgumentException("Invalid account type")
}
