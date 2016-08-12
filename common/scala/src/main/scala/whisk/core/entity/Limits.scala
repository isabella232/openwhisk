/*
 * Copyright 2015-2016 IBM Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package whisk.core.entity

import scala.util.Try
import spray.json.JsValue
import spray.json.RootJsonFormat
import spray.json.deserializationError
import spray.json.DefaultJsonProtocol

/**
 * Abstract type for limits on triggers and actions. This may
 * expand to include global limits as well (for example limits
 * that require global knowledge).
 */
protected[entity] abstract class Limits {
    protected[entity] def toJson: JsValue
    override def toString = toJson.compactPrint
}

/**
 * Limits on a specific action. Includes the following properties
 * {
 *   timeout: maximum duration in msecs an action is allowed to consume in [100 msecs, 5 minutes],
 *   memory: maximum memory in megabytes an action is allowed to consume in [128 MB, 512 MB],
 *   logs: maximum logs line in megabytes an action is allowed to generate [10 MB]
 * }
 *
 * @param timeout the duration in milliseconds, assured to be non-null because it is a value
 * @param memory the memory limit in megabytes, assured to be non-null because it is a value
 * @param logs the limit for logs written by the container and stored in the activation record, assured to be non-null because it is a value
 */
protected[core] case class ActionLimits protected[core] (timeout: TimeLimit, memory: MemoryLimit, logs: LogLimit) extends Limits {
    override protected[entity] def toJson = ActionLimits.serdes.write(this)
}

/**
 * Limits on a specific trigger. None yet.
 */
protected[core] case class TriggerLimits protected[core] () extends Limits {
    override protected[entity] def toJson: JsValue = TriggerLimits.serdes.write(this)
}

protected[core] object ActionLimits
    extends ArgNormalizer[ActionLimits]
    with DefaultJsonProtocol {

    /** Creates a ActionLimits instance with default duration, memory and log limits. */
    protected[core] def apply(): ActionLimits = ActionLimits(TimeLimit(), MemoryLimit(), LogLimit())

    override protected[core] implicit val serdes = new RootJsonFormat[ActionLimits] {
        val helper = jsonFormat3(ActionLimits.apply)

        def read(value: JsValue) = {
            val obj = Try {
                value.asJsObject.convertTo[Map[String, JsValue]]
            } getOrElse deserializationError("no valid json object passed")

            val time = TimeLimit.serdes.read(obj.get("timeout") getOrElse deserializationError("'timeout' is missing"))
            val memory = MemoryLimit.serdes.read(obj.get("memory") getOrElse deserializationError("'memory' is missing"))
            val logs = obj.get("logs") map { LogLimit.serdes.read(_) } getOrElse LogLimit()

            ActionLimits(time, memory, logs)
        }

        def write(a: ActionLimits) = helper.write(a)
    }
}

protected[core] object TriggerLimits
    extends ArgNormalizer[TriggerLimits]
    with DefaultJsonProtocol {

    override protected[core] implicit val serdes = jsonFormat0(TriggerLimits.apply)
}
