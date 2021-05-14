package br.com.itau.managekey

import br.com.zup.manage.pix.AccountType
import br.com.zup.manage.pix.KeyType
import io.micronaut.core.annotation.Introspected
import java.util.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@Introspected
class KeyRequest(
        @field:Size(max = 77) val key: String,
        @field:NotBlank val customerId: String,
        @field:NotNull val keyType: KeyType?,
        @field:NotNull val accountType: AccountType?,
) 
