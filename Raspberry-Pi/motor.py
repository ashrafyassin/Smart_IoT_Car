#this script handles the motor speed
from gpiozero import Motor

#backward(speed) (o-1)
#forward(speed)
#stop()

in1 = 23
in2 = 24

in3 = 17
in4 = 27

leftMotorSpeed = 0
rightMotorSpeed = 0

leftMotor = Motor(in1, in2)
rightMotor = Motor(in3, in4)

def move(speed, dir):
    modSpeed = speed - 5
    modDir = dir - 5

    leftMotorSpeed = float(abs((modSpeed)))/5
    rightMotorSpeed = float(abs((modSpeed)))/5

    if (modDir > 0):
        leftMotorSpeed = (leftMotorSpeed * (5-modDir))/5
    else :
        rightMotorSpeed = (rightMotorSpeed * dir)/5

    print('left motor speed is : ',leftMotorSpeed, ' ---- right motor speed is : ',rightMotorSpeed)

    if (modSpeed == 0):
        leftMotor.stop()
        rightMotor.stop()

    elif (modSpeed < 0 ):
        leftMotor.forward(leftMotorSpeed)
        rightMotor.forward(rightMotorSpeed)
    else :
        leftMotor.backward(leftMotorSpeed)
        rightMotor.backward(rightMotorSpeed)
