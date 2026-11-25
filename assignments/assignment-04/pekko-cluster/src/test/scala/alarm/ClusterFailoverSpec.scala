package alarm

import alarm.HomeAlarmProtocol.*
import alarm.SensorType.Door
import alarm.Zone.*
import alarm.actors.SirenActor
import com.typesafe.config.{ Config, ConfigFactory }
import org.apache.pekko.actor.testkit.typed.scaladsl.ActorTestKit
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.receptionist.Receptionist
import org.apache.pekko.cluster.MemberStatus
import org.apache.pekko.cluster.typed.{ Cluster, ClusterSingleton, ClusterSingletonSettings, Join, SingletonActor }
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{ Millis, Seconds, Span }
import org.scalatest.wordspec.AnyWordSpec

import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration.*

/** Failover of the control unit on a real cluster: three actor systems in one JVM
 *  (two "control" nodes and one "peripherals" node), talking through Artery on loopback.
 *  The control unit is a Cluster Singleton: when the node that hosts it stops, it is recreated on the
 *  other control node with no state, so it must start in RECOVERING.
 */
class ClusterFailoverSpec extends AnyWordSpec, Matchers, Eventually, BeforeAndAfterAll:

  private val cfg = AlarmConfig(
    correctPin = "1234",
    exitDelay  = 200.millis,
    entryDelay = 500.millis,
    knownZones = Set(Perimeter, LivingArea),
  )

  /** Same settings as application.conf, but with random ports (0) and the join done by hand. */
  private def nodeConfig(role: String): Config =
    ConfigFactory.parseString(
      s"""
         |pekko.actor.provider = cluster
         |pekko.actor.allow-java-serialization = off
         |pekko.actor.serialization-bindings {
         |  "alarm.CborSerializable" = jackson-cbor
         |}
         |pekko.remote.artery.canonical.hostname = "127.0.0.1"
         |pekko.remote.artery.canonical.port = 0
         |pekko.cluster.roles = ["$role"]
         |pekko.cluster.jmx.multi-mbeans-in-same-jvm = on
         |""".stripMargin
    )

  private val control1    = ActorTestKit("ClusterSystem", nodeConfig("control"))
  private val control2    = ActorTestKit("ClusterSystem", nodeConfig("control"))
  private val peripherals = ActorTestKit("ClusterSystem", nodeConfig("peripherals"))

  override def afterAll(): Unit =
    Seq(control1, control2, peripherals).foreach(_.shutdownTestKit())

  private def eventuallyWithin[T](seconds: Int)(block: => T): T =
    eventually(timeout(Span(seconds, Seconds)), interval(Span(300, Millis)))(block)

  /** Every node asks for the singleton: the oldest "control" node hosts it, the others get a proxy. */
  private def singleton(kit: ActorTestKit): ActorRef[AlarmCommand] =
    val system = kit.system
    ClusterSingleton(system).init(
      SingletonActor(AlarmControlUnit.clustered(cfg), "alarm-control-unit")
        .withSettings(ClusterSingletonSettings(system).withRole("control"))
    )

  private def upMembers: Int =
    Cluster(control1.system).state.members.count(_.status == MemberStatus.Up)

  "The control unit" should:

    "restart in RECOVERING on another node when the node that hosts it stops" in:
      // 1. form the cluster: control1 starts it, the others join it
      val first = Cluster(control1.system)
      first.manager ! Join(first.selfMember.address)
      eventuallyWithin(30) { upMembers shouldBe 1 }
      Seq(control2, peripherals).foreach(kit => Cluster(kit.system).manager ! Join(first.selfMember.address))
      eventuallyWithin(30) { upMembers shouldBe 3 }

      // 2. the singleton on the control nodes, a proxy on the peripherals node
      singleton(control1)
      singleton(control2)
      val acu = singleton(peripherals)

      // 3. the siren is on the peripherals node and registers itself: the unit finds it with the receptionist
      val sirenProbe = peripherals.createTestProbe[SirenCommand]()
      peripherals.system.receptionist ! Receptionist.Register(SirenActor.Key, sirenProbe.ref)

      def statusOf(): Status =
        val probe = peripherals.createTestProbe[Status]()
        acu ! GetStatus(probe.ref)
        probe.receiveMessage(3.seconds)

      // 4. a new unit is in RECOVERING; a correct PIN disarms it, a second one arms it
      eventuallyWithin(30) { statusOf().state shouldBe "RECOVERING" }
      acu ! PinEntered("1234")
      sirenProbe.expectMessage(10.seconds, DeactivateSiren)
      acu ! PinEntered("1234")
      eventuallyWithin(10) { statusOf().state shouldBe "ARMED" }

      // 5. stop the node that hosts the singleton (a graceful leave)
      val hostBefore = statusOf().node
      val hostKit = Seq(control1, control2).find(_.system.address.toString == hostBefore)
        .getOrElse(fail(s"the unit is hosted on $hostBefore, which is not a control node"))
      hostKit.system.terminate()
      Await.ready(hostKit.system.whenTerminated, 30.seconds)

      // 6. the unit is recreated on the other control node and has lost its state
      eventuallyWithin(60) {
        val status = statusOf()
        status.state shouldBe "RECOVERING"
        (status.node == hostBefore) shouldBe false
      }

      // 7. in RECOVERING a sensor event is ignored, a wrong PIN too
      acu ! SensorTriggered(UUID.randomUUID(), Door, Perimeter)
      sirenProbe.expectNoMessage(cfg.entryDelay + 500.millis)
      statusOf().state shouldBe "RECOVERING"
      acu ! PinEntered("0000")
      statusOf().state shouldBe "RECOVERING"

      // 8. only a correct PIN brings it back to DISARMED (and switches the siren off)
      acu ! PinEntered("1234")
      sirenProbe.expectMessage(15.seconds, DeactivateSiren)
      statusOf().state shouldBe "DISARMED"
