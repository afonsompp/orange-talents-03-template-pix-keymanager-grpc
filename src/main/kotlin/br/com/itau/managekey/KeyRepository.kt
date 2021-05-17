package br.com.itau.managekey

import io.micronaut.data.annotation.Query
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository

@Repository
interface KeyRepository : JpaRepository<Key, Long> {
	fun existsByKey(key: String): Boolean

	@Query("SELECT k FROM Key k WHERE k.id = :keyId AND k.account.owner.id = :customerId")
	fun findByIdAndCustomerId(keyId: Long, customerId: String): Key?

	fun findByKey(key: String): Key?
}
