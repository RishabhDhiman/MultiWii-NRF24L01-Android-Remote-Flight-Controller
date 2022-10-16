/*
NRF24 TRANSMITTER CODE BY ARCHIT BUBBER
Use pin 9,10 for Nrf
This code uses a buzzer for alerting connection lost and sticks values initialization/
Alternatively you can use Serial Monitor for debugging by uncommenting the lines.
Enjoy Flying
*/
#include <SPI.h>
#include <nRF24L01.h>
#include <RF24.h>

const byte slaveAddress[5] = {'R', 'x', 'A', 'A', 'A'};

RF24 radio(9, 10);

struct RF24Data
{
  int throttle;
  int yaw;
  int pitch;
  int roll;
  int AUX1;
  int AUX2;
  //  int switches;
};
int l[6], m[6], h[6]; // 600-1620
RF24Data data;
void resetData()
{
  data.throttle = 0;
  data.yaw = 127;
  data.pitch = 127;
  data.roll = 127;
  data.AUX1 = 0;
  data.AUX2 = 0;
}
void radio_setup()
{
  radio.begin();
  if (radio.isChipConnected())
  {
    radio.setDataRate(RF24_250KBPS);
    radio.setPALevel(RF24_PA_MAX);
    radio.setChannel(69);
    radio.openWritingPipe(slaveAddress);
  }
  else
  {
    while (!radio.isChipConnected())
    {
      Serial.println("I"); // no radio inserted or bad connection
    }
  }
}
int mapJoystickValues(int val, int lower, int middle, int upper, bool reverse)
{
  if (val > middle - 20 && val < middle + 20)
  {
    return 1502;
  }
  else if (val < middle)
    val = map(val, lower, middle, 1004, 1502);
  else
    val = map(val, middle, upper, 1502, 2004);
  return (reverse ? 2002 - val : val);
}

void setup()
{
  Serial.begin(115200);
  Serial.setTimeout(5);
  radio_setup();
  resetData();
}

void loop()
{
  if (radio.isChipConnected())
  {
    send_nrf();
  }
  else
  {
    Serial.println("W");
  }
}
void send_nrf()
{
  while (!Serial.available())
  {
    // wait for user input
  }
  String input = Serial.readString();
  input.trim();
  data.throttle = input.substring(0, 4).toInt();
  data.yaw = input.substring(4, 8).toInt();
  data.pitch = input.substring(8, 12).toInt();
  data.roll = input.substring(12, 16).toInt();
  data.AUX1 = input.substring(16, 20).toInt();
  data.AUX2 = input.substring(20, 24).toInt();
  unsigned long start = micros();
  if (radio.write(&data, sizeof(RF24Data)))
  {
    unsigned long time = ((micros() - start) / 1000);
    Serial.println(time);
  }
  else
  {
    Serial.println("A");
  }
}
