package br.com.itau.managekey

import br.com.zup.manage.pix.AccountType

enum class BcbAccountType(val grpcAccountType: AccountType) {

	CACC(AccountType.CONTA_CORRENTE),
	SVGS(AccountType.CONTA_POUPANCA)
}
