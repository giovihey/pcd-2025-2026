package alarm

import alarm.ArmingMode.{ FullArm, PartialArm }
import alarm.HomeAlarmProtocol.*
import alarm.SensorType.{ Door, Motion, Window }
import alarm.Zone.*
import com.typesafe.config.ConfigFactory
import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.apache.pekko.serialization.{ SerializationExtension, Serializers }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID

object SerializationSpec:
  /** Only the binding: the Jackson CBOR serializer itself is registered by the Pekko Jackson module.
   *  Java serialization is off, so a message that Jackson cannot handle fails instead of silently falling back.
   */
  val config = ConfigFactory.parseString(
    """
      |pekko.actor.allow-java-serialization = off
      |pekko.actor.serialization-bindings {
      |  "alarm.CborSerializable" = jackson-cbor
      |}
      |""".stripMargin
  )

/** Smoke test of the messages that will cross the nodes of the cluster:
 *  each one is serialized and deserialized exactly as Pekko does between two nodes.
 */
class SerializationSpec extends ScalaTestWithActorTestKit(SerializationSpec.config), AnyWordSpecLike, Matchers:

  private val serialization = SerializationExtension(system)

  private def roundTrip[A <: AnyRef](original: A): A =
    val serializer = serialization.findSerializerFor(original)
    val bytes      = serializer.toBinary(original)
    val manifest   = Serializers.manifestFor(serializer, original)
    serialization.deserialize(bytes, serializer.identifier, manifest).get.asInstanceOf[A]

  "Cluster messages" should:

    "be handled by Jackson" in:
      serialization.findSerializerFor(ActivateSiren).getClass.getSimpleName should include("Jackson")

    "preserve PinEntered with full arming" in:
      roundTrip(PinEntered("1234", FullArm)) shouldBe PinEntered("1234", FullArm)

    "preserve PinEntered with partial arming" in:
      val msg = PinEntered("1234", PartialArm(Set(Perimeter, LivingArea)))
      roundTrip(msg) shouldBe msg

    for sensorType <- List(Motion, Door, Window) do
      s"preserve SensorTriggered of a $sensorType sensor" in:
        val msg = SensorTriggered(UUID.randomUUID(), sensorType, Perimeter)
        roundTrip(msg) shouldBe msg

    "restore the siren commands as the very same singleton values" in:
      // SirenActor matches on ActivateSiren / DeactivateSiren: a copy would not match
      roundTrip(ActivateSiren) should be theSameInstanceAs ActivateSiren
      roundTrip(DeactivateSiren) should be theSameInstanceAs DeactivateSiren

    "preserve the Status reply of GetStatus" in:
      val msg = Status("ARMED", "pekko://ClusterSystem@127.0.0.1:2551")
      roundTrip(msg) shouldBe msg
      