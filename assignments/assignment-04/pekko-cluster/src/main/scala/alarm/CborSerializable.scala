package alarm

/** Marker trait: every message that can travel between cluster nodes extends it.
 *
 * `application.conf` binds this trait to the Jackson CBOR serializer
 * (binding the trait, not each message class, as suggested in the seminar).
 */
trait CborSerializable
