'''
/*
 * Copyright 2010-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
 '''

from AWSIoTPythonSDK.MQTTLib import AWSIoTMQTTClient
import sys
import logging
import time
import getopt
import json
import motor
import pigpio

carCmd = ['Speed','Dir']
speed = 5
direction = 5

# Custom MQTT message callback
def customCallback(client, userdata, message):
   global speed, direction
   print("Received a new message: " , message.payload)
   print("--------------\n\n")
   try:
      cmd_dict = json.loads(message.payload.decode('utf-8'))
#      cmd_gyro = json.loads(message.payload)
      #cmd_dict = json.loads(message.payload)
      #print (cmd_dict)
      cmd = cmd_dict["cmd"]
      val = cmd_dict["val"]
      if cmd == carCmd[0]:
         speed = val
      elif cmd == carCmd[1]:
         direction = val
      elif cmd == "MovePiCam" :

        duty_cycle_UD = pi.get_servo_pulsewidth(servo_pin_UD)
        duty_cycle_RL = pi.get_servo_pulsewidth(servo_pin_RL)
        if (val == "Right"):
                duty_cycle_RL = duty_cycle_RL - 30
        if (val == "Left"):
                duty_cycle_RL = duty_cycle_RL + 30
        if (val == "Up"):
                duty_cycle_UD = duty_cycle_UD + 30
        if (val == "Down"):
                duty_cycle_UD = duty_cycle_UD - 30
        if (len(val.split()) == 2):
 #       if (len(message.payload.split()) == 2):
                print("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$")
                try:
                        angle_x = float(val.split()[0])#Range 0 - 90 deg
                        angle_y = float(val.split()[1])#Range 0 - 90  deg
                        duty_cycle_RL = MIN_DUTY+((1300 * angle_x) / 90)
                        duty_cycle_UD = MIN_DUTY+((1300 * angle_y) / 90)
                except:
                        print("ERROR: angles are not floats!")
                        
        #---STAY IN LIMITS---        
        if (duty_cycle_UD < MIN_DUTY):
                duty_cycle_UD = MIN_DUTY
        if (duty_cycle_UD > MAX_DUTY):
                duty_cycle_UD = MAX_DUTY
        if (duty_cycle_RL < MIN_DUTY_RL):
                duty_cycle_RL = MIN_DUTY_RL
        if (duty_cycle_RL > MAX_DUTY_RL):
                duty_cycle_RL = MAX_DUTY_RL
        #---SET SERVO---
       	print("Tilt axis: [Up/Down] ")
        print(duty_cycle_UD)
        print("Pan axis: [Right/Left]: ")
        print(duty_cycle_RL)
        pi.set_servo_pulsewidth(servo_pin_UD ,duty_cycle_UD)
        pi.set_servo_pulsewidth(servo_pin_RL ,duty_cycle_RL)
        print("cmd is : " , cmd , "------val is : ",val )

#------------------------CALLBACKS DONE----------------------------
   except KeyboardInterrupt:
      speed = 5
      direction = 5
   except ValueError:
      print ("MQTT message is not a valid JSON")
   except KeyError:
      print ("MQTT JSON object does not contain the key power_on")

   motor.move(speed,direction)


# Read in command-line parameters
useWebsocket = False
host = ""
rootCAPath = ""
certificatePath = ""
privateKeyPath = ""
try:
	opts, args = getopt.getopt(sys.argv[1:], "hwe:k:c:r:", ["help", "endpoint=", "key=","cert=","rootCA=", "websocket"])
	if len(opts) == 0:
		raise getopt.GetoptError("No input parameters!")
	for opt, arg in opts:
		if opt in ("-h", "--help"):
			print(helpInfo)
			exit(0)
		if opt in ("-e", "--endpoint"):
			host = arg
		if opt in ("-r", "--rootCA"):
			rootCAPath = arg
		if opt in ("-c", "--cert"):
			certificatePath = arg
		if opt in ("-k", "--key"):
			privateKeyPath = arg
		if opt in ("-w", "--websocket"):
			useWebsocket = True
except getopt.GetoptError:
	print("usageInfo")
	exit(1)

# Missing configuration notification
missingConfiguration = False
if not host:
	print("Missing '-e' or '--endpoint'")
	missingConfiguration = True
if not rootCAPath:
	print("Missing '-r' or '--rootCA'")
	missingConfiguration = True
if not useWebsocket:
	if not certificatePath:
		print("Missing '-c' or '--cert'")
		missingConfiguration = True
	if not privateKeyPath:
		print("Missing '-k' or '--key'")
		missingConfiguration = True
if missingConfiguration:
	exit(2)

# Init AWSIoTMQTTClient
myAWSIoTMQTTClient = None

myAWSIoTMQTTClient = AWSIoTMQTTClient("basicSub")
myAWSIoTMQTTClient.configureEndpoint(host, 8883)
myAWSIoTMQTTClient.configureCredentials(rootCAPath, privateKeyPath, certificatePath)

# AWSIoTMQTTClient connection configuration
myAWSIoTMQTTClient.configureAutoReconnectBackoffTime(1, 32, 20)
myAWSIoTMQTTClient.configureOfflinePublishQueueing(-1)  # Infinite offline Publish queueing
myAWSIoTMQTTClient.configureDrainingFrequency(2)  # Draining: 2 Hz
myAWSIoTMQTTClient.configureConnectDisconnectTimeout(10)  # 10 sec
myAWSIoTMQTTClient.configureMQTTOperationTimeout(5)  # 5 sec

# Connect and subscribe to AWS IoT
myAWSIoTMQTTClient.connect()
myAWSIoTMQTTClient.subscribe("car", 1, customCallback)

loopCount = 0
# Publish to the same topic in a loop

#------------------------SERVO Init----------------------------
pi = pigpio.pi() # Connect to local Pi.

#Constants
MIN_DUTY = 1000
MIN_DUTY_RL = 500
MAX_DUTY = 2500
MAX_DUTY_RL = 2500
CENTRE_RL = (((MAX_DUTY_RL - MIN_DUTY_RL) / 2) + MIN_DUTY_RL)
CENTRE = (((MAX_DUTY - MIN_DUTY) / 2) + MIN_DUTY*1.1)


duty_cycle = CENTRE     # Should be the center for a SG90

# Configure the Pi to use pin names 
servo_pin_UD = 13
servo_pin_RL = 5

# Create PWM channel on the servo pin with a frequency of CENTRE
duty_cycle_UD = CENTRE
duty_cycle_RL = CENTRE_RL
print("init Tilt axis [Up/Down]: ")
print(duty_cycle_UD)
print("init Pan axis [Right/Left]: ")
print(duty_cycle_RL)

pi.set_servo_pulsewidth(servo_pin_UD , duty_cycle_UD)
pi.set_servo_pulsewidth(servo_pin_RL , duty_cycle_RL)

#------------------------SERVO Init DONE----------------------------


while 1:
	time.sleep(3)
exit(0)
