/**
 *  Sensibo Integration for Hubitat
 *
 *  Copyright 2015 Eric Gosselin
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
 *  Date          Comments
 *  2019-04-27    Based on work by Eric Gosselin - Modified for Hubitat
 *  2019-07-23    Merge robert1's logging changes
 */

definition(
    name: "Sensibo Integration",
    namespace: "joyfulhouse",
    author: "https://community.hubitat.com/u/blink",
    description: "Connect your Sensibo Pod to Hubitat.",
    category: "Green Living",
    iconUrl: "https://image.ibb.co/j7qAPT/Sensibo_1x.png",
    iconX2Url: "https://image.ibb.co/coZtdo/Sensibo_2x.png",
    iconX3Url: "https://image.ibb.co/cBwTB8/Sensibo_3x.png",
    singleInstance: true) 

{
    appSetting "apikey"
    appSetting "debugLevel"
}

preferences {
    page(name: "SelectAPIKey", title: "API Key", content: "setAPIKey", nextPage: "deviceList", install: false, uninstall: true)
    page(name: "deviceList", title: "Sensibo", content:"SensiboPodList", install:true, uninstall: true)
    page(name: "timePage")
    page(name: "timePageEvent")
}

def getServerUrl() { "https://home.sensibo.com" }
def getapikey() { apiKey }

// debug logging set by debugLevel application preference
def debugLog() { 
	if (debugLevel == "TRACE" || debugLevel == "DEBUG")
		return true
	else
		return false 
}

def traceLog() { 
	if (debugLevel == "TRACE")
		return true
	else
		return false 
}

public static String version() { return "SmartThingsv1.6" }



private def displayDebugLog(message) {
	if (debugLog() == true) log.debug "${message}"
}

private def displayTraceLog(message) {
	if (traceLog() == true) log.trace  "${message}"
}


def setAPIKey()
{
	displayTraceLog( "setAPIKey()")
    
	if(appSettings)
    	def pod = appSettings.apikey
    else {
        def pod = ""
    }
	
    def p = dynamicPage(name: "SelectAPIKey", title: "Enter your API Key", uninstall: true) {
		section("API Key"){
			paragraph "Please enter your API Key provided by Sensibo \n\nAvailable at: \nhttps://home.sensibo.com/me/api"
			input(name: "apiKey", title:"", type: "text", required:true, multiple:false, description: "", defaultValue: pod)
		}
	    	section("Logging"){
			paragraph "Application Logging Level"
			input(name: "logLevel", type: "enum", title: "Logging Level", required:true, multiple:false, options: ["NONE","DEBUG","TRACE"], defaultValue: "NONE")
		}
	}
    return p
}

def SensiboPodList()
{
	displayTraceLog( "SensiboPodList()")

	def stats = getSensiboPodList()
	displayDebugLog( "device list: $stats")
    
	def p = dynamicPage(name: "deviceList", title: "Select Your Sensibo Pod", uninstall: true) {
		section(""){
			paragraph "Tap below to see the list of Sensibo Pods available in your Sensibo account and select the ones you want to connect to SmartThings."
			input(name: "SelectedSensiboPods", title:"Pods", type: "enum", required:true, multiple:true, description: "Tap to choose",  options: stats)
		}
        
        section("Refresh") {
        	input(name:"refreshinminutes", title: "Refresh rates in minutes", type: "enum", required:false, multiple: false, options: ["1", "5", "10","15","30"])
        }

	 	// No push notifications on Hubitat
        /*
        section("Receive Pod sensors infos") {
        	input "boolnotifevery", "bool",submitOnChange: true, required: false, title: "Receive temperature, humidity and battery level notification every hour?"
            href(name: "toTimePageEvent",
                     page: "timePageEvent", title:"Only during a certain time", require: false)
        }

        section("Alert on sensors (threshold)") {
        	input "sendPushNotif", "bool",submitOnChange: true, required: false, title: "Receive alert on Sensibo Pod sensors based on threshold?"                       
        }

		if (sendPushNotif) {
           section("Select the temperature threshold",hideable: true) {
            	input "minTemperature", "decimal", title: "Min Temperature",required:false
            	input "maxTemperature", "decimal", title: "Max Temperature",required:false }
            section("Select the humidity threshold",hideable: true) {
            	input "minHumidity", "decimal", title: "Min Humidity level",required:false
            	input "maxHumidity", "decimal", title: "Max Humidity level",required:false }              
         
        	section("How frequently?") {
        		input(name:"days", title: "Only on certain days of the week", type: "enum", required:false, multiple: true, options: ["Monday", "Tuesday", "Wednesday","Thursday","Friday","Saturday","Sunday"])
        	}
        	section("") {
        		href(name: "toTimePage",
                	 page: "timePage", title:"Only during a certain time", require: false)
        	}
        }
        */
	}
	return p
}

// page def must include a parameter for the params map!
def timePage() {
    dynamicPage(name: "timePage", uninstall: false, install: false, title: "Only during a certain time") {
      section("") {
        input(name: "startTime", title: "Starting at : ", required:false, multiple: false, type:"time",)
        input(name: "endTime", title: "Ending at : ", required:false, multiple: false, type:"time")
      }
   }
}

// page def must include a parameter for the params map!
def timePageEvent() {
    dynamicPage(name: "timePageEvent", uninstall: false, install: false, title: "Only during a certain time") {
      section("") {
        input(name: "startTimeEvent", title: "Starting at : ", required:false, multiple: false, type:"time",)
        input(name: "endTimeEvent", title: "Ending at : ", required:false, multiple: false, type:"time")
      }
   }
}

def getSensiboPodList()
{
	displayTraceLog( "getSensiboPodList called")
       
    def deviceListParams = [
    uri: "${getServerUrl()}",
    path: "/api/v2/users/me/pods",
    requestContentType: "application/json",
    query: [apiKey:"${getapikey()}", integration:"${version()}", type:"json",fields:"id,room" ]]

	def pods = [:]
	
    try {
      httpGet(deviceListParams) { resp ->
    	if(resp.status == 200)
			{
				resp.data.result.each { pod ->
                    def key = pod.id
                    def value = pod.room.name
                        
					pods[key] = value
				}
                state.pods = pods
			}
	  }
    }
    catch(Exception e)
	{
		displayDebugLog( "Exception Get Json: " + e)
		debugEvent ("Exception get JSON: " + e)
	}
    
    displayDebugLog( "Sensibo Pods: $pods"  )
	
    return pods
}

