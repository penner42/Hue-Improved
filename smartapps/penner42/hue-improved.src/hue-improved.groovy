/**
 *  Hue and Improved
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
definition(
        name: "Hue & Improved",
        namespace: "penner42",
        author: "Alan Penner",
        description: "Hue ",
        category: "My Apps",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/hue.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/hue@2x.png",
        singleInstance: true
)

preferences {
    page(name:"Bridges", title:"Hue Bridges", content: "bridges")
    page(name:"linkButton", content: "linkButton")
    page(name:"linkBridge", content: "linkBridge")
    page(name:"manageBridge", content: "manageBridge")
	page(name:"chooseBulbs", content: "chooseBulbs")
	page(name:"chooseScenes", content: "chooseScenes")
	page(name:"chooseGroups", content: "chooseGroups")
}

def chooseBulbs(params) {
    /* with submitOnChange, params don't get sent when the page is refreshed? */
    if (params.mac) {
        state.params = params;
    } else {
        params = state.params;
    }

	def bridge = getBridge(params.mac)
	def addedBulbs = [:]
    def availableBulbs = [:]
    
    bridge.value.bulbs.each {
		def devId = "${params.mac}/BULB${it.key}"
		def name = it.value.name
        
		def d = getChildDevice(devId) 
        if (d) {
        	addedBulbs << it
        } else {
        	availableBulbs << it
        }
    }

	if (params.add) {
	    log.debug("Adding ${params.add}")
        def bulbId = params.add
		params.add = null
        def b = bridge.value.bulbs[bulbId]
		def devId = "${params.mac}/BULB${bulbId}"
        if (b.type.equalsIgnoreCase("Dimmable light")) {
			try {
	            def d = addChildDevice("penner42", "Hue Lux Bulb", devId, bridge.value.hub, ["label": b.name])
				["bri", "reachable", "on"].each { p -> 
					d.updateStatus("state", p, b.state[p])
				}
                addedBulbs[bulbId] = b
                availableBulbs.remove(bulbId)
			} catch (grails.validation.ValidationException e) {
            	log.debug "${devId} already created."
			}    
	    } else {
			try {
            	def d = addChildDevice("penner42", "Hue Bulb", devId, bridge.value.hub, ["label": b.name])
                ["bri", "sat", "reachable", "hue", "on", "ct"].each { p ->
                	d.updateStatus("state", p, b.state[p])
				}
                addedBulbs[bulbId] = b
                availableBulbs.remove(bulbId)
			} catch (grails.validation.ValidationException e) {
	            log.debug "${devId} already created."
			}
		}
	}
    
    if (params.remove) {
    	log.debug "Removing ${params.remove}"
		def devId = params.remove
        params.remove = null
		def bulbId = devId.split("BULB")[1]
		try {
        	deleteChildDevice(devId)
            addedBulbs.remove(bulbId)
            availableBulbs[bulbId] = bridge.value.bulbs[bulbId]
		} catch (physicalgraph.exception.NotFoundException e) {
        	log.debug("${devId} already deleted.")
            addedBulbs.remove(bulbId)
            availableBulbs[bulbId] = bridge.value.bulbs[bulbId]
		} catch (physicalgraph.exception.ConflictException e) {
        	log.debug("${devId} still in use.")
            errorText = "Bulb ${bridge.value.bulbs[bulbId].name} is still in use. Remove from any SmartApps or Dashboards, then try again."
        }     
    }
    
    dynamicPage(name:"chooseBulbs", title: "", install: true) {
    	section() {
        	href(name: "manageBridge", page: "manageBridge", description: "Back to Bridge", title: "", params: [mac: params.mac])
        }
    	section("Added Bulbs") {
			addedBulbs.sort{it.value.name}.each { 
				def devId = "${params.mac}/BULB${it.key}"
				def name = it.value.name
				href(name:"${devId}", page:"chooseBulbs", description:"", title:"Remove ${name}", params: [mac: params.mac, remove: devId])
			}
		}
        section("Available Bulbs") {
			availableBulbs.sort{it.value.name}.each { 
				def devId = "${params.mac}/BULB${it.key}"
				def name = it.value.name
				href(name:"${devId}", page:"chooseBulbs", description:"", title:"Add ${name}", params: [mac: params.mac, add: it.key])
			}
        }
    }
}

