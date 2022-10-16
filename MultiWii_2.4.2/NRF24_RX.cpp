#include "Arduino.h"
#include "config.h"
#include "def.h"
#include "types.h"
#include "MultiWii.h"
#include <RF24.h>
#include "NRF24_RX.h"

#if defined(NRF24_RX)

int16_t nrf24_rcData[RC_CHANS];

// Single radio pipe address for the 2 nodes to communicate.

const byte thisSlaveAddress[5] = {'R', 'x', 'A', 'A', 'A'}; // Remember, SAME AS TRANSMITTER CODE

RF24 radio(9, 10); // CE, CSN

RF24Data MyData;
void radio_setup()
{
  radio.begin();
  radio.setDataRate(RF24_250KBPS);
  radio.setPALevel(RF24_PA_MAX);
  radio.setChannel(69);
  radio.openReadingPipe(1, thisSlaveAddress);
  radio.startListening();
}
void resetRF24Data()
{
  MyData.throttle = 0;
  MyData.yaw = 1500;
  MyData.pitch = 1500;
  MyData.roll = 1500;
  MyData.AUX1 = 0;
  MyData.AUX2 = 0;
}

void NRF24_Init()
{
  resetRF24Data();
  radio_setup();
}

void NRF24_Read_RC()
{
  static unsigned long lastRecvTime = 0;
  unsigned long now = millis();
  while (radio.available())
  {
    radio.read(&MyData, sizeof(RF24Data));
    lastRecvTime = now;
  }
  if (now - lastRecvTime > 1000)
  {
    resetRF24Data();
  }
  nrf24_rcData[THROTTLE] = MyData.throttle;
  nrf24_rcData[ROLL] = MyData.roll;
  nrf24_rcData[PITCH] = MyData.pitch;
  nrf24_rcData[YAW] = MyData.yaw;
  nrf24_rcData[AUX1] = MyData.AUX1;
  nrf24_rcData[AUX2] = MyData.AUX2;
  // If your channels are inverted, reverse the map value. Example. From 1000 to 2000 ---> 2000 to 1000
}

#endif