def installed() {
	displayTraceLog( "Installed() called with settings: ${settings}")

	state.lastTemperaturePush = null
    state.lastHumidityPush = null
  
	initialize()
    
    def d = getChildDevices()

	if (boolnotifevery) {
    	//runEvery1Hour("hournotification")
        schedule("0 0 * * * ?", "hournotification")
	}
    
    displayDebugLog( "Configured health checkInterval when installed()")
	sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 2 * 60, displayed: true)
    
    //subscribe(d,"temperatureUnit",eTempUnitHandler)
    
    if (sendPushNotif) { 
    	subscribe(d, "temperature", eTemperatureHandler)
        subscribe(d, "humidity", eHumidityHandler)
    }
}

def updated() {
	displayTraceLog( "Updated() called with settings: ${settings}")

	unschedule()
    unsubscribe()
	
    state.lastTemperaturePush = null
    state.lastHumidityPush = null
    
    initialize()
    
    def d = getChildDevices()
    
    if (boolnotifevery) {
    	//runEvery1Hour("hournotification")
        schedule("0 0 * * * ?", "hournotification")
	}
    
    displayDebugLog( "Configured health checkInterval when installed()")
	sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 2 * 60, displayed: true)
    
    //subscribe(d,"temperatureUnit",eTempUnitHandler)
    
    if (sendPushNotif) {
    	subscribe(d, "temperature", eTemperatureHandler)
        subscribe(d, "humidity", eHumidityHandler)
    }
}

def ping() {

	displayTraceLog( "ping called")
    def returnStatus = true
    
    def deviceListParams = [
    uri: "${getServerUrl()}",
    path: "/api/v2/users/me/pods",
    requestContentType: "application/json",
    query: [apiKey:"${getapikey()}", integration:"${version()}", type:"json",fields:"id,room" ]]
	
    try {
      httpGet(deviceListParams) { resp ->
    	if(resp.status == 200)
			{
				returnStatus = true
			}
	  }
    }
    catch(Exception e)
	{
		displayDebugLog( "Exception Get Json: " + e)
		debugEvent ("Exception get JSON: " + e)
		
		returnStatus = false
	}
    
    return returnStatus
}

def hournotification() {
	displayTraceLog( "hournotification() called")
    
	def hour = new Date()
	def curHour = hour.format("HH:mm",location.timeZone)
	def curDay = hour.format("EEEE",location.timeZone)
	// Check the time Threshold
    def stext = ""
	if (startTimeEvent && endTimeEvent) {
 		def minHour = new Date().parse(smartThingsDateFormat(), startTimeEvent)
    	def endHour = new Date().parse(smartThingsDateFormat(), endTimeEvent)

    	def minHourstr = minHour.format("HH:mm",location.timeZone)
    	def maxHourstr = endHour.format("HH:mm",location.timeZone)

    	if (curHour >= minHourstr && curHour <= maxHourstr) 
    	{
    		def devices = getChildDevices()
            devices.each { d ->
                displayTraceLog( "Notification every hour for device: ${d.id}")
                def currentPod = d.displayName
                def currentTemperature = d.currentState("temperature").value
                def currentHumidity = d.currentState("humidity").value
                def currentBattery = d.currentState("voltage").value
                def sunit = d.currentState("temperatureUnit").value
                stext = "${currentPod} - Temperature: ${currentTemperature} ${sunit} Humidity: ${currentHumidity}% Battery: ${currentBattery}"    
                
                sendPush(stext)
            }
    	}
    }
    else {
    	 	def devices = getChildDevices()
            devices.each { d ->
                displayTraceLog( "Notification every hour for device: ${d.id}")
                def currentPod = d.displayName
                def currentTemperature = d.currentState("temperature").value
                def currentHumidity = d.currentState("humidity").value
                def currentBattery = d.currentState("voltage").value
                def sunit = d.currentState("temperatureUnit").value
                stext = "${currentPod} - Temperature: ${currentTemperature} ${sunit} Humidity: ${currentHumidity}% Battery: ${currentBattery}"    
                
                sendPush(stext)
            }
    }
}

//def switchesHandler(evt)
//{
//  if (evt.value == "on") {
//        displayDebugLog( "switch turned on!")
//    } else if (evt.value == "off") {
//        displayDebugLog( "switch turned off!")
//    }
//}

def eTempUnitHandler(evt)
{
	//refreshOneDevice(evt.device.displayName)
}

def eTemperatureHandler(evt){
	def currentTemperature = evt.device.currentState("temperature").value
    def currentPod = evt.device.displayName
    def hour = new Date()
    
    if (inDateThreshold(evt,"temperature") == true) {
        if(maxTemperature != null){
            if(currentTemperature.toDouble() > maxTemperature)
            {
            	def stext = "Temperature level is too high at ${currentPod} : ${currentTemperature}"
				sendEvent(name: "lastTemperaturePush", value: "${stext}",  displayed : "true", descriptionText:"${stext}")
                sendPush(stext)

                state.lastTemperaturePush = hour
            }
        }
        if(minTemperature != null) {
            if(currentTemperature.toDouble() < minTemperature)
            {	
            	def stext = "Temperature level is too low at ${currentPod} : ${currentTemperature}"
                sendEvent(name: "lastTemperaturePush", value: "${stext}",  displayed : "true", descriptionText:"${stext}")
                sendPush(stext)

                state.lastTemperaturePush = hour
            }
        }
    } 
}

def eHumidityHandler(evt){
	def currentHumidity = evt.device.currentState("humidity").value
    def currentPod = evt.device.displayName
    def hour = new Date()
    if (inDateThreshold(evt,"humidity") == true) { 
        if(maxHumidity != null){
            if(currentHumidity.toDouble() > maxHumidity)
            {   
            	def stext = "Humidity level is too high at ${currentPod} : ${currentHumidity}"
                sendEvent(name: "lastHumidityPush", value: "${stext}", displayed : "true", descriptionText:"${stext}")
                sendPush(stext)
                
                state.lastHumidityPush = hour
            }
        }
        if(minHumidity != null) {
            if(currentHumidity.toDouble() < minHumidity)
            {
            	def stext = "Humidity level is too low at ${currentPod} : ${currentHumidity}"
                sendEvent(name: "lastHumidityPush", value: "${stext}", displayed : "true", descriptionText:"${stext}")
                sendPush(stext)
                
                state.lastHumidityPush = hour
            }
        }
    }
}