def chooseScenes(params) {
    /* with submitOnChange, params don't get sent when the page is refreshed? */
    if (params.mac) {
        state.params = params;
    } else {
        params = state.params;
    }

	def bridge = getBridge(params.mac)
	def addedScenes = [:]
    def availableScenes = [:]
    
    bridge.value.scenes.each {
		def devId = "${params.mac}/SCENE${it.key}"
		def name = it.value.name
        
		def d = getChildDevice(devId) 
        if (d) {
        	addedScenes << it
        } else {
        	availableScenes << it
        }
    }

	if (params.add) {
	    log.debug("Adding ${params.add}")
        def sceneId = params.add
		params.add = null
        def s = bridge.value.scenes[sceneId]
		def devId = "${params.mac}/SCENE${sceneId}"
		try { 
			def d = addChildDevice("penner42", "Hue Scene", devId, bridge.value.hub, ["label": s.name])
			addedScenes[sceneId] = s
			availableScenes.remove(sceneId)
		} catch (grails.validation.ValidationException e) {
            	log.debug "${devId} already created."
	    }
	}
    
    if (params.remove) {
    	log.debug "Removing ${params.remove}"
		def devId = params.remove
        params.remove = null
		def sceneId = devId.split("SCENE")[1]
        try {
        	deleteChildDevice(devId)
            addedScenes.remove(sceneId)
            availableScenes[sceneId] = bridge.value.scenes[sceneId]
		} catch (physicalgraph.exception.NotFoundException e) {
        	log.debug("${devId} already deleted.")
			addedScenes.remove(sceneId)
            availableScenes[sceneId] = bridge.value.scenes[sceneId]
		} catch (physicalgraph.exception.ConflictException e) {
        	log.debug("${devId} still in use.")
            errorText = "Scene ${bridge.value.scenes[sceneId].name} is still in use. Remove from any SmartApps or Dashboards, then try again."
        }
    }
    
    dynamicPage(name:"chooseScenes", title: "", install: true) {
		section() { 
			href(name: "manageBridge", page: "manageBridge", description: "", title: "Back to Bridge", params: [mac: params.mac])
        }
    	section("Added Scenes") {
			addedScenes.sort{it.value.name}.each { 
				def devId = "${params.mac}/SCENE${it.key}"
				def name = it.value.name
				href(name:"${devId}", page:"chooseScenes", description:"", title:"Remove ${name}", params: [mac: params.mac, remove: devId])
			}
		}
        section("Available Scenes") {
			availableScenes.sort{it.value.name}.each { 
				def devId = "${params.mac}/SCENE${it.key}"
				def name = it.value.name
				href(name:"${devId}", page:"chooseScenes", description:"", title:"Add ${name}", params: [mac: params.mac, add: it.key])
			}
        }
    }
}

