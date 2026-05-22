package alarm

import alarm.HomeAlarmProtocol.AlarmCommand.{ PinEntered, SensorTriggered }
import alarm.HomeAlarmProtocol.SirenCommand
import alarm.HomeAlarmProtocol.SirenCommand.{ ActivateSiren, DeactivateSiren }
import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{ Millis, Seconds, Span }
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID
import scala.concurrent.duration.*

class HomeAlarmWorkflowSpec extends ScalaTestWithActorTestKit, AnyWordSpecLike, Eventually, Matchers:

  private val cfg = AlarmConfig(
    correctPin = "1234",
    exitDelay  = 200.millis,
    entryDelay = 200.millis,
  )

  private def freshAcu() =
    val sirenProbe = createTestProbe[SirenCommand]()
    val acu        = spawn(AlarmControlUnit(sirenProbe.ref, cfg))
    (acu, sirenProbe)

  private def armAcu() =
    val (acu, sirenProbe) = freshAcu()
    acu ! PinEntered("1234")
    Thread.sleep(cfg.exitDelay.toMillis + 100) // let exit delay expire → Armed
    (acu, sirenProbe)

  "AlarmControlUnit" should:
    "not activate the siren if not armed" in:
      val (acu, sirenProbe) = freshAcu()
      acu ! SensorTriggered(UUID.randomUUID(), "motion")
      sirenProbe.expectNoMessage(400.millis)

    "ignore wrong PIN while disarmed" in:
      val (acu, sirenProbe) = freshAcu()
      acu ! PinEntered("0000")
      acu ! SensorTriggered(UUID.randomUUID(), "motion") // would alarm if armed
      sirenProbe.expectNoMessage(400.millis)

    "not activate the siren during exit delay even if a sensor fires" in:
      val (acu, sirenProbe) = freshAcu()
      acu ! PinEntered("1234")               // → exit delay
      acu ! SensorTriggered(UUID.randomUUID(), "motion")
      sirenProbe.expectNoMessage(400.millis)

    "ignore PIN input during exit delay" in:
      val (acu, sirenProbe) = freshAcu()
      acu ! PinEntered("1234")               // → exit delay
      acu ! PinEntered("1234")               // should be ignored, not double-arm
      Thread.sleep(cfg.exitDelay.toMillis + 100)
      // confirm armed: sensor starts entry delay but siren has not fired yet
      acu ! SensorTriggered(UUID.randomUUID(), "door")
      sirenProbe.expectNoMessage(100.millis) // less than entryDelay (200ms)

    "activate the siren after arming, triggering a sensor, and letting entry delay expire" in:
      val (acu, sirenProbe) = freshAcu()
      acu ! PinEntered("1234")
      eventually(timeout(Span(2, Seconds)), interval(Span(50, Millis))):
        acu ! SensorTriggered(UUID.randomUUID(), "motion")
        sirenProbe.expectMessage(500.millis, ActivateSiren)

    "not activate the siren if correct PIN entered before entry delay expires" in:
      val (acu, sirenProbe) = freshAcu()
      acu ! PinEntered("1234")               // → exit delay
      eventually(timeout(Span(2, Seconds)), interval(Span(50, Millis))):
        acu ! SensorTriggered(UUID.randomUUID(), "motion")
        acu ! PinEntered("1234")             // disarm during entry delay
      sirenProbe.expectNoMessage(400.millis)

    "disarm directly when correct PIN is entered while armed" in:
      val (acu, sirenProbe) = armAcu()
      acu ! PinEntered("1234")               // direct disarm — no sensor needed
      // now disarmed: sensor should be ignored
      acu ! SensorTriggered(UUID.randomUUID(), "motion")
      sirenProbe.expectNoMessage(cfg.entryDelay + 300.millis)

    "not disarm on wrong PIN while armed" in:
      val (acu, sirenProbe) = armAcu()
      acu ! PinEntered("0000")               // wrong — stays armed
      acu ! SensorTriggered(UUID.randomUUID(), "motion")
      sirenProbe.expectMessage(cfg.entryDelay + 400.millis, ActivateSiren)

    "not re-trigger entry delay if a second sensor fires during entry delay" in:
      val (acu, sirenProbe) = armAcu()
      acu ! SensorTriggered(UUID.randomUUID(), "door")    // → entry delay starts
      Thread.sleep(50)
      acu ! SensorTriggered(UUID.randomUUID(), "motion")  // ignored — already counting
      // siren fires exactly once after the single entry delay
      sirenProbe.expectMessage(cfg.entryDelay + 400.millis, ActivateSiren)
      sirenProbe.expectNoMessage(300.millis)

    "keep counting down and alarm if wrong PIN entered during entry delay" in:
      val (acu, sirenProbe) = armAcu()
      acu ! SensorTriggered(UUID.randomUUID(), "motion")
      Thread.sleep(50)
      acu ! PinEntered("0000")               // wrong — countdown continues
      sirenProbe.expectMessage(cfg.entryDelay + 400.millis, ActivateSiren)

    "deactivate the siren and return to disarmed on correct PIN in alarm state" in:
      val (acu, sirenProbe) = armAcu()
      acu ! SensorTriggered(UUID.randomUUID(), "motion")
      sirenProbe.expectMessage(cfg.entryDelay + 400.millis, ActivateSiren)
      acu ! PinEntered("1234")
      sirenProbe.expectMessage(500.millis, DeactivateSiren)
      // back to disarmed: sensor should now be ignored
      acu ! SensorTriggered(UUID.randomUUID(), "door")
      sirenProbe.expectNoMessage(400.millis)

    "keep the siren active on wrong PIN in alarm state" in:
      val (acu, sirenProbe) = armAcu()
      acu ! SensorTriggered(UUID.randomUUID(), "motion")
      sirenProbe.expectMessage(cfg.entryDelay + 400.millis, ActivateSiren)
      acu ! PinEntered("0000")               // wrong — siren keeps going
      sirenProbe.expectNoMessage(300.millis)

    "ignore sensor triggers while in alarm state" in:
      val (acu, sirenProbe) = armAcu()
      acu ! SensorTriggered(UUID.randomUUID(), "motion")
      sirenProbe.expectMessage(cfg.entryDelay + 400.millis, ActivateSiren)
      acu ! SensorTriggered(UUID.randomUUID(), "door")   // ignored in alarm
      sirenProbe.expectNoMessage(300.millis)             // no second ActivateSiren
