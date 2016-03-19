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
            tileAttribute ("device.level", key: "SECONDARY_CONTROL") {
	            attributeState "level", label: 'Level ${currentValue}%'
			}
        }

		standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
			state "on", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#79b821", nextState:"turningOff"
			state "off", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"turningOn"
			state "turningOn", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#79b821", nextState:"turningOff"
			state "turningOff", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"turningOn"
        }

        controlTile("levelSliderControl", "device.level", "slider", height: 1, width: 2, inactiveLabel: false, range:"(0..100)") {
            state "level", action:"switch level.setLevel"
        }
        
        controlTile("transitiontime", "device.transTime", "slider", inactiveLabel: false,  width: 5, height: 1, range:"(0..4)") { 
        	state "setTT", action:"setTT", backgroundColor:"#d04e00"
		}
		valueTile("valueTT", "device.transTime", inactiveLabel: false, decoration: "flat", width: 1, height: 1) {
			state "transTime", label: 'Transition    Time: ${currentValue}'
        }

        standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
        }

        main(["switch"])
        details(["rich-control", "transitiontime", "valueTT", "refresh"])
    }

}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"

}

def on() {
	log.debug("Turning on!")
    
    def commandData = parent.getCommandData(device.deviceNetworkId)
	parent.sendHubCommand(new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/lights/${commandData.deviceId}/state",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: [on: true, bri: 254]
		])
	)
}

def off() {
	log.debug("Turning off!")
    
    def commandData = parent.getCommandData(device.deviceNetworkId)
	parent.sendHubCommand(new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/lights/${commandData.deviceId}/state",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: [on: false]
		])
	)
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