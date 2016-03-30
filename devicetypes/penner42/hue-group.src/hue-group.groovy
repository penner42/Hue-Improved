/**
 *  Hue Group
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
	definition (name: "Hue Group", namespace: "penner42", author: "Alan Penner") {
		capability "Switch Level"
		capability "Actuator"
		capability "Color Control"
        capability "Color Temperature"
		capability "Switch"
		capability "Polling"
		capability "Refresh"
		capability "Sensor"
        
        command "updateStatus"
        command "reset"
        
        attribute "colorTemp", "number"
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles (scale: 2){
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-multi", backgroundColor:"#79b821", nextState:"-> Off"
				attributeState "off", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-multi", backgroundColor:"#ffffff", nextState:"-> On"
				attributeState "-> On", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-multi", backgroundColor:"#79b821", nextState:"-> Off"
				attributeState "-> Off", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-multi", backgroundColor:"#ffffff", nextState:"-> On"
			}
			tileAttribute ("device.level", key: "SLIDER_CONTROL") {
				attributeState "level", action:"switch level.setLevel"
			}
			tileAttribute ("device.color", key: "COLOR_CONTROL") {
				attributeState "color", action:"setColor"
			}
		}

		standardTile("reset", "device.reset", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:"Reset Color", action:"reset", icon:"st.lights.philips.hue-multi"
		}
		standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		valueTile("valueCT", "device.colorTemp", inactiveLabel: false, decoration: "flat", width: 2, height: 1) {
			state "colorTemp", label: 'Color Temp:  ${currentValue}'
        }
        controlTile("colorTemp", "device.colorTemp", "slider", inactiveLabel: false,  width: 4, height: 1, range:"(2000..6500)") { 
        	state "setCT", action:"setColorTemperature"
		}
	}
	main(["switch"])
	details(["switch","valueCT","colorTemp","refresh","reset"])
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
}

/** 
 * capability.switchLevel 
 **/
def setLevel(level) {
	def lvl = parent.scaleLevel(level, true)
	log.debug "Setting level to ${lvl}."
    
    def commandData = parent.getCommandData(device.deviceNetworkId)
	return parent.putHubAction(commandData.ip, "/api/${commandData.username}/groups/${commandData.deviceId}/action", [on: true, bri: lvl], "commandResponse")
}

/**
 * capability.colorControl 
 **/
def setColor(value) {
	def hue = parent.scaleLevel(value.hue, true, 65535)
    def sat = parent.scaleLevel(value.saturation, true, 254)
	def bri = value.bri ?: this.device.currentValue("level")
    bri = parent.scaleLevel(bri, true, 254)

	def commandData = parent.getCommandData(device.deviceNetworkId)
    return parent.putHubAction(commandData.ip, "/api/${commandData.username}/groups/${commandData.deviceId}/action", 
    						   [on: true, bri: bri, hue: hue, sat: sat], "commandResponse")
}

def setHue(hue) {
	def sat = this.device.currentValue("sat") ?: 56
    dev level = this.device.currentValue("level") ?: 100
    setColor([bri:level, saturation:sat, hue:hue])
}

def setSaturation(sat) {
	def hue = this.device.currentValue("hue") ?: 23
    dev level = this.device.currentValue("level") ?: 100
    setColor([bri:level, saturation:sat, hue:hue])
}


/**
 * capability.colorTemperature 
 **/
def setColorTemperature(temp) {
	log.debug("Setting color temperature to ${temp}")
    def ct = Math.round(1000000/temp)
	def commandData = parent.getCommandData(device.deviceNetworkId)
    return parent.putHubAction(commandData.ip, "/api/${commandData.username}/groups/${commandData.deviceId}/action", [on: true, ct: ct], "commandResponse")
}

/** 
 * capability.switch
 **/
def on() {
	log.debug("Turning on!")
    
    def commandData = parent.getCommandData(device.deviceNetworkId)
    return parent.putHubAction(commandData.ip, "/api/${commandData.username}/groups/${commandData.deviceId}/action", [on: true, bri: 254], "commandResponse")
}

def off() {
	log.debug("Turning off!")
    
    def commandData = parent.getCommandData(device.deviceNetworkId)
    return parent.putHubAction(commandData.ip, "/api/${commandData.username}/groups/${commandData.deviceId}/action", [on: false], "commandResponse")
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
	parent.refresh()
}

def reset() {
	log.debug "Resetting color."
    def value = [bri:100, saturation:56, hue:23]
    setColor(value)
}

def commandResponse(resp) {
	def parsedEvent = parseLanMessage(resp?.description)
	if (parsedEvent.headers && parsedEvent.body) {
		def body = new groovy.json.JsonSlurper().parseText(parsedEvent.body)
        body.each { 
			if (it.success) {
            	it.success.each { k,v -> 
					def param = k.split("/")[-1]
                    updateStatus("action", param, v)
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
	log.debug "updating status: ${param}:${val}"
	if (action == "action") {
		switch(param) {
        	case "on":
            	def onoff
            	if (val) { onoff = "on" } else { onoff = "off" }
            	sendEvent(name: "switch", value: onoff)
                break
            case "bri":
            	sendEvent(name: "level", value: parent.scaleLevel(val))
                break
			case "hue":
            	sendEvent(name: "hue", value: parent.scaleLevel(val, false, 65535))
                break
            case "sat":
            	sendEvent(name: "saturation", value: parent.scaleLevel(val))
                break
			case "ct": 
            	sendEvent(name: "colorTemp", value: Math.round(1000000/val))
                break
			default: 
				log.debug("Unhandled parameter: ${param}. Value: ${val}")    
		}
    }
}