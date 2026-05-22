package alarm

import org.apache.pekko.actor.typed.{ ActorRef, ActorSystem }
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import alarm.HomeAlarmProtocol.*
import alarm.actors.SensorActor.Trigger
import alarm.actors.KeypadActor.Submit
import alarm.actors.{ KeypadActor, SensorActor, SirenActor }

import java.util.UUID
import scala.concurrent.{ Await, Promise }
import scala.concurrent.duration.*

object Main:

  private case class Refs(
                           keypad:       ActorRef[Submit],
                           frontDoor:    ActorRef[Trigger.type],   // zone: Perimeter
                           upperMotion:  ActorRef[Trigger.type],   // zone: UpperFloor
                         )

  @main def run(): Unit =

    val config = AlarmConfig(
      correctPin = "1234",
      exitDelay  = 3.seconds,
      entryDelay = 3.seconds,
      knownZones = Zone.All,
    )

    val refsPromise = Promise[Refs]()

    val system = ActorSystem(
      Behaviors.setup[Nothing]: ctx =>
        val siren        = ctx.spawn(SirenActor(),                                             "siren")
        val acu          = ctx.spawn(AlarmControlUnit(siren, config),                          "alarm-control-unit")
        val keypad       = ctx.spawn(KeypadActor(acu),                                         "keypad")
        val frontDoor    = ctx.spawn(SensorActor(UUID.randomUUID(), "door",   Zone.Perimeter,  acu), "front-door")
        val upperMotion  = ctx.spawn(SensorActor(UUID.randomUUID(), "motion", Zone.UpperFloor, acu), "upper-motion")
        refsPromise.success(Refs(keypad, frontDoor, upperMotion))
        Behaviors.empty
      , "SmartHomeAlarm"
    )

    val refs = Await.result(refsPromise.future, 5.seconds)

    // ── Scenario 1: Full arm — all zones monitored ────────────────────────────

    println("\n=== [DEMO 1] Full arm with correct PIN ===")
    refs.keypad ! Submit("1234", ArmingMode.FullArm)    // all zones → EXIT DELAY

    Thread.sleep(4_000)                                 // wait for ARMED state

    println("\n=== [DEMO 1] Trigger upper-floor sensor — should alarm ===")
    refs.upperMotion ! Trigger                          // zone active → ENTRY DELAY

    Thread.sleep(4_500)                                 // let entry delay expire → ALARM

    println("\n=== [DEMO 1] Correct PIN to stop alarm ===")
    refs.keypad ! Submit("1234")                        // → DISARMED

    Thread.sleep(2_000)

    // ── Scenario 2: Night / partial arm — only Perimeter armed ───────────────

    println("\n=== [DEMO 2] Night mode: arm Perimeter only ===")
    refs.keypad ! Submit("1234", ArmingMode.PartialArm(Set(Zone.Perimeter)))

    Thread.sleep(4_000)                                 // wait for ARMED state

    println("\n=== [DEMO 2] Trigger upper-floor sensor — should be IGNORED ===")
    refs.upperMotion ! Trigger                          // UpperFloor inactive → silently ignored

    Thread.sleep(1_000)

    println("\n=== [DEMO 2] Trigger front-door sensor — should alarm ===")
    refs.frontDoor ! Trigger                            // Perimeter active → ENTRY DELAY

    Thread.sleep(1_000)

    println("\n=== [DEMO 2] Correct PIN during entry delay — disarmed in time ===")
    refs.keypad ! Submit("1234")                        // → DISARMED

    Thread.sleep(2_000)
    println("\n=== [DEMO] Done ===")
    system.terminate()