public smartThingsDateFormat() { "yyyy-MM-dd'T'HH:mm:ss.SSSZ" }
public smartThingsDateFormatNoMilli() { "yyyy-MM-dd'T'HH:mm:ssZ" }

def canPushNotification(currentPod, hour,sType) {
    // Check if the client already received a push
    if (sType == "temperature") {
        if (sfrequency.toString().isInteger()) {
            if (state.lastTemperaturePush != null) {
                long unxNow = hour.time

                def before = new Date().parse(smartThingsDateFormatNoMilli(),state.lastTemperaturePush)
                long unxEnd = before.time
                
                unxNow = unxNow/1000
                unxEnd = unxEnd/1000
                def timeDiff = Math.abs(unxNow-unxEnd)
                timeDiff = timeDiff/60
                if (timeDiff <= sfrequency)
                {
                    return false
                }
            }
    	}
    }
    else {
        if (sfrequency.toString().isInteger()) {
            if (state.lastHumidityPush != null) {
                long unxNow = hour.time
                
                def before = new Date().parse(smartThingsDateFormatNoMilli(),state.lastHumidityPush)
                long unxEnd = before.time

                unxNow = unxNow/1000
                unxEnd = unxEnd/1000
                def timeDiff = Math.abs(unxNow-unxEnd)
                timeDiff = timeDiff/60

                if (timeDiff <= sfrequency)
                {
                    return false
                }
            }
    	}
   	}

    return true
}

def inDateThreshold(evt,sType) {
	def hour = new Date()
	def curHour = hour.format("HH:mm",location.timeZone)
	def curDay = hour.format("EEEE",location.timeZone)
    def currentPod = evt.device.displayName
     
    // Check if the client already received a push
    
    def result = canPushNotification(currentPod,hour, sType)
    if (!result) { 
        return false 
    }
   
    // Check the day of the week
    if (days != null && !days.contains(curDay)) {
    	return false
    }
    
	// Check the time Threshold
	if (startTime && endTime) {
 		def minHour = new Date().parse(smartThingsDateFormat(), startTime)
    	def endHour = new Date().parse(smartThingsDateFormat(), endTime)

    	def minHourstr = minHour.format("HH:mm",location.timeZone)
    	def maxHourstr = endHour.format("HH:mm",location.timeZone)

    	if (curHour >= minHourstr && curHour < maxHourstr) 
    	{
    		return true
    	}
    	else
    	{ 
	    	return false
	    }
    }
    return true
}

def refresh() {
	displayTraceLog( "refresh() called with rate of " + refreshinminutes + " minutes")

    unschedule()
    
	refreshDevices()
    
    if (refreshinminutes == "1") 
    	runEvery1Minute("refreshDevices")
    else if (refreshinminutes == "5")
    	runEvery5Minutes("refreshDevices")
    else if (refreshinminutes == "10")
    	runEvery10Minutes("refreshDevices")
    else if (refreshinminutes == "15") 
    	runEvery15Minutes("refreshDevices")
    else if (refreshinminutes == "30")
    	runEvery30Minutes("refreshDevices")
    else
        runEvery10Minutes("refreshDevices")
}


def refreshOneDevice(dni) {
	displayTraceLog( "refreshOneDevice() called")
	def d = getChildDevice(dni)
	d.refresh()
}

def refreshDevices() {
	displayTraceLog( "refreshDevices() called")
	def devices = getChildDevices()
	devices.each { d ->
		displayDebugLog( "Calling refresh() on device: ${d.id}")
        
		d.refresh()
	}
}

def getChildNamespace() { "EricG66" }
def getChildTypeName() { "SensiboPod" }

def initialize() {
    displayTraceLog( "initialize() called")
    displayTraceLog( "key "+ getapikey())
    
    state.apikey = getapikey()
	  
	def devices = SelectedSensiboPods.collect { dni ->
		displayDebugLog( dni)
		def d = getChildDevice(dni)

		if(!d)
			{
                
            	def name = getSensiboPodList().find( {key,value -> key == dni })
				displayDebugLog( "Pod : ${name.value} - Hub : ${location.hubs[0].name} - Type : " +  getChildTypeName() + " - Namespace : " + getChildNamespace())

				d = addChildDevice(getChildNamespace(), getChildTypeName(), dni, location.hubs[0].id, [
                	"label" : "Pod ${name.value}",
                    "name" : "Pod ${name.value}"
                    ])
                d.setIcon("on","on","https://image.ibb.co/jgAMW8/sensibo-sky-off.png")
                d.setIcon("off","on","https://image.ibb.co/jgAMW8/sensibo-sky-off.png")
                d.save()              
                
				displayTraceLog( "created ${d.displayName} with id $dni")
			}
			else
			{
				displayTraceLog( "found ${d.displayName} with id $dni already exists")
			}

			return d
		}

	displayTraceLog( "created ${devices.size()} Sensibo Pod")

	def delete
	// Delete any that are no longer in settings
	if(!SelectedSensiboPods)
	{
		displayDebugLog( "delete Sensibo")
		delete = getChildDevices()
	}
	else
	{
		delete = getChildDevices().findAll { !SelectedSensiboPods.contains(it.deviceNetworkId) }
	}

	displayTraceLog( "deleting ${delete.size()} Sensibo")
	delete.each { deleteChildDevice(it.deviceNetworkId) }

	def PodList = getChildDevices()
	
    pollHandler()
    
    refreshDevices()
    
    if (refreshinminutes == "1") 
    	runEvery1Minute("refreshDevices")
    else if (refreshinminutes == "5")
    	runEvery5Minutes("refreshDevices")
    else if (refreshinminutes == "10")
    	runEvery10Minutes("refreshDevices")
    else if (refreshinminutes == "15") 
    	runEvery15Minutes("refreshDevices")
    else if (refreshinminutes == "30")
    	runEvery30Minutes("refreshDevices")
    else
    	runEvery10Minutes("refreshDevices")
}


