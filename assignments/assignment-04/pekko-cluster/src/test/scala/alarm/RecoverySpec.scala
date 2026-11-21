package alarm

import alarm.ArmingMode.FullArm
import alarm.HomeAlarmProtocol.*
import alarm.SensorType.Motion
import alarm.Zone.*
import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.apache.pekko.actor.typed.ActorRef
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID
import scala.concurrent.duration.*

class RecoverySpec extends ScalaTestWithActorTestKit, AnyWordSpecLike, Matchers:

  private val cfg = AlarmConfig(
    correctPin = "1234",
    exitDelay  = 200.millis,
    entryDelay = 200.millis,
    knownZones = Set(Perimeter, LivingArea),
  )

  private def triggered(zone: Zone) =
    SensorTriggered(UUID.randomUUID(), Motion, zone)

  /** Asks the unit for its status. Messages are processed in order, so the answer
   *  reflects everything that was sent to the unit before this call. */
  private def statusOf(acu: ActorRef[AlarmCommand]): Status =
    val probe = createTestProbe[Status]()
    acu ! GetStatus(probe.ref)
    probe.receiveMessage()

  /** A unit that has just been (re)created, as it happens in the cluster. */
  private def freshRecovering(config: AlarmConfig = cfg) =
    val sirenProbe = createTestProbe[SirenCommand]()
    val acu        = spawn(AlarmControlUnit.recovering(sirenProbe.ref, config))
    (acu, sirenProbe)

  "AlarmControlUnit in recovery mode" should:

    "start in RECOVERING" in:
      val (acu, _) = freshRecovering()
      statusOf(acu).state shouldBe "RECOVERING"

    "ignore sensor events: no entry delay and no alarm" in:
      val (acu, sirenProbe) = freshRecovering()
      acu ! triggered(Perimeter)
      statusOf(acu).state shouldBe "RECOVERING"
      sirenProbe.expectNoMessage(cfg.entryDelay + 300.millis)

    "ignore a wrong PIN" in:
      val (acu, sirenProbe) = freshRecovering()
      acu ! PinEntered("0000")
      statusOf(acu).state shouldBe "RECOVERING"
      sirenProbe.expectNoMessage(300.millis)

    "go to DISARMED on the correct PIN, switching the siren off, without arming" in:
      val (acu, sirenProbe) = freshRecovering()
      acu ! PinEntered("1234", FullArm)
      sirenProbe.expectMessage(DeactivateSiren)       // the siren may have been left on
      statusOf(acu).state shouldBe "DISARMED"
      Thread.sleep(cfg.exitDelay.toMillis + 100)
      statusOf(acu).state shouldBe "DISARMED"         // the mode in that PIN was not used to arm

    "need a second PIN to arm, and then behave as usual" in:
      val (acu, sirenProbe) = freshRecovering()
      acu ! PinEntered("1234")
      sirenProbe.expectMessage(DeactivateSiren)
      acu ! PinEntered("1234")                        // second PIN: arming
      statusOf(acu).state shouldBe "EXIT_DELAY"
      Thread.sleep(cfg.exitDelay.toMillis + 100)
      statusOf(acu).state shouldBe "ARMED"
      acu ! triggered(Perimeter)
      sirenProbe.expectMessage(cfg.entryDelay + 400.millis, ActivateSiren)

  "GetStatus" should:

    "name the state of the unit and the node that hosts it" in:
      val sirenProbe = createTestProbe[SirenCommand]()
      val acu        = spawn(AlarmControlUnit(sirenProbe.ref, cfg))   // the normal entry point: DISARMED
      val status     = statusOf(acu)
      status.state shouldBe "DISARMED"
      status.node.nonEmpty shouldBe true

    "follow the whole cycle: disarmed, exit delay, armed, entry delay, alarm" in:
      val slowCfg    = cfg.copy(entryDelay = 1.second)
      val sirenProbe = createTestProbe[SirenCommand]()
      val acu        = spawn(AlarmControlUnit(sirenProbe.ref, slowCfg))

      statusOf(acu).state shouldBe "DISARMED"
      acu ! PinEntered("1234")
      statusOf(acu).state shouldBe "EXIT_DELAY"
      Thread.sleep(slowCfg.exitDelay.toMillis + 100)
      statusOf(acu).state shouldBe "ARMED"
      acu ! triggered(Perimeter)
      statusOf(acu).state shouldBe "ENTRY_DELAY"
      sirenProbe.expectMessage(slowCfg.entryDelay + 400.millis, ActivateSiren)
      statusOf(acu).state shouldBe "ALARM"
