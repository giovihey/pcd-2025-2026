package alarm

import java.util.UUID

object HomeAlarmProtocol:
  enum AlarmCommand:
    case PinEntered(pin: String)
    case SensorTriggered(sensorId: UUID, sensorType: String)
    case ExitDelayExpired
    case EntryDelayExpired

  enum SirenCommand:
    case ActivateSiren
    case DeactivateSiren

  export AlarmCommand.*, SirenCommand.*