// Subscribe functions

def OnOffHandler(evt) {
	displayTraceLog( "on off handler activated")
    debugEvent(evt.value)
    
	//def name = evt.device.displayName

    if (sendPush) {
        if (evt.value == "on") {
            //sendPush("The ${name} is turned on!")
        } else if (evt.value == "off") {
            //sendPush("The ${name} is turned off!")
        }
    }
}

def getPollRateMillis() { return 45 * 1000 }
def getCapabilitiesRateMillis() {return 60 * 1000 }

// Poll Child is invoked from the Child Device itself as part of the Poll Capability
def pollChild( child )
{
	displayTraceLog( "pollChild() called")
	debugEvent ("poll child")
	def now = new Date().time

	debugEvent ("Last Poll Millis = ${state.lastPollMillis}")
	def last = state.lastPollMillis ?: 0
	def next = last + pollRateMillis
	
	displayDebugLog( "pollChild( ${child.device.deviceNetworkId} ): $now > $next ?? w/ current state: ${state.sensibo}")
	debugEvent ("pollChild( ${child.device.deviceNetworkId} ): $now > $next ?? w/ current state: ${state.sensibo}")

	//if( now > next )
	if( true ) // for now let's always poll/refresh
	{
		displayDebugLog( "polling children because $now > $next")
		debugEvent("polling children because $now > $next")

		pollChildren(child.device.deviceNetworkId)

		displayDebugLog( "polled children and looking for ${child.device.deviceNetworkId} from ${state.sensibo}")
		debugEvent ("polled children and looking for ${child.device.deviceNetworkId} from ${state.sensibo}")

		def currentTime = new Date().time
		debugEvent ("Current Time = ${currentTime}")
		state.lastPollMillis = currentTime

		def tData = state.sensibo[child.device.deviceNetworkId]
        
        if (tData == null) return
        
        displayDebugLog(  "DEBUG - TDATA" + tData)
        debugEvent ("Error in Poll ${tData.data.Error}",false)
        //tData.Error = false
        //tData.data.Error = "Failed"
		if(tData.data.Error != "Success")
		{
			log.error "ERROR: Device connection removed? no data for ${child.device.deviceNetworkId} after polling"

			// TODO: flag device as in error state
			// child.errorState = true

			return null
		}

		tData.updated = currentTime
		
		return tData.data
	}
	else if(state.sensibo[child.device.deviceNetworkId] != null)
	{
		displayDebugLog( "not polling children, found child ${child.device.deviceNetworkId} ")

		def tData = state.sensibo[child.device.deviceNetworkId]
		if(!tData.updated)
		{
			// we have pulled new data for this thermostat, but it has not asked us for it
			// track it and return the data
			tData.updated = new Date().time
			return tData.data
		}
		return null
	}
	else if(state.sensibo[child.device.deviceNetworkId] == null)
	{
		log.error "ERROR: Device connection removed? no data for ${child.device.deviceNetworkId}"

		// TODO: flag device as in error state
		// child.errorState = true

		return null
	}
	else
	{
		// it's not time to poll again and this thermostat already has its latest values
	}

	return null
}

def configureClimateReact(child,String PodUid,String JsonString)
{
	displayTraceLog( "configureClimateReact() called for $PodUid with settings : $JsonString"  )
    
    def result = sendPostJsonClimate(PodUid, JsonString)
    
    if (result) {  
		def tData = state.sensibo[child.device.deviceNetworkId]      
        
        if (tData == null) {
        	pollChildren(child.device.deviceNetworkId)
            tData = state.sensibo[child.device.deviceNetworkId]
        }
        
        //tData.data.Climate = ClimateState        
        tData.data.Error = "Success"
    }
    else {
    	def tData = state.sensibo[child.device.deviceNetworkId]
        if (tData == null) return false
    	
        tData.data.Error = "Failed"
    }

    return(result)
}

def setClimateReact(child,String PodUid, ClimateState)
{
	displayTraceLog( "setClimateReact() called for $PodUid Climate React: $ClimateState"   )
    
    def ClimateReact = getClimateReact(PodUid)
    displayDebugLog( "DEBUG " + ClimateReact.Climate + " " + ClimateState)
    if (ClimateReact.Climate == "notdefined") {
    	def tData = state.sensibo[child.device.deviceNetworkId]      
        
        if (tData == null) {
        	pollChildren(child.device.deviceNetworkId)
            tData = state.sensibo[child.device.deviceNetworkId]
        }
        
        tData.data.Climate = ClimateReact.Climate        
        tData.data.Error = "Success"
        
        return true
    }
    
    def jsonRequestBody
    if (ClimateState == "on") { 
    	jsonRequestBody = '{"enabled": true}' 
    }
    else {
    	jsonRequestBody = '{"enabled": false}' 
    }
    
    displayDebugLog( "Mode Request Body = ${jsonRequestBody}")
    
    def result = sendPutJson(PodUid, jsonRequestBody)
    
    if (result) {  
		def tData = state.sensibo[child.device.deviceNetworkId]      
        
        if (tData == null) {
        	pollChildren(child.device.deviceNetworkId)
            tData = state.sensibo[child.device.deviceNetworkId]
        }
        
        tData.data.Climate = ClimateState        
        tData.data.Error = "Success"
    }
    else {
    	def tData = state.sensibo[child.device.deviceNetworkId]
        if (tData == null) return false
    	
        tData.data.Error = "Failed"
    }

    return(result)
}