def chooseGroups(params) {
    /* with submitOnChange, params don't get sent when the page is refreshed? */
    if (params.mac) {
        state.params = params;
    } else {
        params = state.params;
    }

    def errorText = ""

	def bridge = getBridge(params.mac)
	def addedGroups = [:]
    def availableGroups = [:]
    
    bridge.value.groups.each {
		def devId = "${params.mac}/GROUP${it.key}"
		def name = it.value.name
        
		def d = getChildDevice(devId) 
        if (d) {
        	addedGroups << it
        } else {
        	availableGroups << it
        }
    }

	if (params.add) {
	    log.debug("Adding ${params.add}")
        def groupId = params.add
		params.add = null
        def g = bridge.value.groups[groupId]
		def devId = "${params.mac}/GROUP${groupId}"
		try { 
			def d = addChildDevice("penner42", "Hue Group", devId, bridge.value.hub, ["label": g.name])
			addedGroups[groupId] = g
			availableGroups.remove(groupId)
		} catch (grails.validation.ValidationException e) {
            	log.debug "${devId} already created."
	    }
	}
    
    if (params.remove) {
    	log.debug "Removing ${params.remove}"
		def devId = params.remove
        params.remove = null
		def groupId = devId.split("GROUP")[1]
		try {
        	deleteChildDevice(devId)
            addedGroups.remove(groupId)
            availableGroups[groupId] = bridge.value.groups[groupId]
		} catch (physicalgraph.exception.NotFoundException e) {
        	log.debug("${devId} already deleted.")
            addedGroups.remove(groupId)
            availableGroups[groupId] = bridge.value.groups[groupId]
		} catch (physicalgraph.exception.ConflictException e) {
        	log.debug("${devId} still in use.")
            errorText = "Group ${bridge.value.groups[groupId].name} is still in use. Remove from any SmartApps or Dashboards, then try again."
        }
    }

    return dynamicPage(name:"chooseGroups", title: "", install: true) {
	    section(errorText) { 
			href(name: "manageBridge", page: "manageBridge", description: "", title: "Back to Bridge", params: [mac: params.mac])
        }
	    section("Added Groups") {
			addedGroups.sort{it.value.name}.each { 
				def devId = "${params.mac}/GROUP${it.key}"
				def name = it.value.name
				href(name:"${devId}", page:"chooseGroups", description:"", title:"Remove ${name}", params: [mac: params.mac, remove: devId])
			}
		}
        section("Available Groups") {
			availableGroups.sort{it.value.name}.each { 
				def devId = "${params.mac}/GROUP${it.key}"
				def name = it.value.name
				href(name:"${devId}", page:"chooseGroups", description:"", title:"Add ${name}", params: [mac: params.mac, add: it.key])
			}
        }
    }
}

def manageBridge(params) {
    /* with submitOnChange, params don't get sent when the page is refreshed? */
    if (params.mac) {
        state.params = params;
    } else {
        params = state.params;
    }

    def bridge = getBridge(params.mac)
    def ip = convertHexToIP(bridge.value.networkAddress)
    def mac = params.mac
    def bridgeDevice = getChildDevice(mac)
    def title = "${bridgeDevice.label} ${ip}"
    def refreshInterval = 2

    if (!bridgeDevice) {
        log.debug("Bridge device not found?")
        /* Error, bridge device doesn't exist? */
        return
    }

	if (params.refreshItems || (!bridge.value.itemsDiscovered && !state.inItemDiscovery)) {
    	/* don't do device sync while refreshing/discovering */
		unschedule()     
		state.inItemDiscovery = true
        state.bulbsDiscovered = false
        state.scenesDiscovered = false
        state.groupsDiscovered = false
        state.params.refreshItems = false
        params.refreshItems = false
        state.itemRefreshCount = 0
        bridgeDevice.discoverBulbs()
    }
	
	if (state.inItemDiscovery) {
	    int itemRefreshCount = !state.itemRefreshCount ? 0 : state.itemRefreshCount as int
        state.itemRefreshCount = itemRefreshCount + 1
		
        /* resend discovery if no response after 6 seconds */
		if (state.itemRefreshCount % 4 == 0) {
        	if (!state.bulbsDiscovered) {
            	bridgeDevice.discoverBulbs()
            } else if (!state.scenesDiscovered) {
            	bridgeDevice.discoverScenes()
            } else if (!state.groupsDiscovered) {
            	bridgeDevice.discoverGroups()
            }
        }
        
        if (state.bulbsDiscovered && state.scenesDiscovered && state.groupsDiscovered) {
			state.inItemDiscovery = false
            bridge.value.itemsDiscovered = true
        } else {
			def b = "Discovering Bulbs... "
            def s = "Discovering Scenes... "
            def g = "Discovering Groups... "
            def text = b
            if (state.bulbsDiscovered) { text = text + "done.\n" + s }
            if (state.scenesDiscovered) { text = text + "done.\n" + g }
			if (state.groupssDiscovered) { text = text + "done." }
            return dynamicPage(name:"manageBridge", title: "Manage bridge ${ip}", refreshInterval: refreshInterval, install: false) {
                section(text) {
				}
            }
        }
    }
    
	/* discovery complete, re-enable device sync */
	runEvery5Minutes(doDeviceSync)
    
    def bulbList = [:]
    def sceneList = [:]
    def groupList = [:]

    def numBulbs = bridge.value.bulbs.size() ?: 0
    def numScenes = bridge.value.scenes.size() ?: 0
    def numGroups = bridge.value.groups.size() ?: 0

    dynamicPage(name:"manageBridge", title: "Manage bridge ${ip}", /* refreshInterval: refreshInterval ,*/ install: true) {
        section("") {
			href(name:"Refresh items", page:"manageBridge", title:"", description: "Refresh discovered items", params: [mac: mac, refreshItems: true])
            href(name:"Choose Bulbs", page:"chooseBulbs", description:"", title: "Choose Bulbs (${numBulbs} found)", params: [mac: mac])
            href(name:"Choose Scenes", page:"chooseScenes", description:"", title: "Choose Scenes (${numScenes} found)", params: [mac: mac])
			href(name:"Choose Groups", page:"chooseGroups", description:"", title: "Choose Groups (${numGroups} found)", params: [mac: mac])
            href(name:"Back", page:"Bridges", title:"", description: "Back to main page")
		}
    }
}

