package br.com.itau.shered.extension

import br.com.itau.managekey.KeyRequest
import br.com.zup.manage.pix.RegisterKeyRequest

fun RegisterKeyRequest.toModel(): KeyRequest = KeyRequest(value, customerId, type, accountType)
