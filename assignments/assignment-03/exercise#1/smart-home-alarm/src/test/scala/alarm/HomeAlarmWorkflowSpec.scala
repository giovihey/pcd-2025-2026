package alarm

import alarm.HomeAlarmProtocol.AlarmCommand.{PinEntered, SensorTriggered}
import alarm.HomeAlarmProtocol.SirenCommand
import alarm.HomeAlarmProtocol.SirenCommand.ActivateSiren
import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID
import scala.language.postfixOps

class HomeAlarmWorkflowSpec extends  ScalaTestWithActorTestKit, AnyWordSpecLike, Eventually, Matchers:
  private val cfg = AlarmConfig(
    correctPin = "1234",
    exitDelay = 200.millis,
    entryDelay = 200.millis,
  )

  "AlarmControlUnit" should:
    "activate the siren after arming, triggering a sensor, and letting the entry delay expire" in:
      val sirenProbe = createTestProbe[SirenCommand]()
      val acu = spawn(AlarmControlUnit(sirenProbe.ref, cfg))

      acu ! PinEntered("1234")

      eventually(timeout(Span(2, Seconds)), interval(Span(50, Millis))):
        acu ! SensorTriggered(UUID.randomUUID(), "motion")
        sirenProbe.expectMessage(500.millis, ActivateSiren)

    "not activate the siren if not armed" in:
      val sirenProbe = createTestProbe[SirenCommand]()
      val acu = spawn(AlarmControlUnit(sirenProbe.ref, cfg))

      acu ! SensorTriggered(UUID.randomUUID(), "motion")

      sirenProbe.expectNoMessage(500.millis)

    "not activate the siren in the delays time" in:
      val sirenProbe = createTestProbe[SirenCommand]()
      val acu = spawn(AlarmControlUnit(sirenProbe.ref, cfg))

      acu ! PinEntered("1234")           // starts exit delay
      acu ! SensorTriggered(UUID.randomUUID(), "motion")  // still in exit delay, ignored

      sirenProbe.expectNoMessage(500.millis)

    "not activate the siren if armed but pin is entered during entry delay" in:
      val sirenProbe = createTestProbe[SirenCommand]()
      val acu = spawn(AlarmControlUnit(sirenProbe.ref, cfg))

      acu ! PinEntered("1234")           // starts exit delay

      eventually(timeout(Span(2, Seconds)), interval(Span(50, Millis))):
        acu ! SensorTriggered(UUID.randomUUID(), "motion")  // triggers entry delay once armed
        acu ! PinEntered("1234")         // disarm before entry delay expires

      sirenProbe.expectNoMessage(500.millis)