def linkBridge() {
    state.params.done = true
    log.debug "linkBridge"
    dynamicPage(name:"linkBridge") {
        section() {
            getLinkedBridges() << state.params.mac
            paragraph "Linked! Please tap Done."
        }
    }
}

def linkButton(params) {
    /* if the user hit the back button, use saved parameters as the passed ones no longer good
     * also uses state.params to pass these on to the next page
     */
    if (params.mac) {
        state.params = params;
    } else {
        params = state.params;
    }

    int linkRefreshcount = !state.linkRefreshcount ? 0 : state.linkRefreshcount as int
    state.linkRefreshcount = linkRefreshcount + 1
    def refreshInterval = 3

    params.linkingBridge = true
    if (!params.linkDone) {
        if ((linkRefreshcount % 2) == 0) {
			sendHubCommand(postHubAction(params.ip+":80", "/api", [devicetype: "${app.id}-0", username: "${app.id}-0"], "processLinkResponse"))
        }
        log.debug "linkButton ${params}"
        dynamicPage(name: "linkButton", refreshInterval: refreshInterval, nextPage: "linkButton") {
            section("Hue Bridge ${params.ip}") {
                paragraph "Please press the link button on your Hue bridge."
                image "http://www.developers.meethue.com/sites/default/files/smartbridge.jpg"
            }
            section() {
                href(name:"Cancel", page:"Bridges", title: "", description: "Cancel")
            }
        }
    } else {
        /* link success! create bridge device */
        log.debug "Bridge linked!"
        log.debug("ssdp ${params.ssdpUSN}")
        def bridge = getUnlinkedBridges().find{it?.key?.contains(params.ssdpUSN)}
        log.debug("bridge ${bridge}")
        def d = addChildDevice("penner42", "Hue Bridge", bridge.value.mac, bridge.value.hub, [label: "Hue Bridge (${params.ip})"])

        d.sendEvent(name: "networkAddress", value: params.ip)
        d.sendEvent(name: "serialNumber", value: bridge.value.serialNumber)
        d.sendEvent(name: "username", value: params.username)

	    subscribe(d, "bulbDiscovery", bulbDiscoveryHandler)        
	    subscribe(d, "sceneDiscovery", sceneDiscoveryHandler)        
	    subscribe(d, "groupDiscovery", groupDiscoveryHandler)        
        
        params.linkDone = false
        params.linkingBridge = false

        bridge.value << ["bulbs" : [:], "groups" : [:], "scenes" : [:]]
        getLinkedBridges() << bridge
        log.debug "Bridge added to linked list."
        getUnlinkedBridges().remove(params.ssdpUSN)
        log.debug "Removed bridge from unlinked list."

        dynamicPage(name: "linkButton", nextPage: "Bridges") {
            section("Hue Bridge ${params.ip}") {
                paragraph "Successfully linked Hue Bridge! Please tap Next."
            }
        }
    }
}

