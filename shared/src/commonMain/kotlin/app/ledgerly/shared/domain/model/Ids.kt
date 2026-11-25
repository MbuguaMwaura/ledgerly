package app.ledgerly.shared.domain.model

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Ids are random UUID strings rather than database row numbers, so records made on two devices
 * can be merged when a remote source is added later.
 */
@OptIn(ExperimentalUuidApi::class)
internal fun newId(): String = Uuid.random().toString()
