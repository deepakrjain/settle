package com.settle.ledger;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for LedgerEntry.
 *
 * INTENTIONALLY extends Repository (not JpaRepository or CrudRepository) to expose
 * ONLY insert and read operations. There are no save-for-update, delete, or deleteAll
 * methods available. This enforces the append-only invariant at the API level — callers
 * literally cannot call a method that doesn't exist.
 */
public interface LedgerEntryRepository extends Repository<LedgerEntry, UUID> {

    /** Insert a new ledger entry. */
    LedgerEntry save(LedgerEntry entry);

    /** Insert multiple ledger entries. */
    List<LedgerEntry> saveAll(Iterable<LedgerEntry> entries);

    /** Find all ledger entries for a given group. */
    List<LedgerEntry> findByGroupId(UUID groupId);
}
