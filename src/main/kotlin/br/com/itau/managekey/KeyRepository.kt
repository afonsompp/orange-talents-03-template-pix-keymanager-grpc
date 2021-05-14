package br.com.itau.managekey

import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository

@Repository
interface KeyRepository : JpaRepository<Key, Long> {
	fun existsByKey(key: String): Boolean
}