def getLinkedBridges() {
    state.linked_bridges = state.linked_bridges ?: [:]
}

def getUnlinkedBridges() {
    state.unlinked_bridges = state.unlinked_bridges ?: [:]
}

def getVerifiedBridges() {
    getUnlinkedBridges().findAll{it?.value?.verified == true}
}

def getBridgeBySerialNumber(serialNumber) {
    def b = getUnlinkedBridges().find{it?.value?.serialNumber == serialNumber}
    if (!b) {
        return getLinkedBridges().find{it?.value?.serialNumber == serialNumber}
    } else {
        return b
    }
}

def getBridge(mac) {
    def b = getUnlinkedBridges().find{it?.value?.mac == mac}
    if (!b) {
        return getLinkedBridges().find{it?.value?.mac == mac}
    } else {
        return b
    }
}

def bridges() {
    /* Prevent "Unexpected Error has occurred" if the user hits the back button before actually finishing an install.
     * Weird SmartThings bug
     */
    if (!state.installed) {
        return dynamicPage(name:"Bridges", title: "Initial installation", install:true, uninstall:true) {
            section() {
                paragraph "For initial installation, please tap Done, then proceed to Menu -> SmartApps -> Hue & Improved."
            }
        }
    }

    /* clear temporary stuff from other pages */
    state.params = [:]
    state.inItemDiscovery = null
    state.itemDiscoveryComplete = false
    state.numDiscoveryResponses = 0
    state.creatingDevices = false

    int bridgeRefreshCount = !state.bridgeRefreshCount ? 0 : state.bridgeRefreshCount as int
    state.bridgeRefreshCount = bridgeRefreshCount + 1
    def refreshInterval = 3

    if (!state.subscribed) {
        subscribe(location, null, locationHandler, [filterEvents:false])
        state.subscribed = true
    }

    // Send bridge discovery request every 15 seconds
    if ((state.bridgeRefreshCount % 5) == 1) {
        discoverHueBridges()
        log.debug "Bridge discovery sent."
    } else {
        // if we're not sending bridge discovery, verify bridges instead
        verifyHueBridges()
    }

    dynamicPage(name:"Bridges", refreshInterval: refreshInterval, install: true, uninstall: true) {
        section("Linked Bridges") {
            getLinkedBridges().sort { it.value.name }.each {
                def ip = convertHexToIP(it.value.networkAddress)
                def mac = "${it.value.mac}"
                def title = "Hue Bridge ${ip}"
                href(name:"manageBridge ${mac}", page:"manageBridge", title: title, description: "", params: [mac: mac])
            }
        }
        section("Unlinked Bridges") {
            paragraph "Searching for Hue bridges. They will appear here when found. Please wait."
            getVerifiedBridges().sort { it.value.name }.each {
                def ip = convertHexToIP(it.value.networkAddress)
                def mac = "${it.value.mac}"
                def title = "Hue Bridge ${ip}"
                href(name:"linkBridge ${mac}", page:"linkButton", title: title, description: "", params: [mac: mac, ip: ip, ssdpUSN: it.value.ssdpUSN, refreshItems: false])
            }
        }
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    initialize()
}

def uninstalled() {
    log.debug "Uninstalling"
    state.installed = false
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    initialize()
}

def initialize() {
    log.debug "Initialize."
    unsubscribe()
    unschedule()
    state.subscribed = false
    state.unlinked_bridges = [:]
    state.bridgeRefreshCount = 0
    state.installed = true

	doDeviceSync()
	runEvery5Minutes(doDeviceSync)

	state.linked_bridges.each {
        def d = getChildDevice(it.value.mac)
	    subscribe(d, "bulbDiscovery", bulbDiscoveryHandler)        
	    subscribe(d, "sceneDiscovery", sceneDiscoveryHandler)        
	    subscribe(d, "groupDiscovery", groupDiscoveryHandler)        
    }
    subscribe(location, null, locationHandler, [filterEvents:false])
}

def bulbDiscoveryHandler(evt) {
	def bulbs = evt.jsonData[0]
    def mac = evt.jsonData[1]
    def bridge = getBridge(mac)
    def bridgeDev = getChildDevice(mac)
	bulbs.each { k, v ->
		bridge.value.bulbs[k] = [id: k, name: v.name, type: v.type, state: v.state]
    }
    state.bulbsDiscovered = true
    bridgeDev.discoverScenes()
}

def sceneDiscoveryHandler(evt) {
	def scenes = evt.jsonData[0]
    def mac = evt.jsonData[1]
    def bridge = getBridge(mac)
    def bridgeDev = getChildDevice(mac)
    scenes.each { k, v ->
		bridge.value.scenes[k] = [id: k, name: v.name]
    }
    state.scenesDiscovered = true
    bridgeDev.discoverGroups()    
}

def groupDiscoveryHandler(evt) {
	def groups = evt.jsonData[0]
    def mac = evt.jsonData[1]
    def bridge = getBridge(mac)
	groups.each { k, v ->
		bridge.value.groups[k] = [id: k, name: v.name, type: v.type, action: v.action]
    }
    state.groupsDiscovered = true
}

def locationHandler(evt) {
    def description = evt.description
    def hub = evt?.hubId

    def parsedEvent = parseLanMessage(description)
    parsedEvent << ["hub":hub]

    if (parsedEvent?.ssdpTerm?.contains("urn:schemas-upnp-org:device:basic:1")) {
        /* SSDP response */
        processDiscoveryResponse(parsedEvent)
    }
}

/**
 * HUE BRIDGE COMMANDS
 **/
private discoverHueBridges() {
    log.debug("Sending bridge discovery.")
    sendHubCommand(new physicalgraph.device.HubAction("lan discovery urn:schemas-upnp-org:device:basic:1", physicalgraph.device.Protocol.LAN))
}

private verifyHueBridges() {
    def devices = getUnlinkedBridges().findAll { it?.value?.verified != true }
    devices.each {
        def ip = convertHexToIP(it.value.networkAddress)
        def port = convertHexToInt(it.value.deviceAddress)
        verifyHueBridge("${it.value.mac}", (ip + ":" + port))
    }
}

private verifyHueBridge(String deviceNetworkId, String host) {
    log.debug("Sending verify request for ${deviceNetworkId} (${host})")
	sendHubCommand(getHubAction(host, "/description.xml", "processVerifyResponse"))
}

/**
 * HUE BRIDGE RESPONSES
 **/
def processDiscoveryResponse(parsedEvent) {
    log.debug("Discovered bridge ${parsedEvent.mac} (${convertHexToIP(parsedEvent.networkAddress)})")

    def bridge = getUnlinkedBridges().find{it?.key?.contains(parsedEvent.ssdpUSN)} 
    if (!bridge) { bridge = getLinkedBridges().find{it?.key?.contains(parsedEvent.ssdpUSN)} }
    if (bridge) {
        /* have already discovered this bridge */
        log.debug("Previously found bridge discovered")
        /* update IP address */
        if (parsedEvent.networkAddress != bridge.value.networkAddress) {
        	bridge.value.networkAddress = parsedEvent.networkAddress
        	def bridgeDev = getChildDevice(parsedEvent.mac)
            if (bridgeDev) {
            	bridgeDev.sendEvent(name: "networkAddress", value: convertHexToIP(bridge.value.networkAddress))
            }
        }
    } else {
        log.debug("Found new bridge.")
        state.unlinked_bridges << ["${parsedEvent.ssdpUSN}":parsedEvent]
    }
}

private processVerifyResponse(resp) {
    log.debug("Processing verify response.")
    def body = new XmlSlurper().parseText(parseLanMessage(resp.description).body)
    if (body?.device?.modelName?.text().startsWith("Philips hue bridge")) {
        log.debug(body?.device?.UDN?.text())
        def bridge = getUnlinkedBridges().find({it?.key?.contains(body?.device?.UDN?.text())})
        if (bridge) {
        	log.debug "Verified ${bridge.value.mac}"
            bridge.value << [name:body?.device?.friendlyName?.text(), serialNumber:body?.device?.serialNumber?.text(), verified: true, itemsDiscovered: false]
        } else {
            log.error "/description.xml returned a bridge that didn't exist"
        }
    }
}

def processLinkResponse(resp) {
	def parsedEvent = parseLanMessage(resp.description)
	def body = new groovy.json.JsonSlurper().parseText(parsedEvent.body)
    if (body.success != null && body.success[0] != null && body.success[0].username) {
 		// got username from bridge 
	    state.params.linkDone = true
		state.params.username = body.success[0].username
	}
}

/**
 * UTILITY FUNCTIONS
 **/
def getCommandData(id) {
    def ids = id.split("/")
    def bridge = getBridge(ids[0])
    def bridgeDev = getChildDevice(ids[0])

    def result = [ip: "${bridgeDev.currentValue("networkAddress")}:80",
                  username: "${bridgeDev.currentValue("username")}",
                  deviceId: "${ids[1] - "BULB" - "GROUP" - "SCENE"}",
    ]
    return result
}

private Integer convertHexToInt(hex) {
    Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
    [convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

def scaleLevel(level, fromST = false, max = 254) {
    /* scale level from 0-254 to 0-100 */
    if (fromST) {
        return Math.round( level * max / 100 )
    } else {
        return Math.round( level * 100 / max )
    }
}

def parse(desc) {
    log.debug("parse")
}

def doDeviceSync() {
	log.debug "Doing Hue Device Sync!"
    state.doingSync = true
    try {
		subscribe(location, null, locationHandler, [filterEvents:false])
    } catch (e) {
 	}
	state.linked_bridges.each {
		def bridgeDev = getChildDevice(it.value.mac)
        if (bridgeDev) {
        	bridgeDev.discoverBulbs()
        }
	}
	discoverHueBridges()
    state.doingSync = false
}

def getHubAction(host, url, callback) {
    return new physicalgraph.device.HubAction("GET ${url} HTTP/1.1\r\nHOST: ${host}\r\n\r\n",
            physicalgraph.device.Protocol.LAN, "${host}", [callback: callback])
}

def putHubAction(host, url, body, callback) {
	def jBody = new groovy.json.JsonBuilder(body)
    def len = jBody.toString().length()
    
    return new physicalgraph.device.HubAction("PUT ${url} HTTP/1.1\r\nHOST: ${host}\r\nContent-Length: ${len}\r\n\r\n${jBody}",
            physicalgraph.device.Protocol.LAN, "${host}", [callback: callback])
}

def postHubAction(host, url, body, callback) {
	def jBody = new groovy.json.JsonBuilder(body)
    def len = jBody.toString().length()
    
    return new physicalgraph.device.HubAction("POST ${url} HTTP/1.1\r\nHOST: ${host}\r\nContent-Length: ${len}\r\n\r\n${jBody}",
            physicalgraph.device.Protocol.LAN, "${host}", [callback: callback])
}