def setACStates(child,String PodUid, on, mode, targetTemperature, fanLevel, swingM, sUnit)
{
	displayTraceLog( "setACStates() called for $PodUid ON: $on - MODE: $mode - Temp : $targetTemperature - FAN : $fanLevel - SWING MODE : $swingM - UNIT : $sUnit")
    
    //Return false if no values was read from Sensibo API
    if (on == "--") { return false }
    
    def OnOff = (on == "on") ? true : false
    //if (swingM == null) swingM = "stopped"
    
    displayTraceLog( "Target Temperature :" + targetTemperature)
    
	def jsonRequestBody = '{"acState":{"on": ' + OnOff.toString() + ',"mode": "' + mode + '"'
    
    displayDebugLog( "Fan Level is :$fanLevel")
    displayDebugLog( "Swing is :$swingM")
    displayDebugLog( "Target Temperature is :$targetTemperature")
    
    if (fanLevel != null) {
       displayDebugLog( "Fan Level info is present")
       jsonRequestBody += ',"fanLevel": "' + fanLevel + '"'
    }
    
    if (targetTemperature != 0) {
    	jsonRequestBody += ',"targetTemperature": '+ targetTemperature + ',"temperatureUnit": "' + sUnit + '"'       
    }
    if (swingM)
    {
        jsonRequestBody += ',"swing": "' + swingM + '"'
    }
    
    jsonRequestBody += '}}'
    
    displayDebugLog( "Mode Request Body = ${jsonRequestBody}")
	debugEvent ("Mode Request Body = ${jsonRequestBody}")

	boolean result = true
	if(!sendJson(PodUid, jsonRequestBody))
	{ result = false }
		
	if (result) {
		def tData = state.sensibo[child.device.deviceNetworkId]      
        
        if (tData == null) {
        	pollChildren(child.device.deviceNetworkId)
            tData = state.sensibo[child.device.deviceNetworkId]
        }        
        
        displayDebugLog( "Device : " + child.device.deviceNetworkId + " state : " + tData)
        
		tData.data.fanLevel = fanLevel
        tData.data.thermostatFanMode = fanLevel
        tData.data.on = on
        tData.data.currentmode = mode
        displayDebugLog( "Thermostat mode " + on)
        if (on=="off") {
        	tData.data.thermostatMode = "off"
        }
        else {
        	 tData.data.thermostatMode = mode
        }
        tData.data.targetTemperature = targetTemperature
        tData.data.coolingSetpoint = targetTemperature
        tData.data.heatingSetpoint = targetTemperature
        tData.data.thermostatSetpoint = targetTemperature
        tData.data.temperatureUnit = sUnit
        tData.data.swing = swingM
        tData.data.Error = "Success"
	}
    else {
    	def tData = state.sensibo[child.device.deviceNetworkId]
        if (tData == null) return false
    	
        tData.data.Error = "Failed"
    }

	return(result)
}

//Get the capabilities of the A/C Unit
def getCapabilities(PodUid, mode)
{
	displayTraceLog( "getCapabilities() called")
    def now = new Date().time
    
    def last = state.lastPollCapabilitiesMillis ?: 0
	def next = last + getCapabilitiesRateMillis()
    
    def data = [:] 
   
    if (state.capabilities == null || state.capabilities.$PodUid == null || now > next)
    //if (true)
	{
    	displayDebugLog( "Now : " + now + " Next : " + next    	)
        
    	//def data = [:]   
		def pollParams = [
    	uri: "${getServerUrl()}",
    	path: "/api/v2/pods/${PodUid}",
    	requestContentType: "application/json",
    	query: [apiKey:"${getapikey()}", integration:"${version()}", type:"json", fields:"remoteCapabilities,productModel"]]
     
     	try {
			displayTraceLog( "getCapabilities() called - Request sent to Sensibo API(remoteCapabilities) for PODUid : $PodUid - ${version()}")
            
     		httpGet(pollParams) { resp ->
                if (resp.data) {
                    displayDebugLog( "Status : " + resp.status)
                    if(resp.status == 200) {
                        //resp.data = [result: [remoteCapabilities: [modes: [heat: [swing: ["stopped", "fixedTop", "fixedMiddleTop", "fixedMiddle", "fixedMiddleBottom", "fixedBottom", "rangeTop", "rangeMiddle", "rangeBottom", "rangeFull"], temperatures: [C: ["isNative": true, "values": [16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30]], F: ["isNative": false, "values": [61, 63, 64, 66, 68, 70, 72, 73, 75, 77, 79, 81, 82, 84, 86]]], fanLevels: ["low", "medium", "high", "auto"]], fan: [swing: ["stopped", "fixedMiddleTop", "fixedMiddle", "fixedMiddleBottom", "fixedBottom", "rangeTop", "rangeMiddle", "rangeBottom", "rangeFull"], temperatures: [C: ["isNative": true, "values": [16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30]], F: ["isNative": false, "values": [61, 63, 64, 66, 68, 70, 72, 73, 75, 77, 79, 81, 82, 84, 86]]], fanLevels: ["low", "medium", "high", "auto"]], cool: [swing: ["stopped", "fixedTop", "fixedMiddleTop", "fixedMiddle", "fixedMiddleBottom", "fixedBottom", "rangeTop", "rangeMiddle", "rangeBottom", "rangeFull"], temperatures: ["C": ["isNative": true, "values": [16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30]], F: ["isNative": false, "values": [61, 63, 64, 66, 68, 70, 72, 73, 75, 77, 79, 81, 82, 84, 86]]], fanLevels: ["low", "high", "auto"]]]]]]
                        //resp.data = ["result": ["productModel": "skyv2", "remoteCapabilities": ["modes": ["dry": ["temperatures": ["C": ["isNative": false, "values": [17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30]], "F": ["isNative": true, "values": [62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86]]], "swing": ["stopped", "rangeFull"]], "auto": ["temperatures": ["C": ["isNative": false, "values": [17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30]], "F": ["isNative": true, "values": [62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86]]], "swing": ["stopped", "rangeFull"]], "heat": ["swing": ["stopped", "rangeFull"], "temperatures": ["C": ["isNative": false, "values": [17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30]], "F": ["isNative": true, "values": [62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86]]], "fanLevels": ["low", "medium", "high", "auto"]], "fan": ["swing": ["stopped", "rangeFull"], "temperatures": [], "fanLevels": ["low", "medium", "high", "auto"]], "cool": ["swing": ["stopped", "rangeFull"], "temperatures": ["C": ["isNative": false, "values": [17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30]], "F": ["isNative": true, "values": [62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86]]], "fanLevels": ["low", "medium", "high", "auto"]]]]]]
                        
                        displayDebugLog( resp.data)

                        if (state.capabilities == null) { state.capabilities = [:] }
                        
                        state.capabilities.$PodUid = resp.data
                        displayDebugLog( "Succes read from Sensibo")
                        displayTraceLog( "Capabilities from Sensibo for ${PodUid} : " + state.capabilities.$PodUid	)		
						
                        def currentTime = new Date().time
						debugEvent ("Current Time = ${currentTime}")
						state.lastPollCapabilitiesMillis = currentTime
                        
                        switch (mode){
                            case "dry":
                                data = [
                                    remoteCapabilities : resp.data.result.remoteCapabilities.modes.dry,
                                    productModel :  resp.data.result.productModel
                                ]	
                                break
                            case "cool":
                                data = [
                                    remoteCapabilities : resp.data.result.remoteCapabilities.modes.cool,
                                    productModel : resp.data.result.productModel
                                ]	
                                break
                            case "heat":
                                data = [
                                    remoteCapabilities : resp.data.result.remoteCapabilities.modes.heat,
                                    productModel : resp.data.result.productModel
                                ]	
                                break
                            case "fan":
                                data = [
                                    remoteCapabilities : resp.data.result.remoteCapabilities.modes.fan,
                                    productModel : resp.data.result.productModel
                                ]	
                                break
                            case "auto":
                                data = [
                                    remoteCapabilities : resp.data.result.remoteCapabilities.modes.auto,
                                    productModel : resp.data.result.productModel
                                ]	
                                break
                            case "modes":
                                data = [
                                    remoteCapabilities : resp.data.result.remoteCapabilities.modes,
                                    productModel : resp.data.result.productModel
                                ]	
                                break                        
                        }
                        displayTraceLog( "Returning remoteCapabilities from Sensibo")
                        return data
                    }
                    else {
                        displayDebugLog( "get remoteCapabilities Failed")

                        data = [
                            remoteCapabilities : "",
                            productModel : ""
                        ]                    
                        return data
                    }
                }
        	}
        	return data
     	}
     	catch(Exception e) {     	
        	displayDebugLog( "get remoteCapabilities Failed")
        
        	data = [
        		remoteCapabilities : "",
            	productModel : ""
        	]        
     		return data
     	}
    }
    
    else {
    	displayTraceLog( "Capabilities from local for ${PodUid} : " + state.capabilities.$PodUid)
        //return
    	switch (mode){
            case "dry":
            data = [
                remoteCapabilities : state.capabilities.$PodUid.result.remoteCapabilities.modes.dry,
                productModel : state.capabilities.$PodUid.result.productModel
            ]	
            break
            case "cool":
            data = [
                remoteCapabilities : state.capabilities.$PodUid.result.remoteCapabilities.modes.cool,
                productModel : state.capabilities.$PodUid.result.productModel
            ]	
            break
            case "heat":
            data = [
                remoteCapabilities :state.capabilities.$PodUid.result.remoteCapabilities.modes.heat,
                productModel : state.capabilities.$PodUid.result.productModel
            ]	
            break
            case "fan":
            data = [
                remoteCapabilities : state.capabilities.$PodUid.result.remoteCapabilities.modes.fan,
                productModel : state.capabilities.$PodUid.result.productModel
            ]	
            break
            case "auto":
            data = [
                remoteCapabilities : state.capabilities.$PodUid.result.remoteCapabilities.modes.auto,
                productModel : state.capabilities.$PodUid.result.productModel
            ]	
            break
            case "modes":
            data = [
                remoteCapabilities : state.capabilities.$PodUid.result.remoteCapabilities.modes,
                productModel : state.capabilities.$PodUid.result.productModel
            ]	
            break                        
        }
        displayTraceLog( "Returning remoteCapabilities from local")
        return data
    }                  
}


