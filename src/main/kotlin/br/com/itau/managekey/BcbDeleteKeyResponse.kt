package br.com.itau.managekey

import java.time.LocalDateTime

class BcbDeleteKeyResponse(val key: String, val participant: String, deletedAt: LocalDateTime)
