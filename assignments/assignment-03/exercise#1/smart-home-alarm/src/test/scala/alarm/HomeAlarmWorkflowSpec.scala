package alarm

import alarm.HomeAlarmProtocol.AlarmCommand
import alarm.HomeAlarmProtocol.AlarmCommand.{ PinEntered, SensorTriggered }
import alarm.HomeAlarmProtocol.SirenCommand
import alarm.HomeAlarmProtocol.SirenCommand.{ ActivateSiren, DeactivateSiren }
import alarm.ArmingMode.{ FullArm, PartialArm }
import alarm.SensorType.{ Door, Motion }
import alarm.Zone.*
import alarm.actors.{ KeypadActor, SensorActor }
import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID
import scala.concurrent.duration.*

class HomeAlarmWorkflowSpec extends ScalaTestWithActorTestKit, AnyWordSpecLike, Matchers:

  private val cfg = AlarmConfig(
    correctPin = "1234",
    exitDelay  = 200.millis,
    entryDelay = 200.millis,
    knownZones = Set(Perimeter, LivingArea),
  )

  /** A zone the system does not know about (not in `cfg.knownZones`). */
  private val Garage = Zone("garage")

  private def triggered(zone: Zone, sensorType: SensorType = Motion) =
    SensorTriggered(UUID.randomUUID(), sensorType, zone)

  private def freshAcu(config: AlarmConfig = cfg) =
    val sirenProbe = createTestProbe[SirenCommand]()
    val acu        = spawn(AlarmControlUnit(sirenProbe.ref, config))
    (acu, sirenProbe)

  /** Creates an ACU and brings it to the ARMED state (waiting for the exit delay). */
  private def armAcu(config: AlarmConfig = cfg, mode: ArmingMode = FullArm) =
    val (acu, sirenProbe) = freshAcu(config)
    acu ! PinEntered(config.correctPin, mode)
    Thread.sleep(config.exitDelay.toMillis + 100) // let the exit delay expire → Armed
    (acu, sirenProbe)

  "AlarmControlUnit" should:
    "not activate the siren if not armed" in:
      val (acu, sirenProbe) = freshAcu()
      acu ! triggered(Perimeter)
      sirenProbe.expectNoMessage(400.millis)

    "ignore wrong PIN while disarmed" in:
      val (acu, sirenProbe) = freshAcu()
      acu ! PinEntered("0000")
      Thread.sleep(cfg.exitDelay.toMillis + 100)   // a wrong PIN must not have started arming
      acu ! triggered(Perimeter)                   // so this sensor must still be ignored
      sirenProbe.expectNoMessage(cfg.entryDelay + 300.millis)

    "not activate the siren during exit delay even if a sensor fires" in:
      val (acu, sirenProbe) = freshAcu()
      acu ! PinEntered("1234")
      // → exit delay
      acu ! triggered(Perimeter)
      sirenProbe.expectNoMessage(400.millis)

    "ignore PIN input during exit delay" in:
      val (acu, sirenProbe) = freshAcu()
      acu ! PinEntered("1234")
      // → exit delay
      acu ! PinEntered("1234")                     // ignored: must not disarm nor restart anything
      Thread.sleep(cfg.exitDelay.toMillis + 100)
      acu ! triggered(Perimeter, Door)
      sirenProbe.expectMessage(cfg.entryDelay + 400.millis, ActivateSiren)

    "activate the siren after arming, a sensor trigger and the expiry of the entry delay" in:
      val (acu, sirenProbe) = armAcu()
      acu ! triggered(Perimeter)
      sirenProbe.expectMessage(cfg.entryDelay + 400.millis, ActivateSiren)

    "not activate the siren if the correct PIN is entered before the entry delay expires" in:
      val (acu, sirenProbe) = armAcu()
      acu ! triggered(Perimeter)
      // → entry delay
      acu ! PinEntered("1234")                     // disarm in time
      sirenProbe.expectNoMessage(cfg.entryDelay + 300.millis)

    "disarm directly when the correct PIN is entered while armed" in:
      val (acu, sirenProbe) = armAcu()
      acu ! PinEntered("1234")                     // direct disarm
      acu ! triggered(Perimeter)
      sirenProbe.expectNoMessage(cfg.entryDelay + 300.millis)

    "not disarm on wrong PIN while armed" in:
      val (acu, sirenProbe) = armAcu()
      acu ! PinEntered("0000")                     // stays armed
      acu ! triggered(Perimeter)
      sirenProbe.expectMessage(cfg.entryDelay + 400.millis, ActivateSiren)

    "not restart the entry delay when a second sensor fires during the countdown" in:
      val slowCfg = cfg.copy(entryDelay = 1.second)
      val (acu, sirenProbe) = armAcu(slowCfg)
      acu ! triggered(Perimeter)                   // countdown starts at t = 0
      Thread.sleep(300)
      acu ! triggered(Perimeter, Door)             // must be ignored, not restart the countdown
      // due at t = 1000 ms (about 700 ms from now); a restarted countdown would end at t = 1300 ms
      sirenProbe.expectMessage(850.millis, ActivateSiren)
      sirenProbe.expectNoMessage(300.millis)       // and the siren fires only once

    "keep counting down and alarm if a wrong PIN is entered during the entry delay" in:
      val (acu, sirenProbe) = armAcu()
      acu ! triggered(Perimeter)
      Thread.sleep(50)
      acu ! PinEntered("0000")                     // wrong — the countdown continues
      sirenProbe.expectMessage(cfg.entryDelay + 400.millis, ActivateSiren)

    "deactivate the siren and return to disarmed on correct PIN in alarm state" in:
      val (acu, sirenProbe) = armAcu()
      acu ! triggered(Perimeter)
      sirenProbe.expectMessage(cfg.entryDelay + 400.millis, ActivateSiren)
      acu ! PinEntered("1234")
      sirenProbe.expectMessage(500.millis, DeactivateSiren)
      acu ! triggered(Perimeter, Door)             // back to disarmed: ignored
      sirenProbe.expectNoMessage(cfg.entryDelay + 300.millis)

    "keep the siren active on wrong PIN in alarm state" in:
      val (acu, sirenProbe) = armAcu()
      acu ! triggered(Perimeter)
      sirenProbe.expectMessage(cfg.entryDelay + 400.millis, ActivateSiren)
      acu ! PinEntered("0000")                     // wrong — no DeactivateSiren
      sirenProbe.expectNoMessage(300.millis)

    "ignore sensor triggers while in alarm state" in:
      val (acu, sirenProbe) = armAcu()
      acu ! triggered(Perimeter)
      sirenProbe.expectMessage(cfg.entryDelay + 400.millis, ActivateSiren)
      acu ! triggered(Perimeter, Door)             // ignored in alarm
      sirenProbe.expectNoMessage(300.millis)       // no second ActivateSiren

  "AlarmControlUnit with zones" should:

    "monitor every known zone with FullArm" in:
      val (acu, sirenProbe) = armAcu(mode = FullArm)
      acu ! triggered(LivingArea)
      sirenProbe.expectMessage(cfg.entryDelay + 400.millis, ActivateSiren)

    "alarm on a sensor of an active zone with PartialArm" in:
      val (acu, sirenProbe) = armAcu(mode = PartialArm(Set(LivingArea)))
      acu ! triggered(LivingArea)
      sirenProbe.expectMessage(cfg.entryDelay + 400.millis, ActivateSiren)

    "ignore a sensor of a known but inactive zone with PartialArm" in:
      val (acu, sirenProbe) = armAcu(mode = PartialArm(Set(Perimeter)))
      acu ! triggered(LivingArea)                  // known zone, but not armed
      sirenProbe.expectNoMessage(cfg.entryDelay + 300.millis)

    "ignore a sensor of an unknown zone" in:
      val (acu, sirenProbe) = armAcu(mode = PartialArm(Set(Perimeter)))
      acu ! triggered(Garage)                      // Garage is not in knownZones: never active
      sirenProbe.expectNoMessage(cfg.entryDelay + 300.millis)

    "ignore a sensor of an unknown zone even with FullArm" in:
      val (acu, sirenProbe) = armAcu(mode = FullArm)
      acu ! triggered(Garage)
      sirenProbe.expectNoMessage(cfg.entryDelay + 300.millis)

    "refuse to arm when no known zone is selected" in:
      val (acu, sirenProbe) = freshAcu()
      acu ! PinEntered("1234", PartialArm(Set(Garage)))  // refused: stays disarmed
      Thread.sleep(100)
      acu ! PinEntered("1234")                           // a normal arming is still possible
      Thread.sleep(cfg.exitDelay.toMillis + 100)
      acu ! triggered(Perimeter)
      sirenProbe.expectMessage(cfg.entryDelay + 400.millis, ActivateSiren)

  "SensorActor" should:
    "report a detection with its own id, type and zone" in:
      val acuProbe = createTestProbe[AlarmCommand]()
      val id       = UUID.randomUUID()
      val sensor   = spawn(SensorActor(id, Door, Perimeter, acuProbe.ref))
      sensor ! SensorActor.Trigger
      acuProbe.expectMessage(SensorTriggered(id, Door, Perimeter))

  "KeypadActor" should:
    "forward the PIN with the requested arming mode" in:
      val acuProbe = createTestProbe[AlarmCommand]()
      val keypad   = spawn(KeypadActor(acuProbe.ref))
      keypad ! KeypadActor.Submit("1234", PartialArm(Set(Perimeter)))
      acuProbe.expectMessage(PinEntered("1234", PartialArm(Set(Perimeter))))

    "default to full arming" in:
      val acuProbe = createTestProbe[AlarmCommand]()
      val keypad   = spawn(KeypadActor(acuProbe.ref))
      keypad ! KeypadActor.Submit("1234")
      acuProbe.expectMessage(PinEntered("1234", FullArm))

  "The whole system" should:
    "raise and stop the alarm through the real keypad and sensor actors" in:
      val sirenProbe = createTestProbe[SirenCommand]()
      val acu        = spawn(AlarmControlUnit(sirenProbe.ref, cfg))
      val keypad     = spawn(KeypadActor(acu))
      val door       = spawn(SensorActor(UUID.randomUUID(), Door, Perimeter, acu))

      keypad ! KeypadActor.Submit("1234", PartialArm(Set(Perimeter)))  // arm via the keypad
      Thread.sleep(cfg.exitDelay.toMillis + 100)
      door ! SensorActor.Trigger                                         // intrusion via the sensor
      sirenProbe.expectMessage(cfg.entryDelay + 400.millis, ActivateSiren)
      keypad ! KeypadActor.Submit("1234")                                // stop it via the keypad
      sirenProbe.expectMessage(500.millis, DeactivateSiren)
