/**
 *  Hue Lux Bulb
 *
 *  Copyright 2016 Alan Penner
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
metadata {
	definition (name: "Hue Lux Bulb", namespace: "penner42", author: "Alan Penner") {
		capability "Switch Level"
		capability "Actuator"
		capability "Switch"
		capability "Refresh"
		capability "Polling"
		capability "Sensor"
        
        command "updateStatus"
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles(scale: 2) {
        multiAttributeTile(name:"rich-control", type: "lighting", canChangeIcon: true){
            tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
              attributeState "on", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#79b821", nextState:"turningOff"
              attributeState "off", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"turningOn"
              attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#79b821", nextState:"turningOff"
              attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"turningOn"
            }
            tileAttribute ("device.level", key: "SLIDER_CONTROL") {
              attributeState "level", action:"switch level.setLevel", range:"(0..100)"
            }
        }

        standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
        }

        main(["rich-control"])
        details(["rich-control", "refresh"])
    }

}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"

}

/** 
 * capability.switch
 **/
def on() {
	log.debug("Turning on!")
    
    def commandData = parent.getCommandData(device.deviceNetworkId)
    return parent.putHubAction(commandData.ip, "/api/${commandData.username}/lights/${commandData.deviceId}/state", [on: true, bri: 254], "commandResponse")

}

def off() {
	log.debug("Turning off!")
    
    def commandData = parent.getCommandData(device.deviceNetworkId)
    return parent.putHubAction(commandData.ip, "/api/${commandData.username}/lights/${commandData.deviceId}/state", [on: false], "commandResponse")
}

/** 
 * capability.switchLevel 
 **/
def setLevel(level) {
	def lvl = parent.scaleLevel(level, true)
	log.debug "Setting level to ${lvl}."
    
    def commandData = parent.getCommandData(device.deviceNetworkId)
	return parent.putHubAction(commandData.ip, "/api/${commandData.username}/lights/${commandData.deviceId}/state", [on: true, bri: lvl], "commandResponse")
}

/** 
 * capability.polling
 **/
def poll() {
	refresh()
}

/**
 * capability.refresh
 **/
def refresh() {
	parent.doDeviceSync()
}

def commandResponse(resp) {
	def parsedEvent = parseLanMessage(resp?.description)
	if (parsedEvent.headers && parsedEvent.body) {
		def body = new groovy.json.JsonSlurper().parseText(parsedEvent.body)
        body.each { 
			if (it.success) {
            	it.success.each { k,v -> 
					def param = k.split("/")[-1]
                    updateStatus("state", param, v)
				}
			} else if (it.error != null) {
				log.debug("Error: ${it}") 
	        } else {
				log.debug("Unknown response: ${it}")        
	        }
		}
	}
}

def updateStatus(action, param, val) {
	if (action == "state") {
		switch(param) {
        	case "on":
            	def onoff
            	if (val) { onoff = "on" } else { onoff = "off" }
            	sendEvent(name: "switch", value: onoff)
                break
            case "bri":
            	sendEvent(name: "level", value: parent.scaleLevel(val))
                break
        }
    }
}