// Get Climate React settings
def getClimateReact(PodUid)
{
	displayTraceLog( "getClimateReact() called - ${version()}")
	def data = [:]
	def pollParams = [
    	uri: "${getServerUrl()}",
    	path: "/api/v2/pods/${PodUid}/smartmode",
    	requestContentType: "application/json",
    	query: [apiKey:"${getapikey()}", integration:"${version()}", type:"json", fields:"*"]]
        
    try {
    
       httpGet(pollParams) { resp ->           
			if (resp.data) {
				debugEvent ("Response from Sensibo GET = ${resp.data}")
				debugEvent ("Response Status = ${resp.status}")
			}
			
            displayTraceLog( "Get ClimateReact " + resp.data.result)
			if(resp.status == 200) {
                if (!resp.data.result) {
                	data = [
                 		Climate : "notdefined",
                 		Error : "Success"]
                    
                 	displayDebugLog( "Returning Climate React (not configured)")
                 	return data
                }
            	resp.data.result.any { stat ->                	
                    displayTraceLog( "get ClimateReact Success")
                    displayDebugLog( "PodUID : $PodUid : " + PodUid			)		
                    
                    def OnOff = "off"
                    
                    if (resp.data.result.enabled != null) {
                    	OnOff = resp.data.result.enabled ? "on" : "off"
                    }

                    data = [
                        Climate : OnOff.toString(),
                        Error : "Success"
                    ]

                    displayDebugLog( "Climate: ${data.Climate}")
                    displayTraceLog( "Returning Climate React"   )                     
                    return data
               }
            }
            else {
           	     data = [
                 	Climate : "notdefined",
                 	Error : "Failed"]
                    
                 displayDebugLog( "get ClimateReact Failed")
                 return data
            }
       }
       return data
    }
    catch(Exception e)
	{
		displayDebugLog( "Exception Get Json: " + e)
		debugEvent ("Exception get JSON: " + e)
		
        data = [
            Climate : "notdefined",            
            Error : "Failed" 
		]
        displayDebugLog( "get ClimateReact Failed")
        return data
	}      
}

// Get the latest state from the Sensibo Pod
def getACState(PodUid)
{
	displayTraceLog( "getACState() called - ${version()}")
	def data = [:]
	def pollParams = [
    	uri: "${getServerUrl()}",
    	path: "/api/v2/pods/${PodUid}/acStates",
    	requestContentType: "application/json",
    	query: [apiKey:"${getapikey()}", integration:"${version()}", type:"json", limit:1, fields:"status,acState,device"]]
    
    try {
       httpGet(pollParams) { resp ->

			if (resp.data) {
				debugEvent ("Response from Sensibo GET = ${resp.data}")
				debugEvent ("Response Status = ${resp.status}")
			}
			
			if(resp.status == 200) {
            	resp.data.result.any { stat ->
                	
                	if (stat.status == "Success") {
                    	
                        displayTraceLog( "get ACState Success")
                        displayDebugLog( "PodUID : $PodUid : " + stat.acState)
                        
                        def OnOff = stat.acState.on ? "on" : "off"
                        stat.acState.on = OnOff
						
						def stemp
                        if (stat.acState.targetTemperature == null) {
                          stemp = stat.device.measurements.temperature.toInteger()
                        }
                        else {
                          stemp = stat.acState.targetTemperature.toInteger()
                        }
                        
                        def tempUnit
                        if (stat.acState.temperatureUnit == null) {
                          tempUnit = stat.device.temperatureUnit
                        }
                        else {
                          tempUnit = stat.acState.temperatureUnit
                        }	
					
                        def tMode                        
                        if (OnOff=="off") {
        					tMode = "off"
        				}
				        else {
        	 				tMode = stat.acState.mode
                        }
						
						def sMode
						if (stat.acState.swing == null) {
                          sMode = "stopped"
                        }
                        else {
                          sMode = stat.acState.swing
                        }
                        
                        displayDebugLog( "product Model : " + stat.device.productModel)
                        def battery = stat.device.productModel == "skyv1" ? "battery" : "mains"
                        
                        displayDebugLog( "swing Mode :" + stat.acState.swing)
                        data = [
                            targetTemperature : stemp,
                            fanLevel : stat.acState.fanLevel,
                            currentmode : stat.acState.mode,
                            on : OnOff.toString(),
                            switch: OnOff.toString(),
                            thermostatMode: tMode,
                            thermostatFanMode : stat.acState.fanLevel,
                            coolingSetpoint : stemp,
                            heatingSetpoint : stemp,
                            thermostatSetpoint : stemp,
                            temperatureUnit : tempUnit,
                            swing : sMode,
                            powerSource : battery,
                            productModel : stat.device.productModel,
                            firmwareVersion : stat.device.firmwareVersion,
                            Error : "Success"
                        ]

                        displayDebugLog( "On: ${data.on} targetTemp: ${data.targetTemperature} fanLevel: ${data.fanLevel} Thermostat mode: ${data.mode} swing: ${data.swing}")
                        displayTraceLog( "Returning ACState")
                        return data
                	}
                    else { displayDebugLog( "get ACState Failed") }
               }
           }
           else {
           	  data = [
                 targetTemperature : "0",
                 fanLevel : "--",
                 currentmode : "--",
                 on : "--",
                 switch : "--",
                 thermostatMode: "--",
                 thermostatFanMode : "--",
                 coolingSetpoint : "0",
                 heatingSetpoint : "0",
                 thermostatSetpoint : "0",
                 temperatureUnit : "",
                 swing : "--",
                 powerSource : "",
                 productModel : "",
                 firmwareVersion : "",
                 Error : "Failed"
			  ]
              displayDebugLog( "get ACState Failed")
              return data
           }
       }
       return data
    }
    catch(Exception e)
	{
		displayDebugLog( "Exception Get Json: " + e)
		debugEvent ("Exception get JSON: " + e)
		
        data = [
            targetTemperature : "0",
            fanLevel : "--",
            currentmode : "--",
            on : "--",
            switch : "--",
            thermostatMode: "--",
            thermostatFanMode : "--",
            coolingSetpoint : "0",
            heatingSetpoint : "0",
            thermostatSetpoint : "0",
            temperatureUnit : "",
            swing : "--",
            powerSource : "",
            productModel : "",
            firmwareVersion : "",
            Error : "Failed" 
		]
        displayDebugLog( "get ACState Failed")
        return data
	} 
}

def sendPutJson(String PodUid, String jsonBody)
{
 	displayTraceLog( "sendPutJson() called - Request sent to Sensibo API(smartmode) for PODUid : $PodUid - ${version()} - $jsonBody")
	def cmdParams = [
		uri: "${getServerUrl()}",
		path: "/api/v2/pods/${PodUid}/smartmode",
		headers: ["Content-Type": "application/json"],
        query: [apiKey:"${getapikey()}", integration:"${version()}", type:"json"],
		body: jsonBody]

    try{
       httpPut(cmdParams) { resp ->
			if(resp.status == 200) {
                displayDebugLog( "updated ${resp.data}")
				debugEvent("updated ${resp.data}")
                displayTraceLog( "Successful call to Sensibo API.")
				               
                displayDebugLog( "Returning True")
				return true
            }
           	else { 
            	displayTraceLog( "Failed call to Sensibo API.")
                return false
            }
       }
    }    
    catch(Exception e)
	{
		displayDebugLog( "Exception Sending Json: " + e)
		debugEvent ("Exception Sending JSON: " + e)
		return false
	}
}

def sendPostJsonClimate(String PodUid, String jsonBody)
{
 	displayTraceLog( "sendPostJsonClimate() called - Request sent to Sensibo API(smartmode) for PODUid : $PodUid - ${version()} - $jsonBody")
	def cmdParams = [
		uri: "${getServerUrl()}",
		path: "/api/v2/pods/${PodUid}/smartmode",
		headers: ["Content-Type": "application/json"],
        query: [apiKey:"${getapikey()}", integration:"${version()}", type:"json"],
		body: jsonBody]

    try{
       httpPost(cmdParams) { resp ->
			if(resp.status == 200) {
                displayDebugLog( "updated ${resp.data}")
				debugEvent("updated ${resp.data}")
                displayTraceLog( "Successful call to Sensibo API.")
				               
                displayDebugLog( "Returning True")
				return true
            }
           	else { 
            	displayTraceLog( "Failed call to Sensibo API.")
                return false
            }
       }
    }    
    catch(Exception e)
	{
		displayDebugLog( "Exception Sending Json: " + e)
		debugEvent ("Exception Sending JSON: " + e)
		return false
	}
}

// Send state to the Sensibo Pod
def sendJson(String PodUid, String jsonBody)
{
    displayTraceLog( "sendJson() called - Request sent to Sensibo API(acStates) for PODUid : $PodUid - ${version()} - $jsonBody")
	def cmdParams = [
		uri: "${getServerUrl()}",
		path: "/api/v2/pods/${PodUid}/acStates",
		headers: ["Content-Type": "application/json"],
        query: [apiKey:"${getapikey()}", integration:"${version()}", type:"json", fields:"acState"],
		body: jsonBody]

	def returnStatus = false
    try{
       httpPost(cmdParams) { resp ->
			if(resp.status == 200) {
                displayDebugLog( "updated ${resp.data}")
				debugEvent("updated ${resp.data}")
                displayTraceLog( "Successful call to Sensibo API.")
				
                //returnStatus = resp.status
                
                displayDebugLog( "Returning True")
				returnStatus = true
            }
           	else { 
            	displayTraceLog( "Failed call to Sensibo API.")
                returnStatus = false
            }
       }
    }
    catch(Exception e)
	{
		displayDebugLog( "Exception Sending Json: " + e)
		debugEvent ("Exception Sending JSON: " + e)
		returnStatus = false
	}
    
	displayDebugLog( "Return Status: ${returnStatus}")
	return returnStatus
}

def pollChildren(PodUid)
{
    displayTraceLog( "pollChildren() called")
    
    def thermostatIdsString = PodUid

	displayTraceLog( "polling children: $thermostatIdsString")
    
	def pollParams = [
    	uri: "${getServerUrl()}",
    	path: "/api/v2/pods/${thermostatIdsString}/measurements",
    	requestContentType: "application/json",
    	query: [apiKey:"${getapikey()}", integration:"${version()}", type:"json", fields:"batteryVoltage,temperature,humidity,time"]]

	debugEvent ("Before HTTPGET to Sensibo.")

	try{
		httpGet(pollParams) { resp ->

			if (resp.data) {
				debugEvent ("Response from Sensibo GET = ${resp.data}")
				debugEvent ("Response Status = ${resp.status}")
			}

			if(resp.status == 200) {
				displayTraceLog( "poll results returned"     )                           

                displayDebugLog( "DEBUG DATA RESULT" + resp.data.result)
                
                if (resp.data.result == null || resp.data.result.empty) 
                {
                	displayDebugLog( "Cannot get measurement from the API, should ask Sensibo Support Team")
                	debugEvent ("Cannot get measurement from the API, should ask Sensibo Support Team",true)
                }
                
                def setTemp = getACState(thermostatIdsString)
                
                def ClimateReact = getClimateReact(thermostatIdsString)
           
                if (setTemp.Error != "Failed") {
                
				 state.sensibo = resp.data.result.inject([:]) { collector, stat ->

					def dni = thermostatIdsString
					
					displayDebugLog( "updating dni $dni")
                    
                    def stemp = stat.temperature ? stat.temperature.toDouble().round(1) : 0
                    def shumidify = stat.humidity ? stat.humidity.toDouble().round() : 0

                    if (setTemp.temperatureUnit == "F") {
                        stemp = cToF(stemp).round(1)
                    }

					def tMode                        
                    if (setTemp.on=="off") {
        				tMode = "off"
        			}
				    else {
        	 			tMode = setTemp.currentmode
                    }

					def battpourcentage = 100
                    def battVoltage = stat.batteryVoltage
                    
					if (battVoltage == null) 
                    {
                    	battVoltage = 3000
                    }                    
                    
                    if (battVoltage < 2850) battpourcentage = 10
                    if (battVoltage > 2850 && battVoltage < 2950) battpourcentage = 50
                    
					def data = [
						temperature: stemp,
						humidity: shumidify,
                        targetTemperature: setTemp.targetTemperature,
                        fanLevel: setTemp.fanLevel,
                        currentmode: setTemp.currentmode,
                        on: setTemp.on,
                        switch : setTemp.on,
                        thermostatMode: tMode,
                        thermostatFanMode: setTemp.fanLevel,
                        coolingSetpoint: setTemp.targetTemperature,
                        heatingSetpoint: setTemp.targetTemperature,
                        thermostatSetpoint: setTemp.targetTemperature,
                        temperatureUnit : setTemp.temperatureUnit,
                        voltage : battVoltage,
                        swing : setTemp.swing,
                        battery : battpourcentage,
                        powerSource : setTemp.powerSource,
                        productModel : setTemp.productModel,
                        firmwareVersion : setTemp.firmwareVersion,
                        Climate : ClimateReact.Climate,
                        Error: setTemp.Error
					]
                    
					debugEvent ("Event Data = ${data}",false)

					collector[dni] = [data:data]
                    
					return collector
				 }				
                }
                
				displayDebugLog( "updated ${state.sensibo[thermostatIdsString].size()} stats: ${state.sensibo[thermostatIdsString]}")
                debugEvent ("updated ${state.sensibo[thermostatIdsString]}",false)
			}
			else
			{
				log.error "polling children & got http status ${resp.status}"		
			}
		}

	}
	catch(Exception e)
	{
		displayDebugLog( "___exception polling children: " + e)
		debugEvent ("${e}")
	}
}

def pollHandler() {

	debugEvent ("in Poll() method.")
	
    // Hit the Sensibo API for update on all the Pod
	
    def PodList = getChildDevices()
    
    displayDebugLog( PodList)
    PodList.each { 
    	displayDebugLog( "polling " + it.deviceNetworkId)
        pollChildren(it.deviceNetworkId) }
	
    state.sensibo.each {stat ->

		def dni = stat.key

		displayDebugLog( ("DNI = ${dni}"))
		debugEvent ("DNI = ${dni}")

		def d = getChildDevice(dni)

		if(d)
		{        
			displayDebugLog( ("Found Child Device."))
			debugEvent ("Found Child Device.")
			debugEvent("Event Data before generate event call = ${stat}")
			displayDebugLog( state.sensibo[dni])
			d.generateEvent(state.sensibo[dni].data)
		}
	}
}

def debugEvent(message, displayEvent = false) {

	def results = [
		name: "appdebug",
		descriptionText: message,
		displayed: displayEvent
	]
	displayDebugLog( "Generating AppDebug Event: ${results}")
	sendEvent (results)

}

def cToF(temp) {
	return (temp * 1.8 + 32).toDouble()
}

def fToC(temp) {
	return ((temp - 32) / 1.8).toDouble()
}
